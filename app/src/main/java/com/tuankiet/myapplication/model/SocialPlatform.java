package com.tuankiet.myapplication.model;

public enum SocialPlatform {
    FACEBOOK("Facebook Page", "Facebook"),
    LINKEDIN("LinkedIn Profile", "LinkedIn");

    private final String displayName;
    private final String shortName;

    SocialPlatform(String displayName, String shortName) {
        this.displayName = displayName;
        this.shortName = shortName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getShortName() {
        return shortName;
    }
}

