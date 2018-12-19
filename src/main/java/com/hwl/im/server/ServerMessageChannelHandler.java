package com.hwl.im.server;

import java.util.function.Consumer;

import com.hwl.im.core.imaction.MessageReceiveExecutor;
import com.hwl.im.core.immode.MessageOperate;
import com.hwl.im.core.imom.OnlineManage;
import com.hwl.imcore.improto.ImMessageContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class ServerMessageChannelHandler extends SimpleChannelInboundHandler<ImMessageContext> {
    static boolean isDebug = false;
    static Logger log = LogManager.getLogger(ServerMessageChannelHandler.class.getName());

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.ALL_IDLE) {
                ctx.channel().close();
            }
            log.debug("Server : client will disconnect , " + ctx.channel().remoteAddress());
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ImMessageContext msg) throws Exception {
        log.debug("Server channel read : {}", msg.toString());
        
        MessageReceiveExecutor receiveExecutor = MessageExecuteFactory.create(msg);
        if (receiveExecutor == null)
            return;

        receiveExecutor.setChannel(ctx.channel());
        ImMessageContext response = receiveExecutor.execute();

        MessageOperate.send(ctx.channel(), response, new Consumer<Boolean>() {
            @Override
            public void accept(Boolean succ) {
                log.debug("Server response {} :{}", succ, response != null ? response.toString() : "");
            }
        });
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("Server channel active: remote client {} connect", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("Server channel inactive: remote client {} disconnect ", ctx.channel().remoteAddress());
		MessageOperate.moveSentMessageIntoOffline(OnlineManage.getInstance().getUserId(ctx.channel()));
        OnlineManage.getInstance().removeChannel(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
