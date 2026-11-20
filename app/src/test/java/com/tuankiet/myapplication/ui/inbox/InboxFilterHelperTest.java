package com.tuankiet.myapplication.ui.inbox;

import com.tuankiet.myapplication.model.CommentItem;
import com.tuankiet.myapplication.model.SocialPlatform;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InboxFilterHelperTest {

    private List<CommentItem> sampleComments;

    @Before
    public void setUp() {
        sampleComments = new ArrayList<>();
        sampleComments.add(makeComment(1, SocialPlatform.FACEBOOK, "Bài AI 2026", false));
        sampleComments.add(makeComment(2, SocialPlatform.THREADS, "Bài AI 2026", false));
        sampleComments.add(makeComment(3, SocialPlatform.FACEBOOK, "Bài On-Device AI", true));
        sampleComments.add(makeComment(4, SocialPlatform.THREADS, "Bài On-Device AI", true));
        sampleComments.add(makeComment(5, SocialPlatform.LINKEDIN, "Bài Legacy LinkedIn", false));
    }

    private CommentItem makeComment(long id, SocialPlatform platform, String postTitle, boolean answered) {
        CommentItem item = new CommentItem();
        item.setId(id);
        item.setPlatform(platform);
        item.setPostTitle(postTitle);
        item.setAnswered(answered);
        return item;
    }

    @Test
    public void filterByChannel_supportsAllFacebookThreadsAndExcludesLinkedIn() {
        List<CommentItem> all = InboxFilterHelper.filter(
                sampleComments, InboxFilterHelper.CHANNEL_ALL, InboxFilterHelper.POST_ALL, InboxFilterHelper.STATUS_ALL);
        assertEquals(4, all.size());

        List<CommentItem> fb = InboxFilterHelper.filter(
                sampleComments, InboxFilterHelper.CHANNEL_FACEBOOK, InboxFilterHelper.POST_ALL, InboxFilterHelper.STATUS_ALL);
        assertEquals(2, fb.size());
        for (CommentItem c : fb) {
            assertEquals(SocialPlatform.FACEBOOK, c.getPlatform());
        }

        List<CommentItem> threads = InboxFilterHelper.filter(
                sampleComments, InboxFilterHelper.CHANNEL_THREADS, InboxFilterHelper.POST_ALL, InboxFilterHelper.STATUS_ALL);
        assertEquals(2, threads.size());
        for (CommentItem c : threads) {
            assertEquals(SocialPlatform.THREADS, c.getPlatform());
        }
    }

    @Test
    public void filterByPostAndStatus_combinesAllThreeDimensions() {
        List<CommentItem> unansweredPost1 = InboxFilterHelper.filter(
                sampleComments,
                InboxFilterHelper.CHANNEL_ALL,
                "Bài AI 2026",
                InboxFilterHelper.STATUS_UNANSWERED
        );
        assertEquals(2, unansweredPost1.size());

        List<CommentItem> fbUnansweredPost1 = InboxFilterHelper.filter(
                sampleComments,
                InboxFilterHelper.CHANNEL_FACEBOOK,
                "Bài AI 2026",
                InboxFilterHelper.STATUS_UNANSWERED
        );
        assertEquals(1, fbUnansweredPost1.size());
        assertEquals(1L, fbUnansweredPost1.get(0).getId());

        List<CommentItem> answeredThreads = InboxFilterHelper.filter(
                sampleComments,
                InboxFilterHelper.CHANNEL_THREADS,
                InboxFilterHelper.POST_ALL,
                InboxFilterHelper.STATUS_ANSWERED
        );
        assertEquals(1, answeredThreads.size());
        assertEquals(4L, answeredThreads.get(0).getId());
    }

    @Test
    public void extractDistinctPostsAndCountByStatus_returnsAccurateResults() {
        List<String> posts = InboxFilterHelper.extractDistinctPosts(sampleComments, InboxFilterHelper.CHANNEL_ALL);
        assertEquals(2, posts.size());
        assertTrue(posts.contains("Bài AI 2026"));
        assertTrue(posts.contains("Bài On-Device AI"));

        int[] counts = InboxFilterHelper.countByStatus(
                sampleComments, InboxFilterHelper.CHANNEL_ALL, InboxFilterHelper.POST_ALL);
        assertEquals(4, counts[0]);
        assertEquals(2, counts[1]);
        assertEquals(2, counts[2]);
    }
}
