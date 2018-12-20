package com.hwl.im.core.immode;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import com.hwl.im.core.ThreadPoolUtil;
import com.hwl.im.core.imaction.MessageSendExecutor;
import com.hwl.im.core.imom.OnlineManage;
import com.hwl.im.core.imqos.RetryMessageManage;
import com.hwl.im.core.imqos.SentMessageManage;
import com.hwl.im.core.imstore.OfflineMessageManage;
import com.hwl.imcore.improto.ImMessageContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

public class MessageOperate {

    final static Logger log = LogManager.getLogger(MessageOperate.class.getName());
    final static ExecutorService executorService = Executors.newFixedThreadPool(ThreadPoolUtil.ioIntesivePoolSize());

    public static void send(Channel channel, ImMessageContext messageContext, Consumer<Boolean> callback) {
        if (channel == null || messageContext == null) {
            callback.accept(false);
            return;
        }
        ChannelFuture channelFuture = channel.writeAndFlush(messageContext);
        channelFuture.addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (callback != null) {
                    log.debug("Server channel complete operate {}", future.isSuccess());
                    callback.accept(future.isSuccess());
                }
            }
        });
    }

    // public static void send(Channel channel, ImMessageContext messageContext, Function<Boolean, Void> callback) {
    //     if (channel == null || messageContext == null) {
    //         callback.apply(false);
    //         return;
    //     }
    //     ChannelFuture channelFuture = channel.writeAndFlush(messageContext);
    //     channelFuture.addListener(new ChannelFutureListener() {

    //         @Override
    //         public void operationComplete(ChannelFuture future) throws Exception {
    //             if (callback != null)
    //                 callback.apply(future.isSuccess());
    //         }
    //     });
    // }

    public static void clientSend(Channel channel, MessageSendExecutor sendExecutor) {
        if (sendExecutor == null || channel == null)
            return;
        ImMessageContext messageContext = sendExecutor.getMessageContext();
        if (messageContext == null) {
            sendExecutor.sendResultCallback(false);
            return;
        }
        ChannelFuture channelFuture = channel.writeAndFlush(messageContext);
        channelFuture.addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (sendExecutor.isSendFailedAndClose() && !future.isSuccess()) {
                    channel.close();
                }
                sendExecutor.sendResultCallback(future.isSuccess());
            }
        });
    }

//    public static void serverSendAndRetry(Long userid, ImMessageContext messageContext,
//                                          Consumer<Boolean> callback) {
//        Channel toUserChannel = OnlineManage.getInstance().getChannel(userid);
//        if (toUserChannel == null) {
//            // offline
//            OfflineMessageManage.getInstance().addMessage(userid, messageContext);
//            if (callback != null) {
//                callback.accept(false);
//            }
//        } else {
//            // online
//            send(toUserChannel, messageContext, callback);
//        }
//    }
//
//    public static void serverSendAndRetry(Long userid, ImMessageContext messageContext, Runnable succCallback) {
//        Channel toUserChannel = OnlineManage.getInstance().getChannel(userid);
//        if (toUserChannel == null) {
//            // offline
//            OfflineMessageManage.getInstance().addMessage(userid, messageContext);
//        } else {
//            // online
//            send(toUserChannel, messageContext, new Consumer<Boolean>() {
//
//                @Override
//                public void accept(Boolean succ) {
//                    if (succ) {
//                        if (succCallback != null) {
//                            succCallback.run();
//                        }
//                    } else {
//                        // failed
//                        RetryMessageManage.getInstance().addMessage(userid, messageContext, false);
//                    }
//                }
//            });
//        }
//    }

    public static void moveSentMessageIntoOffline(long userid) {
        LinkedList<ImMessageContext> messages = SentMessageManage.getInstance().getMessages(userid);
		synchronized(messages){
			OfflineMessageManage.getInstance().addMessages(userid, messages);
			messages.clear();
		}
    }

    public static void removeSentMessage(long userid, String messageGuid) {
        SentMessageManage.getInstance().removeMessage(userid, messageGuid);
    }

    /**
    *  �ͻ�������Ͽ���������û�в�׽������ʱ��������Ϊ�������ӵ��ͻ���
    *  ����������Ȼ���Գɹ����������ݵ��ͻ��ˣ����ʱ�����͵����ݶ��ᴢ����SentMessageManage�е�Queue��
    *  ���ͻ���û����������ֱ����������⵽����ʱ��Ҫ��SentMessageManage��Queue������ת�Ƶ�Offline���ߴ洢����
    *  ���ͻ����������Ӳ���֤�ɹ��󣬼�⵽SentMessageManage�е�Queue�����ݣ���ʱ��Ҫ��SentMessageManage��Queue������ת�Ƶ�Offline���ߴ洢���У����ҿ���������Ϣ�����߳�
    */
    public static void serverPushOffline(long userid, String messageGuid) {
		removeSentMessage(userid,messageGuid);
		moveSentMessageIntoOffline(userid);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                serverPushOffline(userid, OfflineMessageManage.getInstance().pollMessage(userid));
            }
        });
    }

    public static void serverPushOffline(long userid, ImMessageContext messageContext) {
        if (userid <= 0 || messageContext == null) return;

        Channel toUserChannel = OnlineManage.getInstance().getChannel(userid);
        if (toUserChannel == null) {
            // offline
            log.debug("Server push offline message operate : user is offline , userid({}) message({})",userid, messageContext.toString());
            OfflineMessageManage.getInstance().addMessage(userid, messageContext);
            return;
        }

        if (SentMessageManage.getInstance().isSpilled(userid)) {
            log.debug("Server push offline message operate : sent message queue is spilled , userid({}) message({})",userid, messageContext.toString());
            OfflineMessageManage.getInstance().addMessage(userid, messageContext);
            return;
        }

        // online
        send(toUserChannel, messageContext, new Consumer<Boolean>() {

            @Override
            public void accept(Boolean succ) {
                if (succ) {
                    SentMessageManage.getInstance().addMessage(userid, messageContext);
                    serverPushOffline(userid, OfflineMessageManage.getInstance().pollMessage(userid));
                } else {
                    // failed
                    log.debug("Server push offline message failed : userid({}) message({})",userid, messageContext.toString());
                    OfflineMessageManage.getInstance().addMessage(userid, messageContext);
                }
            }
        });
    }

    public static void serverPushOnline(Long userid, ImMessageContext messageContext, Consumer<Boolean> callback) {
        if (userid <= 0 || messageContext == null) return;

        Channel toUserChannel = OnlineManage.getInstance().getChannel(userid);
        if (toUserChannel == null || SentMessageManage.getInstance().isSpilled(userid)) {
            // add offline message
            OfflineMessageManage.getInstance().addMessage(userid, messageContext);
            return;
        }

        send(toUserChannel, messageContext, new Consumer<Boolean>() {
            @Override
            public void accept(Boolean succ) {
                if (callback != null) {
                    callback.accept(succ);
                }
                if (succ) {
                    SentMessageManage.getInstance().addMessage(userid, messageContext);
                }else{
                    // failed
                    OfflineMessageManage.getInstance().addMessage(userid, messageContext);
                }
            }
        });
    }

//    public static void serverPush(Long userid) {
//        Channel toUserChannel = OnlineManage.getInstance().getChannel(userid);
//        if (toUserChannel == null)
//            return;
//
//        executorService.execute(new Runnable() {
//
//            @Override
//            public void run() {
//                log.debug("Server push offline message run , current thread {}", Thread.currentThread().getName());
//                serverPushOfflineMessage(userid, toUserChannel);
//            }
//        });
//    }

//    private static void serverPushOfflineMessage(Long userid, Channel channel) {
//
//        ImMessageContext messageContext = OfflineMessageManage.getInstance().pollMessage(userid);
//        if (messageContext == null)
//            return;
//
//        send(channel, messageContext, new Consumer<Boolean>() {
//
//            @Override
//            public void accept(Boolean succ) {
//                if (succ) {
//                    log.debug("Sever push offline message success : {}", messageContext.toString());
//                } else {
//                    log.debug("Sever push offline message failed : {}", messageContext.toString());
//                    // failed
//                    RetryMessageManage.getInstance().addMessage(userid, messageContext, true);
//                }
//                serverPushOfflineMessage(userid, channel);
//            }
//        });
//    }

}