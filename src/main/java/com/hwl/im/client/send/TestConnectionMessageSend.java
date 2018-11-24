package com.hwl.im.client.send;

import com.hwl.im.core.imaction.AbstractMessageSendExecutor;
import com.hwl.imcore.improto.ImMessageRequest;
import com.hwl.imcore.improto.ImMessageType;
import com.hwl.imcore.improto.ImTestConnectionMessageRequest;

import java.util.function.Consumer;

public class TestConnectionMessageSend extends AbstractMessageSendExecutor {
    Consumer<Boolean> sendCallback;
    long userId;

    public TestConnectionMessageSend(long userId, Consumer<Boolean> sendCallback) {
        this.sendCallback = sendCallback;
        this.userId = userId;
    }

    @Override
    public Consumer<Boolean> sendStatusCallback() {
        return sendCallback;
    }

    @Override
    public void setRequestBody(ImMessageRequest.Builder request) {
        ImTestConnectionMessageRequest testConnectionMessageRequest = ImTestConnectionMessageRequest.newBuilder().setFromUserId(userId)
                .build();
        request.setTestConnectionMessageRequest(testConnectionMessageRequest);
    }

    @Override
    public ImMessageType getMessageType() {
        return ImMessageType.TestConnection;
    }
}