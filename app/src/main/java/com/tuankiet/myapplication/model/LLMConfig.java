package com.tuankiet.myapplication.model;

import java.io.Serializable;

public class LLMConfig implements Serializable {
    public static final String PROVIDER_DEMO = "DEMO";
    public static final String PROVIDER_GEMINI = "Google Gemini";
    public static final String PROVIDER_OPENAI = "OpenAI (GPT-4o)";
    public static final String PROVIDER_OPENROUTER = "OpenRouter";

    private String provider;
    private String apiKey;
    private boolean isDemoMode;
    private int totalTokensUsed;
    private double totalCostUsd;
    private String postGenerationModel;
    private String commentClassificationModel;
    private String imagePromptModel;

    public LLMConfig() {
        this.provider = PROVIDER_GEMINI;
        this.apiKey = "";
        this.isDemoMode = true; // Default to demo mode for instantaneous test
        this.totalTokensUsed = 12450;
        this.totalCostUsd = 0.048;
        this.postGenerationModel = "gemini-3.1-flash-lite";
        this.commentClassificationModel = "gemini-2.5-flash-lite";
        this.imagePromptModel = "gemini-2.0-flash";
    }

    public LLMConfig(String provider, String apiKey, boolean isDemoMode, int totalTokensUsed, double totalCostUsd) {
        this(provider, apiKey, isDemoMode, totalTokensUsed, totalCostUsd, "gemini-3.1-flash-lite", "gemini-2.5-flash-lite", "gemini-2.0-flash");
    }

    public LLMConfig(String provider, String apiKey, boolean isDemoMode, int totalTokensUsed, double totalCostUsd,
                     String postGenerationModel, String commentClassificationModel, String imagePromptModel) {
        this.provider = provider;
        this.apiKey = apiKey;
        this.isDemoMode = isDemoMode;
        this.totalTokensUsed = totalTokensUsed;
        this.totalCostUsd = totalCostUsd;
        this.postGenerationModel = postGenerationModel;
        this.commentClassificationModel = commentClassificationModel;
        this.imagePromptModel = imagePromptModel;
    }

    public String getProvider() {
        return provider != null ? provider : PROVIDER_GEMINI;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getApiKey() {
        return apiKey != null ? apiKey : "";
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean isDemoMode() {
        return isDemoMode;
    }

    public void setDemoMode(boolean demoMode) {
        isDemoMode = demoMode;
    }

    public int getTotalTokensUsed() {
        return totalTokensUsed;
    }

    public void setTotalTokensUsed(int totalTokensUsed) {
        this.totalTokensUsed = totalTokensUsed;
    }

    public double getTotalCostUsd() {
        return totalCostUsd;
    }

    public void setTotalCostUsd(double totalCostUsd) {
        this.totalCostUsd = totalCostUsd;
    }

    public void addTokens(int tokens, double cost) {
        this.totalTokensUsed += tokens;
        this.totalCostUsd += cost;
    }

    public String getPostGenerationModel() {
        return postGenerationModel != null ? postGenerationModel : "gemini-3.1-flash-lite";
    }

    public void setPostGenerationModel(String postGenerationModel) {
        this.postGenerationModel = postGenerationModel;
    }

    public String getCommentClassificationModel() {
        return commentClassificationModel != null ? commentClassificationModel : "gemini-2.5-flash-lite";
    }

    public void setCommentClassificationModel(String commentClassificationModel) {
        this.commentClassificationModel = commentClassificationModel;
    }

    public String getImagePromptModel() {
        return imagePromptModel != null ? imagePromptModel : "gemini-2.0-flash";
    }

    public void setImagePromptModel(String imagePromptModel) {
        this.imagePromptModel = imagePromptModel;
    }
}

