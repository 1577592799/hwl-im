package com.hwl.im.server.core;

import com.hwl.im.server.action.OnlineChannelManager;
import com.hwl.im.server.action.ServerMessageOperator;
import com.hwl.im.server.extra.IRequestValidator;
import com.hwl.imcore.improto.ImMessageContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class ServerMessageChannelHandler extends SimpleChannelInboundHandler<ImMessageContext> {

    static Logger log = LogManager.getLogger(ServerMessageChannelHandler.class.getName());

    private IRequestValidator requestValidator;

    public ServerMessageChannelHandler(IRequestValidator requestValidator) {
        this.requestValidator = requestValidator;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.ALL_IDLE) {
                log.debug("Server : client {} channel is disconnected." , ctx.channel().remoteAddress());
                ctx.channel().close();
            }
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ImMessageContext msg) throws Exception {
        log.debug("Server channel read : {}", msg.toString());

        ServerMessageReceiveExecutor receiveExecutor = ServerMessageExecuteFactory.create(msg.getType(), msg.getRequest());
        receiveExecutor.setRequestValidator(requestValidator);
        ImMessageContext response = receiveExecutor.execute(msg.getType(), msg.getRequest().getRequestHead(), ctx.channel());
        if (response != null) ctx.writeAndFlush(response);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("Server channel active: remote client {} connect", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("Server channel inactive: remote client {} disconnect ", ctx.channel().remoteAddress());
        ServerMessageOperator.getInstance().moveSentMessageIntoOffline(ctx.channel());
        OnlineChannelManager.getInstance().removeChannel(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
