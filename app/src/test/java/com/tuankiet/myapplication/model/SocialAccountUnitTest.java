package com.tuankiet.myapplication.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class SocialAccountUnitTest {

    @Test
    public void testSocialPlatforms() {
        assertEquals("Facebook Page", SocialPlatform.FACEBOOK.getDisplayName());
        assertEquals("Facebook", SocialPlatform.FACEBOOK.getShortName());

        assertEquals("Threads Profile", SocialPlatform.THREADS.getDisplayName());
        assertEquals("Threads", SocialPlatform.THREADS.getShortName());
    }

    @Test
    public void testSocialAccountEntity() {
        SocialAccount acc = new SocialAccount(
                "acc_threads_1",
                SocialPlatform.THREADS,
                "Tech Lead",
                "@techlead",
                true,
                8920,
                "T",
                "Connected"
        );

        assertEquals("acc_threads_1", acc.getId());
        assertEquals(SocialPlatform.THREADS, acc.getPlatform());
        assertEquals("Tech Lead", acc.getAccountName());
        assertEquals("@techlead", acc.getHandle());
        assertTrue(acc.isConnected());
        assertEquals(8920, acc.getFollowerCount());
        assertEquals("T", acc.getAvatarInitial());
        assertEquals("Connected", acc.getStatusMessage());

        acc.setConnected(false);
        acc.setStatusMessage("Disconnected");
        assertFalse(acc.isConnected());
        assertEquals("Disconnected", acc.getStatusMessage());
    }
}
