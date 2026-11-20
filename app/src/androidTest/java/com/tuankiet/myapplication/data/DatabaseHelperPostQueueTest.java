package com.tuankiet.myapplication.data;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.tuankiet.myapplication.model.Post;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class DatabaseHelperPostQueueTest {

    private DatabaseHelper dbHelper;
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        dbHelper = DatabaseHelper.getInstance(context);
    }

    @Test
    public void testInsertAndGetPendingPosts() {
        String testKey = "idempotency-" + UUID.randomUUID().toString();
        Post post = new Post();
        post.setTopic("Test F5 Offline Queue Post");
        post.setTone("Chuyên gia");
        post.setContentFacebook("Nội dung bài viết Facebook F5");
        post.setContentThreads("Nội dung bài viết Threads F5");
        post.setStatus(Post.STATUS_PENDING);
        post.setTargetFacebook(true);
        post.setTargetThreads(true);
        post.setIdempotencyKey(testKey);

        long id = dbHelper.insertPost(post);
        assertTrue(id > 0);

        List<Post> pendingPosts = dbHelper.getPendingPosts();
        assertNotNull(pendingPosts);
        boolean found = false;
        for (Post p : pendingPosts) {
            if (testKey.equals(p.getIdempotencyKey())) {
                found = true;
                assertEquals(Post.STATUS_PENDING, p.getStatus());
                assertEquals("Test F5 Offline Queue Post", p.getTopic());
                break;
            }
        }
        assertTrue("Post must be found in pending queue", found);
    }

    @Test
    public void testGetPostByIdempotencyKeyAndPostUpdate() {
        String testKey = "idempotency-unique-" + UUID.randomUUID().toString();
        Post post = new Post();
        post.setTopic("Test Idempotency Key Search");
        post.setStatus(Post.STATUS_PENDING);
        post.setIdempotencyKey(testKey);

        long id = dbHelper.insertPost(post);
        assertTrue(id > 0);

        Post retrieved = dbHelper.getPostByIdempotencyKey(testKey);
        assertNotNull("Post should be retrieved by idempotency key", retrieved);
        assertEquals(testKey, retrieved.getIdempotencyKey());

        // Update post with published state and permalink
        retrieved.setStatus(Post.STATUS_PUBLISHED);
        retrieved.setFbPostUrl("https://facebook.com/socialai/posts/999888");
        retrieved.setThreadsPostUrl("https://www.threads.net/@user/post/888777");
        retrieved.setPublishedFacebook(true);
        retrieved.setPublishedThreads(true);
        retrieved.setRetryCount(2);

        dbHelper.updatePost(retrieved);

        Post updated = dbHelper.getPostById(retrieved.getId());
        assertNotNull(updated);
        assertEquals(Post.STATUS_PUBLISHED, updated.getStatus());
        assertEquals("https://facebook.com/socialai/posts/999888", updated.getFbPostUrl());
        assertEquals("https://www.threads.net/@user/post/888777", updated.getThreadsPostUrl());
        assertTrue(updated.isPublishedFacebook());
        assertTrue(updated.isPublishedThreads());
        assertEquals(2, updated.getRetryCount());
    }
}
