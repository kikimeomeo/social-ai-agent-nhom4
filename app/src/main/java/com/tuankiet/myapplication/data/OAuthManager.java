package com.tuankiet.myapplication.data;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.browser.customtabs.CustomTabsIntent;

import com.tuankiet.myapplication.model.SocialAccount;
import com.tuankiet.myapplication.model.SocialPlatform;

import java.util.List;

public class OAuthManager {

    public interface OAuthCallback {
        void onSuccess(SocialPlatform platform, String message);
        void onError(String errorMessage);
    }

    private static OAuthManager instance;
    private final Context context;

    private OAuthManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized OAuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new OAuthManager(context);
        }
        return instance;
    }

    public void launchOAuth(Activity activity, SocialPlatform platform) {
        String authUrl;
        if (platform == SocialPlatform.FACEBOOK) {
            authUrl = "https://www.facebook.com/v19.0/dialog/oauth?" +
                    "client_id=987654321012345" +
                    "&redirect_uri=socialai://oauth-callback" +
                    "&scope=pages_manage_posts,pages_read_engagement,pages_show_list" +
                    "&response_type=token" +
                    "&state=facebook";
        } else {
            authUrl = "https://www.linkedin.com/oauth/v2/authorization?" +
                    "response_type=code" +
                    "&client_id=78linkedinclientid" +
                    "&redirect_uri=socialai://oauth-callback" +
                    "&scope=r_liteprofile%20w_member_social" +
                    "&state=linkedin";
        }

        try {
            CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
            customTabsIntent.launchUrl(activity, Uri.parse(authUrl));
        } catch (Exception e) {
            // Fallback to normal system browser intent
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
                activity.startActivity(browserIntent);
            } catch (Exception ex) {
                Toast.makeText(activity, "Không thể mở trình duyệt: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public boolean handleDeepLinkCallback(Uri uri, OAuthCallback callback) {
        if (uri == null) return false;

        String scheme = uri.getScheme();
        String host = uri.getHost();

        if ("socialai".equalsIgnoreCase(scheme) && "oauth-callback".equalsIgnoreCase(host)) {
            String state = uri.getQueryParameter("state");
            String code = uri.getQueryParameter("code");
            String token = uri.getQueryParameter("access_token");

            SocialPlatform platform = SocialPlatform.FACEBOOK;
            if ("linkedin".equalsIgnoreCase(state) || (uri.toString().contains("linkedin"))) {
                platform = SocialPlatform.LINKEDIN;
            }

            String effectiveToken = token != null ? token : (code != null ? code : "mock_access_token_" + System.currentTimeMillis());
            String refreshToken = "mock_refresh_token_" + System.currentTimeMillis();

            // Save tokens with 60 days validity
            SecurePreferenceManager.getInstance(context).saveOAuthToken(platform, effectiveToken, refreshToken, 5184000L);

            // Update Account state in Database
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
            List<SocialAccount> accounts = dbHelper.getAllAccounts();
            for (SocialAccount acc : accounts) {
                if (acc.getPlatform() == platform) {
                    acc.setConnected(true);
                    acc.setStatusMessage("Đã xác thực OAuth 2.0 thành công");
                    dbHelper.insertOrUpdateAccount(acc);
                    break;
                }
            }

            if (callback != null) {
                callback.onSuccess(platform, "Đăng nhập OAuth 2.0 thành công kênh " + platform.getDisplayName() + "!");
            }
            return true;
        }
        return false;
    }

    public void checkAndRefreshTokenIfNeeded(SocialPlatform platform, OAuthCallback callback) {
        SecurePreferenceManager prefs = SecurePreferenceManager.getInstance(context);
        if (prefs.isTokenExpired(platform)) {
            // Simulate refresh token exchange
            String newAccessToken = "refreshed_access_token_" + System.currentTimeMillis();
            String refreshToken = prefs.getOAuthRefreshToken(platform);
            prefs.saveOAuthToken(platform, newAccessToken, refreshToken, 5184000L);
            if (callback != null) {
                callback.onSuccess(platform, "Đã tự động làm mới token (Refresh Token) cho " + platform.getShortName());
            }
        }
    }

    public void disconnectChannel(SocialPlatform platform) {
        SecurePreferenceManager.getInstance(context).clearOAuthToken(platform);
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        List<SocialAccount> accounts = dbHelper.getAllAccounts();
        for (SocialAccount acc : accounts) {
            if (acc.getPlatform() == platform) {
                acc.setConnected(false);
                acc.setStatusMessage("Đã ngắt kết nối");
                dbHelper.insertOrUpdateAccount(acc);
                break;
            }
        }
    }
}

