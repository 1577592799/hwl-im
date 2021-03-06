package com.hwl.im.server.action;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.hwl.im.server.extra.IOnlineSessionStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

public class OnlineChannelManager {

    public final static String USER_SESSION_IDENTITY = "user-session-identity";
    public final static AttributeKey<String> USER_SESSION_IDENTITY_ATTR = AttributeKey.valueOf(USER_SESSION_IDENTITY);

    private final static ConcurrentHashMap<String, Channel> onlineChannels = new ConcurrentHashMap<String, Channel>();
    private static OnlineChannelManager instance = new OnlineChannelManager();
    private static Logger log = LogManager.getLogger(OnlineChannelManager.class.getName());

    private boolean isDebug = true;
    private IOnlineSessionStorage sessionManager = null;

    private OnlineChannelManager() {
    }

    public static OnlineChannelManager getInstance() {
        if (instance == null)
            instance = new OnlineChannelManager();

        return instance;
    }

    public static void setSessionStorage(IOnlineSessionStorage sessionStorage) {
        instance.sessionManager = sessionStorage;
        if (sessionStorage == null) {
            throw new NullPointerException("The param of sessionStorage is empty.");
        }
    }

    public void printLog() {
        if (!isDebug)
            return;

        log.debug("Online users : {}", onlineChannels.size());

        if (onlineChannels.size() <= 0)
            return;

        for (String key : onlineChannels.keySet())
            log.debug(" sessionid : {} ,address : {}", key, onlineChannels.get(key).remoteAddress());
    }

    public Channel getChannel(String sessionid) {
        if (sessionid == null || sessionid.isEmpty())
            return null;
        return onlineChannels.get(sessionid);
    }

    public void addChannel(Channel channel) {
        String key = channel.attr(USER_SESSION_IDENTITY_ATTR).get();

        onlineChannels.put(key, channel);

        printLog();
    }

    public void removeChannel(Channel channel) {
        String key = channel.attr(USER_SESSION_IDENTITY_ATTR).get();
        if (key != null) {
            sessionManager.removeSession(key);
            onlineChannels.remove(key);
        }

        printLog();
    }

    public String getSession(long userid) {
        if (userid <= 0)
            return null;
        return sessionManager.getSession(userid);
    }

    public long getUserId(Channel channel) {
        String key = channel.attr(USER_SESSION_IDENTITY_ATTR).get();
        return sessionManager.getUserId(key);
    }

    public boolean isOnline(long userid) {
        if (userid <= 0)
            return false;

        return sessionManager.getSession(userid) != null;
    }

    public boolean isOnline(String sessionid) {
        if (sessionid == null || sessionid.isEmpty())
            return false;
        return onlineChannels.containsKey(sessionid);
    }

    public boolean isOnline(Channel channel) {
        long userId = getUserId(channel);
        if (userId <= 0) return false;
        Channel uc = getChannel(userId);
        return uc.isActive();
    }

    public Channel getChannel(long userid) {
        if (userid <= 0)
            return null;

        String sessid = sessionManager.getSession(userid);
        if (sessid == null || sessid.isEmpty()) {
            return null;
        }

        return onlineChannels.get(sessid);
    }

    public void setChannelSessionid(long userid, String sessionid, Channel channel, Consumer<Boolean> operateCallback) {
        if (userid <= 0 || sessionid == null || sessionid.isEmpty() || channel == null) {
            log.error("setChannelSessionid : userid = {} , sessionid = {} , channel = {}", userid, sessionid,
                    channel.remoteAddress());
            return;
        }

        channel.attr(USER_SESSION_IDENTITY_ATTR).set(sessionid);
        onlineChannels.put(sessionid, channel);
        sessionManager.setSession(userid, sessionid, operateCallback);
    }
}