package com.tuankiet.myapplication.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.tuankiet.myapplication.model.LLMConfig;
import com.tuankiet.myapplication.model.SocialPlatform;
import com.tuankiet.myapplication.util.AesEncryptionUtil;

import java.nio.charset.StandardCharsets;

public class SecurePreferenceManager {
    private static final String PREF_NAME = "social_ai_secure_prefs";
    private static final String KEY_PROVIDER = "key_provider";
    private static final String KEY_API_KEY = "key_api_key";
    private static final String KEY_DEMO_MODE = "key_demo_mode";
    private static final String KEY_TOKENS = "key_tokens";
    private static final String KEY_COST = "key_cost";
    private static final String LEGACY_ENCRYPTION_PREFIX = "ENC_AES_";

    // OAuth Keys
    private static final String KEY_FB_TOKEN = "oauth_fb_token";
    private static final String KEY_FB_REFRESH = "oauth_fb_refresh";
    private static final String KEY_FB_EXPIRY = "oauth_fb_expiry";

    private static final String KEY_LI_TOKEN = "oauth_li_token";
    private static final String KEY_LI_REFRESH = "oauth_li_refresh";
    private static final String KEY_LI_EXPIRY = "oauth_li_expiry";

    private static final String KEY_THREADS_TOKEN = "threads_access_token";
    private static final String KEY_FB_PAGE_ID = "fb_page_id";
    private static final String KEY_FB_PAGE_TOKEN = "fb_page_access_token";
    private static final String KEY_CONTROLLED_AUTO_REPLY = "controlled_auto_reply_enabled";
    private static final String KEY_LAST_COMMENT_SYNC_TIME = "last_comment_sync_time";

    private final SharedPreferences prefs;
    private static SecurePreferenceManager instance;

    private SecurePreferenceManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SecurePreferenceManager getInstance(Context context) {
        if (instance == null) {
            instance = new SecurePreferenceManager(context);
        }
        return instance;
    }

    // --- HARDWARE-BACKED AES-256-GCM ENCRYPTION HELPERS ---
    private String encryptSecret(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        return AesEncryptionUtil.encrypt(raw);
    }

    private String decryptSecret(String stored) {
        if (stored == null || stored.isEmpty()) return "";

        // Hỗ trợ tương thích ngược nếu chứa tiền tố XOR cũ
        if (stored.startsWith(LEGACY_ENCRYPTION_PREFIX)) {
            try {
                String encoded = stored.substring(LEGACY_ENCRYPTION_PREFIX.length());
                byte[] bytes = Base64.decode(encoded, Base64.NO_WRAP);
                for (int i = 0; i < bytes.length; i++) {
                    bytes[i] = (byte) (bytes[i] ^ 0x5A);
                }
                return new String(bytes, StandardCharsets.UTF_8);
            } catch (Exception e) {
                return stored;
            }
        }

        return AesEncryptionUtil.decrypt(stored);
    }

    public LLMConfig getConfig() {
        String provider = prefs.getString(KEY_PROVIDER, LLMConfig.PROVIDER_GEMINI);
        String rawApiKey = prefs.getString(KEY_API_KEY, "");
        String apiKey = decryptSecret(rawApiKey);
        boolean isDemo = prefs.getBoolean(KEY_DEMO_MODE, true); // Demo mode by default
        int tokens = prefs.getInt(KEY_TOKENS, 12450);
        float cost = prefs.getFloat(KEY_COST, 0.048f);
        return new LLMConfig(provider, apiKey, isDemo, tokens, cost);
    }

    public void saveConfig(String provider, String apiKey, boolean isDemoMode) {
        String encryptedKey = encryptSecret(apiKey != null ? apiKey.trim() : "");
        prefs.edit()
                .putString(KEY_PROVIDER, provider)
                .putString(KEY_API_KEY, encryptedKey)
                .putBoolean(KEY_DEMO_MODE, isDemoMode)
                .apply();
    }

    public void setDemoMode(boolean isDemo) {
        prefs.edit().putBoolean(KEY_DEMO_MODE, isDemo).apply();
    }

    public boolean isDemoMode() {
        return prefs.getBoolean(KEY_DEMO_MODE, true);
    }

    public void addTokens(int tokens, double cost) {
        int currentTokens = prefs.getInt(KEY_TOKENS, 12450);
        float currentCost = prefs.getFloat(KEY_COST, 0.048f);
        prefs.edit()
                .putInt(KEY_TOKENS, currentTokens + tokens)
                .putFloat(KEY_COST, (float) (currentCost + cost))
                .apply();
    }

    public void resetTokenStats() {
        prefs.edit()
                .putInt(KEY_TOKENS, 0)
                .putFloat(KEY_COST, 0.0f)
                .apply();
    }

    // --- OAuth 2.0 Token Storage ---
    public void saveOAuthToken(SocialPlatform platform, String accessToken, String refreshToken, long expiresInSeconds) {
        long expiryTime = System.currentTimeMillis() + (expiresInSeconds * 1000L);
        SharedPreferences.Editor editor = prefs.edit();

        String encAccess = encryptSecret(accessToken);
        String encRefresh = encryptSecret(refreshToken);

        if (platform == SocialPlatform.FACEBOOK) {
            editor.putString(KEY_FB_TOKEN, encAccess);
            editor.putString(KEY_FB_REFRESH, encRefresh);
            editor.putLong(KEY_FB_EXPIRY, expiryTime);
        } else {
            editor.putString(KEY_LI_TOKEN, encAccess);
            editor.putString(KEY_LI_REFRESH, encRefresh);
            editor.putLong(KEY_LI_EXPIRY, expiryTime);
        }
        editor.apply();
    }

    public String getOAuthToken(SocialPlatform platform) {
        String stored = "";
        if (platform == SocialPlatform.FACEBOOK) {
            stored = prefs.getString(KEY_FB_TOKEN, "");
        } else if (platform == SocialPlatform.LINKEDIN) {
            stored = prefs.getString(KEY_LI_TOKEN, "");
        }
        return decryptSecret(stored);
    }

    public String getOAuthRefreshToken(SocialPlatform platform) {
        String stored = "";
        if (platform == SocialPlatform.FACEBOOK) {
            stored = prefs.getString(KEY_FB_REFRESH, "");
        } else if (platform == SocialPlatform.LINKEDIN) {
            stored = prefs.getString(KEY_LI_REFRESH, "");
        }
        return decryptSecret(stored);
    }

    public boolean isTokenExpired(SocialPlatform platform) {
        String token = getOAuthToken(platform);

        if (token == null || token.trim().isEmpty()) {
            return true;
        }

        long expiry;

        if (platform == SocialPlatform.FACEBOOK) {
            expiry = prefs.getLong(KEY_FB_EXPIRY, 0L);
        } else if (platform == SocialPlatform.LINKEDIN) {
            expiry = prefs.getLong(KEY_LI_EXPIRY, 0L);
        } else {
            return true;
        }

        return expiry <= 0L || System.currentTimeMillis() >= expiry;
    }

    public void clearOAuthToken(SocialPlatform platform) {
        SharedPreferences.Editor editor = prefs.edit();
        if (platform == SocialPlatform.FACEBOOK) {
            editor.remove(KEY_FB_TOKEN).remove(KEY_FB_REFRESH).remove(KEY_FB_EXPIRY);
        } else {
            editor.remove(KEY_LI_TOKEN).remove(KEY_LI_REFRESH).remove(KEY_LI_EXPIRY);
        }
        editor.apply();
    }

    public void saveThreadsToken(String token) {
        String clean = token != null ? token.trim() : "";
        String encToken = encryptSecret(clean);
        prefs.edit().putString(KEY_THREADS_TOKEN, encToken).apply();
    }

    public String getThreadsToken() {
        String stored = prefs.getString(KEY_THREADS_TOKEN, "");
        if (stored.isEmpty()) return "";
        return decryptSecret(stored);
    }

    public void clearThreadsToken() {
        prefs.edit()
                .remove(KEY_THREADS_TOKEN)
                .apply();
    }

    // --- Facebook Page Graph API Credentials ---
    public void saveFacebookPageCredentials(String pageId, String pageAccessToken) {
        String cleanPageId = pageId != null ? pageId.trim() : "";
        String cleanToken = pageAccessToken != null ? pageAccessToken.trim() : "";
        prefs.edit()
                .putString(KEY_FB_PAGE_ID, cleanPageId)
                .putString(KEY_FB_PAGE_TOKEN, encryptSecret(cleanToken))
                .apply();
    }

    public String getFacebookPageId() {
        return prefs.getString(KEY_FB_PAGE_ID, "").trim();
    }

    public String getFacebookPageToken() {
        String stored = prefs.getString(KEY_FB_PAGE_TOKEN, "");
        if (!stored.isEmpty()) {
            return decryptSecret(stored);
        }
        return getOAuthToken(SocialPlatform.FACEBOOK);
    }

    public void clearFacebookPageCredentials() {
        prefs.edit()
                .remove(KEY_FB_PAGE_ID)
                .remove(KEY_FB_PAGE_TOKEN)
                .apply();
    }

    // --- F6 Controlled Auto-Reply & Sync State ---
    public boolean isControlledAutoReplyEnabled() {
        return prefs.getBoolean(KEY_CONTROLLED_AUTO_REPLY, true);
    }

    public void setControlledAutoReplyEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_CONTROLLED_AUTO_REPLY, enabled).apply();
    }

    public long getLastCommentSyncTime() {
        return prefs.getLong(KEY_LAST_COMMENT_SYNC_TIME, 0L);
    }

    public void setLastCommentSyncTime(long timestampMillis) {
        prefs.edit().putLong(KEY_LAST_COMMENT_SYNC_TIME, timestampMillis).apply();
    }
}
