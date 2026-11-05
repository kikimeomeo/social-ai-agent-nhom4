package com.tuankiet.myapplication.model;

import java.io.Serializable;

public class AgentLog implements Serializable {
    private long id;
    private long timestamp;
    private String stepName;
    private String agentRole;
    private String promptUsed;
    private String outputSnippet;
    private int tokensUsed;
    private double costEstimate;
    private long durationMs;

    public AgentLog() {
    }

    public AgentLog(long id, long timestamp, String stepName, String agentRole, String promptUsed,
                    String outputSnippet, int tokensUsed, double costEstimate, long durationMs) {
        this.id = id;
        this.timestamp = timestamp;
        this.stepName = stepName;
        this.agentRole = agentRole;
        this.promptUsed = promptUsed;
        this.outputSnippet = outputSnippet;
        this.tokensUsed = tokensUsed;
        this.costEstimate = costEstimate;
        this.durationMs = durationMs;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getAgentRole() {
        return agentRole;
    }

    public void setAgentRole(String agentRole) {
        this.agentRole = agentRole;
    }

    public String getPromptUsed() {
        return promptUsed;
    }

    public void setPromptUsed(String promptUsed) {
        this.promptUsed = promptUsed;
    }

    public String getOutputSnippet() {
        return outputSnippet;
    }

    public void setOutputSnippet(String outputSnippet) {
        this.outputSnippet = outputSnippet;
    }

    public int getTokensUsed() {
        return tokensUsed;
    }

    public void setTokensUsed(int tokensUsed) {
        this.tokensUsed = tokensUsed;
    }

    public double getCostEstimate() {
        return costEstimate;
    }

    public void setCostEstimate(double costEstimate) {
        this.costEstimate = costEstimate;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
}

