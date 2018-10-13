package com.hwl.im.client.send;

import com.hwl.im.core.imaction.AbstractMessageSendExecutor;
import com.hwl.im.improto.ImAddFriendMessageContent;
import com.hwl.im.improto.ImAddFriendMessageRequest;
import com.hwl.im.improto.ImMessageRequest;
import com.hwl.im.improto.ImMessageType;

import java.util.function.Consumer;

public class AddFriendMessageSend extends AbstractMessageSendExecutor {

    ImAddFriendMessageContent messageContent;
    Consumer<Boolean> sendCallback;

    public AddFriendMessageSend(Long fromUserId, String fromUserName, String fromUserImage, Long toUserId, String content) {
        messageContent = ImAddFriendMessageContent.newBuilder()
                .setFromUserId(fromUserId)
                .setFromUserName(fromUserName)
                .setFromUserHeadImage(fromUserImage)
                .setToUserId(toUserId)
                .setContent(content)
                .build();
//        this.sendCallback = sendCallback;
    }

    @Override
    public Consumer<Boolean> sendStatusCallback() {
        return sendCallback;
    }

    @Override
    public void setRequestBody(ImMessageRequest.Builder request) {
        request.setAddFriendMessageRequest(
                ImAddFriendMessageRequest.newBuilder().setAddFriendMessageContent(messageContent)
                        .build());
    }

    @Override
    public ImMessageType getMessageType() {
        return ImMessageType.AddFriend;
    }
}