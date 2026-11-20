package com.tuankiet.myapplication.ui.inbox;

import com.tuankiet.myapplication.model.CommentItem;
import com.tuankiet.myapplication.model.SocialPlatform;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure filtering logic for Inbox comments across 3 dimensions:
 * 1. Channel (ALL [Facebook + Threads], FACEBOOK, THREADS)
 * 2. Post (ALL or exact postTitle)
 * 3. Status (ALL, UNANSWERED, ANSWERED)
 */
public final class InboxFilterHelper {

    public static final String CHANNEL_ALL = "ALL";
    public static final String CHANNEL_FACEBOOK = "FACEBOOK";
    public static final String CHANNEL_THREADS = "THREADS";

    public static final String POST_ALL = "ALL";

    public static final String STATUS_ALL = "ALL";
    public static final String STATUS_UNANSWERED = "UNANSWERED";
    public static final String STATUS_ANSWERED = "ANSWERED";

    private InboxFilterHelper() {
    }

    public static boolean matchesChannel(CommentItem item, String channelFilter) {
        if (item == null || item.getPlatform() == null) {
            return false;
        }
        SocialPlatform platform = item.getPlatform();
        if (channelFilter == null || CHANNEL_ALL.equalsIgnoreCase(channelFilter)) {
            return platform == SocialPlatform.FACEBOOK || platform == SocialPlatform.THREADS;
        }
        if (CHANNEL_FACEBOOK.equalsIgnoreCase(channelFilter)) {
            return platform == SocialPlatform.FACEBOOK;
        }
        if (CHANNEL_THREADS.equalsIgnoreCase(channelFilter)) {
            return platform == SocialPlatform.THREADS;
        }
        return false;
    }

    public static boolean matchesPost(CommentItem item, String postFilter) {
        if (postFilter == null || postFilter.trim().isEmpty() || POST_ALL.equalsIgnoreCase(postFilter)) {
            return true;
        }
        return item != null
                && item.getPostTitle() != null
                && item.getPostTitle().trim().equalsIgnoreCase(postFilter.trim());
    }

    public static boolean matchesStatus(CommentItem item, String statusFilter) {
        if (statusFilter == null || STATUS_ALL.equalsIgnoreCase(statusFilter)) {
            return true;
        }
        if (item == null) {
            return false;
        }
        if (STATUS_UNANSWERED.equalsIgnoreCase(statusFilter)) {
            return !item.isAnswered();
        }
        if (STATUS_ANSWERED.equalsIgnoreCase(statusFilter)) {
            return item.isAnswered();
        }
        return true;
    }

    public static List<CommentItem> filter(List<CommentItem> source,
                                           String channelFilter,
                                           String postFilter,
                                           String statusFilter) {
        List<CommentItem> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (CommentItem item : source) {
            if (matchesChannel(item, channelFilter)
                    && matchesPost(item, postFilter)
                    && matchesStatus(item, statusFilter)) {
                result.add(item);
            }
        }
        return result;
    }

    public static List<String> extractDistinctPosts(List<CommentItem> source, String channelFilter) {
        Set<String> titles = new LinkedHashSet<>();
        if (source != null) {
            for (CommentItem item : source) {
                if (matchesChannel(item, channelFilter)
                        && item.getPostTitle() != null
                        && !item.getPostTitle().trim().isEmpty()) {
                    titles.add(item.getPostTitle().trim());
                }
            }
        }
        return new ArrayList<>(titles);
    }

    /**
     * Returns [total, unanswered, answered] for the active channel and post filter.
     */
    public static int[] countByStatus(List<CommentItem> source, String channelFilter, String postFilter) {
        int total = 0;
        int unanswered = 0;
        int answered = 0;
        if (source != null) {
            for (CommentItem item : source) {
                if (matchesChannel(item, channelFilter) && matchesPost(item, postFilter)) {
                    total++;
                    if (item.isAnswered()) {
                        answered++;
                    } else {
                        unanswered++;
                    }
                }
            }
        }
        return new int[]{total, unanswered, answered};
    }
}
