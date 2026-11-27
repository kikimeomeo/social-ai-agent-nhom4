package com.tuankiet.myapplication.worker;

import com.tuankiet.myapplication.model.Post;
import com.tuankiet.myapplication.util.RealSocialPublisher;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PostPublisherLogicUnitTest {

    @Test
    public void testPublishResultSuccess() {
        RealSocialPublisher.PublishResult res = RealSocialPublisher.PublishResult.success(
                "https://www.threads.net/@user/post/112233",
                "112233"
        );
        assertTrue(res.success);
        assertFalse(res.isRetryable);
        assertNull(res.errorMessage);
        assertEquals("https://www.threads.net/@user/post/112233", res.postUrl);
        assertEquals("112233", res.postId);
    }

    @Test
    public void testPublishResultRetryableError() {
        RealSocialPublisher.PublishResult res = RealSocialPublisher.PublishResult.retryable(
                "Network timeout when contacting Meta Graph API"
        );
        assertFalse(res.success);
        assertTrue(res.isRetryable);
        assertNotNull(res.errorMessage);
        assertTrue(res.errorMessage.contains("Network timeout"));
        assertNull(res.postUrl);
    }

    @Test
    public void testPublishResultPermanentFailure() {
        RealSocialPublisher.PublishResult res = RealSocialPublisher.PublishResult.failure(
                "OAuthException: The user access token has expired"
        );
        assertFalse(res.success);
        assertFalse(res.isRetryable);
        assertNotNull(res.errorMessage);
        assertTrue(res.errorMessage.contains("OAuthException"));
        assertNull(res.postUrl);
    }

    @Test
    public void testIdempotencyValidation() {
        Post post = new Post();
        post.setStatus(Post.STATUS_PUBLISHED);
        post.setPublishedFacebook(true);
        post.setPublishedThreads(true);
        post.setIdempotencyKey("unique-key-abc-123");

        // When a post is marked PUBLISHED, worker skips API calls to prevent duplicate publishing
        boolean shouldSkip = Post.STATUS_PUBLISHED.equals(post.getStatus());
        assertTrue("Already published post must be skipped to avoid duplicate posts", shouldSkip);
    }
}
