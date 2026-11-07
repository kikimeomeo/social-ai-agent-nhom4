package com.tuankiet.myapplication.model;

import java.io.Serializable;
import java.util.List;

public class AnalyticsData implements Serializable {
    public static class DayMetric implements Serializable {
        private String dayLabel;
        private int facebookEngagement;
        private int linkedinEngagement;

        public DayMetric(String dayLabel, int facebookEngagement, int linkedinEngagement) {
            this.dayLabel = dayLabel;
            this.facebookEngagement = facebookEngagement;
            this.linkedinEngagement = linkedinEngagement;
        }

        public String getDayLabel() {
            return dayLabel;
        }

        public int getFacebookEngagement() {
            return facebookEngagement;
        }

        public int getLinkedinEngagement() {
            return linkedinEngagement;
        }

        public int getTotal() {
            return facebookEngagement + linkedinEngagement;
        }
    }

    private int totalPostsCreated;
    private int weeklyEngagement;
    private int pendingComments;
    private int facebookReach;
    private int facebookLikes;
    private int facebookComments;
    private double facebookEngagementRate;

    private int linkedinReach;
    private int linkedinLikes;
    private int linkedinComments;
    private double linkedinEngagementRate;

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

    public int getLinkedinReach() {
        return linkedinReach;
    }

    public void setLinkedinReach(int linkedinReach) {
        this.linkedinReach = linkedinReach;
    }

    public int getLinkedinLikes() {
        return linkedinLikes;
    }

    public void setLinkedinLikes(int linkedinLikes) {
        this.linkedinLikes = linkedinLikes;
    }

    public int getLinkedinComments() {
        return linkedinComments;
    }

    public void setLinkedinComments(int linkedinComments) {
        this.linkedinComments = linkedinComments;
    }

    public double getLinkedinEngagementRate() {
        return linkedinEngagementRate;
    }

    public void setLinkedinEngagementRate(double linkedinEngagementRate) {
        this.linkedinEngagementRate = linkedinEngagementRate;
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
}

