package com.tuankiet.myapplication.data;

import android.content.Context;
import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.tuankiet.myapplication.model.SocialAccount;
import com.tuankiet.myapplication.model.SocialPlatform;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class OAuthManagerTest {

    private Context context;
    private OAuthManager oauthManager;
    private SecurePreferenceManager prefManager;
    private DatabaseHelper dbHelper;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        oauthManager = OAuthManager.getInstance(context);
        prefManager = SecurePreferenceManager.getInstance(context);
        dbHelper = DatabaseHelper.getInstance(context);
    }

    @Test
    public void testDeepLinkErrorHandling() throws InterruptedException {
        Uri errorUri = Uri.parse("socialai://oauth-callback?error=access_denied&error_description=User+cancelled");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean errorHandled = new AtomicBoolean(false);

        boolean handled = oauthManager.handleDeepLinkCallback(errorUri, new OAuthManager.OAuthCallback() {
            @Override
            public void onSuccess(SocialPlatform platform, String message) {
                latch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                errorHandled.set(true);
                assertTrue(errorMessage.contains("cancelled") || errorMessage.contains("access_denied") || errorMessage.contains("huỷ"));
                latch.countDown();
            }
        });

        assertTrue(handled);
        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertTrue(errorHandled.get());
    }

    @Test
    public void testFacebookTokenStorageAndExpiry() {
        String testPageId = "fb_page_123456";
        String testPageName = "AI Tech Test Fanpage";
        String testToken = "EAAGtest_access_token_fb";

        prefManager.saveFacebookToken(testPageId, testPageName, testToken, 3600);

        assertEquals(testToken, prefManager.getFacebookToken());
        assertEquals(testPageId, prefManager.getFacebookPageId());
        assertEquals(testPageName, prefManager.getFacebookPageName());
        assertFalse(prefManager.isFacebookTokenExpired());

        prefManager.clearFacebookToken();
        assertTrue(prefManager.getFacebookToken().isEmpty());
        assertTrue(prefManager.isFacebookTokenExpired());
    }

    @Test
    public void testThreadsAccountStorageAndExpiry() {
        String testUserId = "th_user_999";
        String testUsername = "aitech_threads_test";
        String testToken = "THQtest_threads_token";

        prefManager.saveThreadsAccount(testUserId, testUsername, testToken, "", 7200);

        assertEquals(testToken, prefManager.getThreadsToken());
        assertEquals(testUserId, prefManager.getThreadsUserId());
        assertEquals(testUsername, prefManager.getThreadsUsername());
        assertFalse(prefManager.isThreadsTokenExpired());

        prefManager.clearThreadsToken();
        assertTrue(prefManager.getThreadsToken().isEmpty());
        assertTrue(prefManager.isThreadsTokenExpired());
    }

    @Test
    public void testDisconnectChannelUpdatesDatabase() {
        SocialAccount acc = new SocialAccount("acc_fb_1", SocialPlatform.FACEBOOK, "Test Page", "@test", true, 500, "F", "Connected");
        dbHelper.insertOrUpdateAccount(acc);

        oauthManager.disconnectChannel(SocialPlatform.FACEBOOK);

        boolean found = false;
        for (SocialAccount a : dbHelper.getAllAccounts()) {
            if (a.getPlatform() == SocialPlatform.FACEBOOK) {
                assertFalse(a.isConnected());
                assertTrue(a.getStatusMessage().contains("ngắt kết nối"));
                found = true;
                break;
            }
        }
        assertTrue(found);
    }
}
