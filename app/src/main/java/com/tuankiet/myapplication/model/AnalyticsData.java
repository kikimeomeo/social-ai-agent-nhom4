package com.tuankiet.myapplication.model;

import java.io.Serializable;
import java.util.List;

public class AnalyticsData implements Serializable {
    public static class DayMetric implements Serializable {
        private String dayLabel;
        private int facebookEngagement;
        private int threadsEngagement;

        public DayMetric(String dayLabel, int facebookEngagement, int threadsEngagement) {
            this.dayLabel = dayLabel;
            this.facebookEngagement = facebookEngagement;
            this.threadsEngagement = threadsEngagement;
        }

        public String getDayLabel() {
            return dayLabel;
        }

        public int getFacebookEngagement() {
            return facebookEngagement;
        }

        public int getThreadsEngagement() {
            return threadsEngagement;
        }

        public int getTotal() {
            return facebookEngagement + threadsEngagement;
        }
    }

    private int totalPostsCreated;
    private int weeklyEngagement;
    private int pendingComments;
    private int facebookReach;
    private int facebookLikes;
    private int facebookComments;
    private double facebookEngagementRate;

    private int threadsReach;
    private int threadsLikes;
    private int threadsComments;
    private double threadsEngagementRate;

    private String weeklyAiSummary;
    private String topPerformingPost;
    private String nextWeekAdvice;

    private List<DayMetric> weeklyTimeline;

    public AnalyticsData() {
    }

    public int getTotalPostsCreated() {
        return totalPostsCreated;
    }

    public void setTotalPostsCreated(int totalPostsCreated) {
        this.totalPostsCreated = totalPostsCreated;
    }

    public int getWeeklyEngagement() {
        return weeklyEngagement;
    }

    public void setWeeklyEngagement(int weeklyEngagement) {
        this.weeklyEngagement = weeklyEngagement;
    }

    public int getPendingComments() {
        return pendingComments;
    }

    public void setPendingComments(int pendingComments) {
        this.pendingComments = pendingComments;
    }

    public int getFacebookReach() {
        return facebookReach;
    }

    public void setFacebookReach(int facebookReach) {
        this.facebookReach = facebookReach;
    }

    public int getFacebookLikes() {
        return facebookLikes;
    }

    public void setFacebookLikes(int facebookLikes) {
        this.facebookLikes = facebookLikes;
    }

    public int getFacebookComments() {
        return facebookComments;
    }

    public void setFacebookComments(int facebookComments) {
        this.facebookComments = facebookComments;
    }

    public double getFacebookEngagementRate() {
        return facebookEngagementRate;
    }

    public void setFacebookEngagementRate(double facebookEngagementRate) {
        this.facebookEngagementRate = facebookEngagementRate;
    }

    public int getThreadsReach() {
        return threadsReach;
    }

    public void setThreadsReach(int threadsReach) {
        this.threadsReach = threadsReach;
    }

    public int getThreadsLikes() {
        return threadsLikes;
    }

    public void setThreadsLikes(int threadsLikes) {
        this.threadsLikes = threadsLikes;
    }

    public int getThreadsComments() {
        return threadsComments;
    }

    public void setThreadsComments(int threadsComments) {
        this.threadsComments = threadsComments;
    }

    public double getThreadsEngagementRate() {
        return threadsEngagementRate;
    }

    public void setThreadsEngagementRate(double threadsEngagementRate) {
        this.threadsEngagementRate = threadsEngagementRate;
    }

    public String getWeeklyAiSummary() {
        return weeklyAiSummary;
    }

    public void setWeeklyAiSummary(String weeklyAiSummary) {
        this.weeklyAiSummary = weeklyAiSummary;
    }

    public String getTopPerformingPost() {
        return topPerformingPost;
    }

    public void setTopPerformingPost(String topPerformingPost) {
        this.topPerformingPost = topPerformingPost;
    }

    public String getNextWeekAdvice() {
        return nextWeekAdvice;
    }

    public void setNextWeekAdvice(String nextWeekAdvice) {
        this.nextWeekAdvice = nextWeekAdvice;
    }

    public List<DayMetric> getWeeklyTimeline() {
        return weeklyTimeline;
    }

    public void setWeeklyTimeline(List<DayMetric> weeklyTimeline) {
        this.weeklyTimeline = weeklyTimeline;
    }

    // --- HELPER AGGREGATION METRICS FOR OVERVIEW CARDS ---
    public int getTotalReach() {
        return facebookReach + threadsReach;
    }

    public int getTotalLikes() {
        return facebookLikes + threadsLikes;
    }

    public int getTotalComments() {
        return facebookComments + threadsComments;
    }

    public double getAverageEngagementRate() {
        int totalReach = getTotalReach();
        if (totalReach <= 0) return 0.0;
        int totalInteractions = getTotalLikes() + getTotalComments();
        return ((double) totalInteractions / totalReach) * 100.0;
    }
}

