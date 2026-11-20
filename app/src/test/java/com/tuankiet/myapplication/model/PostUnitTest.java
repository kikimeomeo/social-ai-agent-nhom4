package com.tuankiet.myapplication.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PostUnitTest {

    @Test
    public void testPostIdempotencyKey() {
        Post post = new Post();
        assertNull(post.getIdempotencyKey());

        String key = "test-uuid-1234-5678";
        post.setIdempotencyKey(key);
        assertEquals(key, post.getIdempotencyKey());
    }

    @Test
    public void testPostStatusAndRetryCount() {
        Post post = new Post();
        assertEquals(Post.STATUS_DRAFT, post.getStatus());
        assertEquals(0, post.getRetryCount());

        post.setStatus(Post.STATUS_PENDING);
        assertEquals(Post.STATUS_PENDING, post.getStatus());

        post.setRetryCount(3);
        assertEquals(3, post.getRetryCount());

        post.setPublishError("OAuth token expired (HTTP 401)");
        assertEquals("OAuth token expired (HTTP 401)", post.getPublishError());

        post.setStatus(Post.STATUS_FAILED);
        assertEquals(Post.STATUS_FAILED, post.getStatus());
    }

    @Test
    public void testPostMultiChannelUrlsAndFlags() {
        Post post = new Post();
        post.setTargetFacebook(true);
        post.setTargetThreads(true);

        assertTrue(post.isTargetFacebook());
        assertTrue(post.isTargetThreads());
        assertFalse(post.isPublishedFacebook());
        assertFalse(post.isPublishedThreads());

        post.setFbPostUrl("https://www.facebook.com/123456789");
        post.setThreadsPostUrl("https://www.threads.net/@user/post/987654321");
        post.setPublishedFacebook(true);
        post.setPublishedThreads(true);
        post.setStatus(Post.STATUS_PUBLISHED);

        assertEquals("https://www.facebook.com/123456789", post.getFbPostUrl());
        assertEquals("https://www.threads.net/@user/post/987654321", post.getThreadsPostUrl());
        assertTrue(post.isPublishedFacebook());
        assertTrue(post.isPublishedThreads());
        assertEquals(Post.STATUS_PUBLISHED, post.getStatus());
    }
}
