package com.tuankiet.myapplication.model;

import java.io.Serializable;

public class CommentItem implements Serializable {
    public static final String CATEGORY_PRAISE = "PRAISE";
    public static final String CATEGORY_QUESTION = "QUESTION";
    public static final String CATEGORY_NEGATIVE = "NEGATIVE";
    public static final String CATEGORY_SPAM = "SPAM";

    private long id;
    private SocialPlatform platform;
    private String postTitle;
    private String authorName;
    private String authorInitial;
    private String content;
    private String timeAgo;
    private String category;
    private boolean isAnswered;
    private String replyText;
    private String aiSuggestedReply;
    private boolean isNegativeFlagged;
    private String riskWarning;

    public CommentItem() {
    }

    public CommentItem(long id, SocialPlatform platform, String postTitle, String authorName,
                       String authorInitial, String content, String timeAgo, String category,
                       boolean isAnswered, String replyText, String aiSuggestedReply,
                       boolean isNegativeFlagged, String riskWarning) {
        this.id = id;
        this.platform = platform;
        this.postTitle = postTitle;
        this.authorName = authorName;
        this.authorInitial = authorInitial;
        this.content = content;
        this.timeAgo = timeAgo;
        this.category = category;
        this.isAnswered = isAnswered;
        this.replyText = replyText;
        this.aiSuggestedReply = aiSuggestedReply;
        this.isNegativeFlagged = isNegativeFlagged;
        this.riskWarning = riskWarning;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public SocialPlatform getPlatform() {
        return platform;
    }

    public void setPlatform(SocialPlatform platform) {
        this.platform = platform;
    }

    public String getPostTitle() {
        return postTitle;
    }

    public void setPostTitle(String postTitle) {
        this.postTitle = postTitle;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorInitial() {
        return authorInitial;
    }

    public void setAuthorInitial(String authorInitial) {
        this.authorInitial = authorInitial;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTimeAgo() {
        return timeAgo;
    }

    public void setTimeAgo(String timeAgo) {
        this.timeAgo = timeAgo;
    }

    public String getCategory() {
        return category != null ? category : CATEGORY_PRAISE;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isAnswered() {
        return isAnswered;
    }

    public void setAnswered(boolean answered) {
        isAnswered = answered;
    }

    public String getReplyText() {
        return replyText;
    }

    public void setReplyText(String replyText) {
        this.replyText = replyText;
    }

    public String getAiSuggestedReply() {
        return aiSuggestedReply;
    }

    public void setAiSuggestedReply(String aiSuggestedReply) {
        this.aiSuggestedReply = aiSuggestedReply;
    }

    public boolean isNegativeFlagged() {
        return isNegativeFlagged;
    }

    public void setNegativeFlagged(boolean negativeFlagged) {
        isNegativeFlagged = negativeFlagged;
    }

    public String getRiskWarning() {
        return riskWarning;
    }

    public void setRiskWarning(String riskWarning) {
        this.riskWarning = riskWarning;
    }
}

