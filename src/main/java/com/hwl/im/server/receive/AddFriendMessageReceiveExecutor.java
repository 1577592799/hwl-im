package com.hwl.im.server.receive;

import java.security.InvalidParameterException;

import com.hwl.im.core.imaction.AbstractMessageReceivExecutor;
import com.hwl.im.core.immode.MessageOperate;
import com.hwl.im.improto.ImAddFriendMessageRequest;
import com.hwl.im.improto.ImAddFriendMessageResponse;
import com.hwl.im.improto.ImMessageContext;
import com.hwl.im.improto.ImMessageResponse;
import com.hwl.im.improto.ImMessageType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AddFriendMessageReceiveExecutor extends AbstractMessageReceivExecutor<ImAddFriendMessageRequest> {
    
    static Logger log = LogManager.getLogger(AddFriendMessageReceiveExecutor.class.getName());

    public AddFriendMessageReceiveExecutor(ImAddFriendMessageRequest imAddFriendMessageRequest) {
        super(imAddFriendMessageRequest);
    }

    @Override
    protected void checkRequestParams() {
        super.checkRequestParams();
        if (request.getAddFriendMessageContent() == null)
            throw new NullPointerException("AddFriendMessageContent");

        if (request.getAddFriendMessageContent().getFromUserId() <= 0) {
            throw new InvalidParameterException("fromUserId");
        }
        if (request.getAddFriendMessageContent().getToUserId() <= 0) {
            throw new InvalidParameterException("toUserId");
        }
    }

    @Override
    public ImMessageType getMessageType() {
        return ImMessageType.AddFriend;
    }

    @Override
    public void executeCore(ImMessageResponse.Builder response) {
        response.setAddFriendMessageResponse(
                ImAddFriendMessageResponse.newBuilder().setAddFriendMessageContent(request.getAddFriendMessageContent())
                        .setBuildTime(System.currentTimeMillis()).build());
        ImMessageContext messageContext = super.getMessageContext(response);

        Long userid = request.getAddFriendMessageContent().getToUserId();
        MessageOperate.serverSendAndRetry(userid, messageContext, (succ) -> {
            if (succ) {
                log.debug("Server push add friend message success : {}", messageContext.toString());
            } else {
                log.error("Server push add friend message failed : {}", messageContext.toString());
            }
        });
    }
}