package com.tuankiet.myapplication.model;

import java.io.Serializable;

public class Post implements Serializable {
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_PENDING = "PENDING_QUEUE";

    private long id;
    private String topic;
    private String tone;
    private String language = "Tiếng Việt";
    private String contentFacebook;
    private String contentThreads;
    private String contentLinkedIn;
    private String status;
    private boolean targetFacebook;
    private boolean targetThreads;
    private boolean targetLinkedIn;
    private long createdAt;
    private String imageUrl;
    private String imagePrompt;
    private int reachCount;
    private int likeCount;
    private int commentCount;
    private int shareCount;
    private boolean isPublishedFacebook;
    private boolean isPublishedThreads;
    private boolean isPublishedLinkedIn;

    public Post() {
        this.status = STATUS_DRAFT;
        this.createdAt = System.currentTimeMillis();
    }

    public Post(long id, String topic, String tone, String contentFacebook, String contentLinkedIn,
                String status, boolean targetFacebook, boolean targetLinkedIn, long createdAt,
                String imageUrl, int reachCount, int likeCount, int commentCount, int shareCount) {
        this.id = id;
        this.topic = topic;
        this.tone = tone;
        this.contentFacebook = contentFacebook;
        this.contentLinkedIn = contentLinkedIn;
        this.status = status;
        this.targetFacebook = targetFacebook;
        this.targetLinkedIn = targetLinkedIn;
        this.createdAt = createdAt;
        this.imageUrl = imageUrl;
        this.reachCount = reachCount;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.shareCount = shareCount;
        this.isPublishedFacebook = targetFacebook && STATUS_PUBLISHED.equals(status);
        this.isPublishedLinkedIn = targetLinkedIn && STATUS_PUBLISHED.equals(status);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getTone() {
        return tone;
    }

    public void setTone(String tone) {
        this.tone = tone;
    }

    public String getLanguage() {
        return language != null ? language : "Tiếng Việt";
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getContentFacebook() {
        return contentFacebook != null ? contentFacebook : "";
    }

    public void setContentFacebook(String contentFacebook) {
        this.contentFacebook = contentFacebook;
    }

    public String getContentThreads() {
        return contentThreads != null ? contentThreads : "";
    }

    public void setContentThreads(String contentThreads) {
        this.contentThreads = contentThreads;
    }

    public String getContentLinkedIn() {
        return contentLinkedIn != null ? contentLinkedIn : "";
    }

    public void setContentLinkedIn(String contentLinkedIn) {
        this.contentLinkedIn = contentLinkedIn;
    }

    public String getStatus() {
        return status != null ? status : STATUS_DRAFT;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isTargetFacebook() {
        return targetFacebook;
    }

    public void setTargetFacebook(boolean targetFacebook) {
        this.targetFacebook = targetFacebook;
    }

    public boolean isTargetThreads() {
        return targetThreads;
    }

    public void setTargetThreads(boolean targetThreads) {
        this.targetThreads = targetThreads;
    }

    public boolean isTargetLinkedIn() {
        return targetLinkedIn;
    }

    public void setTargetLinkedIn(boolean targetLinkedIn) {
        this.targetLinkedIn = targetLinkedIn;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImagePrompt() {
        return imagePrompt;
    }

    public void setImagePrompt(String imagePrompt) {
        this.imagePrompt = imagePrompt;
    }

    public int getReachCount() {
        return reachCount;
    }

    public void setReachCount(int reachCount) {
        this.reachCount = reachCount;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public int getShareCount() {
        return shareCount;
    }

    public void setShareCount(int shareCount) {
        this.shareCount = shareCount;
    }

    public boolean isPublishedFacebook() {
        return isPublishedFacebook;
    }

    public void setPublishedFacebook(boolean publishedFacebook) {
        isPublishedFacebook = publishedFacebook;
    }

    public boolean isPublishedThreads() {
        return isPublishedThreads;
    }

    public void setPublishedThreads(boolean publishedThreads) {
        isPublishedThreads = publishedThreads;
    }

    public boolean isPublishedLinkedIn() {
        return isPublishedLinkedIn;
    }

    public void setPublishedLinkedIn(boolean publishedLinkedIn) {
        isPublishedLinkedIn = publishedLinkedIn;
    }
}

