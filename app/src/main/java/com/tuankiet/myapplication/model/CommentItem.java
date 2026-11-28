package com.tuankiet.myapplication.model;

import java.io.Serializable;
import java.util.Locale;

public class CommentItem implements Serializable {
    public static final String CATEGORY_QUESTION = "QUESTION";
    public static final String CATEGORY_PRAISE = "PRAISE";
    public static final String CATEGORY_SUGGESTION = "SUGGESTION";
    public static final String CATEGORY_NEGATIVE = "NEGATIVE";
    public static final String CATEGORY_SPAM = "SPAM";

    private long id;
    private SocialPlatform platform;
    private String postTitle;
    private String postContent;
    private String authorName;
    private String authorInitial;
    private String content;
    private String timeAgo;
    private String externalCommentId;
    private String category;
    private boolean isAnswered;
    private String replyText;
    private String aiSuggestedReply;
    private boolean isNegativeFlagged;
    private String riskWarning;
    private boolean isFaq;
    private boolean hasPersonalInfo;
    private double aiConfidence = 1.0;
    private boolean autoReplied;
    private String autoReplyBlockedReason;

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
        this.aiConfidence = 0.90;
    }

    public static String getCategoryDisplayName(String category) {
        if (category == null) return "Câu hỏi";
        if (CATEGORY_PRAISE.equalsIgnoreCase(category)) return "Khen ngợi";
        if (CATEGORY_QUESTION.equalsIgnoreCase(category)) return "Câu hỏi";
        if (CATEGORY_SUGGESTION.equalsIgnoreCase(category)) return "Góp ý";
        if (CATEGORY_NEGATIVE.equalsIgnoreCase(category)) return "Tiêu cực";
        if (CATEGORY_SPAM.equalsIgnoreCase(category)) return "Spam";
        return "Câu hỏi";
    }

    public static String normalizeCategory(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals(CATEGORY_QUESTION) || normalized.contains("QUESTION")
                || normalized.contains("CÂU HỎI") || normalized.contains("CAU HOI")
                || normalized.contains("FAQ")) {
            return CATEGORY_QUESTION;
        }
        if (normalized.equals(CATEGORY_PRAISE) || normalized.contains("PRAISE")
                || normalized.contains("KHEN") || normalized.contains("POSITIVE")
                || normalized.contains("COMPLIMENT")) {
            return CATEGORY_PRAISE;
        }
        if (normalized.equals(CATEGORY_SUGGESTION) || normalized.contains("SUGGEST")
                || normalized.contains("FEEDBACK") || normalized.contains("GÓP Ý")
                || normalized.contains("GOP Y") || normalized.contains("ĐỀ XUẤT")) {
            return CATEGORY_SUGGESTION;
        }
        if (normalized.equals(CATEGORY_NEGATIVE) || normalized.contains("NEGATIVE")
                || normalized.contains("TIÊU CỰC") || normalized.contains("TIEU CUC")
                || normalized.contains("COMPLAINT") || normalized.contains("TOXIC")) {
            return CATEGORY_NEGATIVE;
        }
        if (normalized.equals(CATEGORY_SPAM) || normalized.contains("SPAM")
                || normalized.contains("SCAM") || normalized.contains("ADVERT")) {
            return CATEGORY_SPAM;
        }
        return null;
    }

    public String getEffectiveExternalCommentId() {
        if (externalCommentId != null && !externalCommentId.trim().isEmpty()) {
            return externalCommentId.trim();
        }
        if (timeAgo != null && timeAgo.contains("ID:")) {
            String extracted = timeAgo.substring(timeAgo.indexOf("ID:") + 3).trim();
            if (!extracted.isEmpty()) {
                return extracted;
            }
        }
        return null;
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

    public String getPostContent() {
        return postContent;
    }

    public void setPostContent(String postContent) {
        this.postContent = postContent;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorInitial() {
        if (authorInitial != null && !authorInitial.trim().isEmpty()) {
            return authorInitial;
        }
        if (authorName != null && !authorName.trim().isEmpty()) {
            String clean = authorName.trim().replace("@", "");
            if (!clean.isEmpty()) {
                return clean.substring(0, 1).toUpperCase(Locale.ROOT);
            }
        }
        return "?";
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

    public String getExternalCommentId() {
        return externalCommentId;
    }

    public void setExternalCommentId(String externalCommentId) {
        this.externalCommentId = externalCommentId;
    }

    public String getCategory() {
        return category != null ? category : CATEGORY_QUESTION;
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

    public boolean isFaq() {
        return isFaq;
    }

    public void setFaq(boolean faq) {
        isFaq = faq;
    }

    public boolean hasPersonalInfo() {
        return hasPersonalInfo;
    }

    public void setHasPersonalInfo(boolean hasPersonalInfo) {
        this.hasPersonalInfo = hasPersonalInfo;
    }

    public double getAiConfidence() {
        return aiConfidence;
    }

    public void setAiConfidence(double aiConfidence) {
        this.aiConfidence = Math.max(0.0, Math.min(1.0, aiConfidence));
    }

    public boolean isAutoReplied() {
        return autoReplied;
    }

    public void setAutoReplied(boolean autoReplied) {
        this.autoReplied = autoReplied;
    }

    public String getAutoReplyBlockedReason() {
        return autoReplyBlockedReason;
    }

    public void setAutoReplyBlockedReason(String autoReplyBlockedReason) {
        this.autoReplyBlockedReason = autoReplyBlockedReason;
    }
}
