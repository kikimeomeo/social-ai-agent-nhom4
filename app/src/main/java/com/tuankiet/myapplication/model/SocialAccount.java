package com.tuankiet.myapplication.model;

import java.io.Serializable;

public class SocialAccount implements Serializable {
    private String id;
    private SocialPlatform platform;
    private String accountName;
    private String handle;
    private boolean isConnected;
    private int followerCount;
    private String avatarInitial;
    private String statusMessage;

    public SocialAccount() {
    }

    public SocialAccount(String id, SocialPlatform platform, String accountName, String handle, boolean isConnected, int followerCount, String avatarInitial, String statusMessage) {
        this.id = id;
        this.platform = platform;
        this.accountName = accountName;
        this.handle = handle;
        this.isConnected = isConnected;
        this.followerCount = followerCount;
        this.avatarInitial = avatarInitial;
        this.statusMessage = statusMessage;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SocialPlatform getPlatform() {
        return platform;
    }

    public void setPlatform(SocialPlatform platform) {
        this.platform = platform;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public int getFollowerCount() {
        return followerCount;
    }

    public void setFollowerCount(int followerCount) {
        this.followerCount = followerCount;
    }

    public String getAvatarInitial() {
        return avatarInitial;
    }

    public void setAvatarInitial(String avatarInitial) {
        this.avatarInitial = avatarInitial;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}

