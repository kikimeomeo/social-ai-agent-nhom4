package com.example.app.model;

public class Comment {

    public enum SocialPlatform {
        ALL,
        FACEBOOK,
        LINKEDIN,
        THREADS
    }

    private String id;
    private String authorName;
    private String authorAvatarUrl;
    private String content;
    private SocialPlatform platform;
    private boolean isReplied;
    private String timestamp;

    public Comment(String id, String authorName, String authorAvatarUrl, String content, SocialPlatform platform, boolean isReplied, String timestamp) {
        this.id = id;
        this.authorName = authorName;
        this.authorAvatarUrl = authorAvatarUrl;
        this.content = content;
        this.platform = platform;
        this.isReplied = isReplied;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getAuthorAvatarUrl() {
        return authorAvatarUrl;
    }

    public String getContent() {
        return content;
    }

    public SocialPlatform getPlatform() {
        return platform;
    }

    public boolean isReplied() {
        return isReplied;
    }

    public String getTimestamp() {
        return timestamp;
    }
}