package com.tuankiet.myapplication.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.tuankiet.myapplication.model.LLMConfig;
import com.tuankiet.myapplication.model.SocialPlatform;

public class SecurePreferenceManager {
    private static final String PREF_NAME = "social_ai_secure_prefs";
    private static final String KEY_PROVIDER = "key_provider";
    private static final String KEY_API_KEY = "key_api_key";
    private static final String KEY_DEMO_MODE = "key_demo_mode";
    private static final String KEY_TOKENS = "key_tokens";
    private static final String KEY_COST = "key_cost";

    // OAuth Keys
    private static final String KEY_FB_TOKEN = "oauth_fb_token";
    private static final String KEY_FB_REFRESH = "oauth_fb_refresh";
    private static final String KEY_FB_EXPIRY = "oauth_fb_expiry";

    private static final String KEY_LI_TOKEN = "oauth_li_token";
    private static final String KEY_LI_REFRESH = "oauth_li_refresh";
    private static final String KEY_LI_EXPIRY = "oauth_li_expiry";

    private static final String KEY_THREADS_TOKEN = "threads_access_token";

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

    public LLMConfig getConfig() {
        String provider = prefs.getString(KEY_PROVIDER, LLMConfig.PROVIDER_GEMINI);
        String apiKey = prefs.getString(KEY_API_KEY, "");
        boolean isDemo = prefs.getBoolean(KEY_DEMO_MODE, true); // Demo mode by default
        int tokens = prefs.getInt(KEY_TOKENS, 12450);
        float cost = prefs.getFloat(KEY_COST, 0.048f);
        return new LLMConfig(provider, apiKey, isDemo, tokens, cost);
    }

    public void saveConfig(String provider, String apiKey, boolean isDemoMode) {
        prefs.edit()
                .putString(KEY_PROVIDER, provider)
                .putString(KEY_API_KEY, apiKey)
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
        if (platform == SocialPlatform.FACEBOOK) {
            editor.putString(KEY_FB_TOKEN, accessToken);
            editor.putString(KEY_FB_REFRESH, refreshToken);
            editor.putLong(KEY_FB_EXPIRY, expiryTime);
        } else {
            editor.putString(KEY_LI_TOKEN, accessToken);
            editor.putString(KEY_LI_REFRESH, refreshToken);
            editor.putLong(KEY_LI_EXPIRY, expiryTime);
        }
        editor.apply();
    }

    public String getOAuthToken(SocialPlatform platform) {
        if (platform == SocialPlatform.FACEBOOK) {
            return prefs.getString(KEY_FB_TOKEN, "");
        }

        if (platform == SocialPlatform.LINKEDIN) {
            return prefs.getString(KEY_LI_TOKEN, "");
        }

        return "";
    }

    public String getOAuthRefreshToken(SocialPlatform platform) {
        if (platform == SocialPlatform.FACEBOOK) {
            return prefs.getString(KEY_FB_REFRESH, "");
        }

        if (platform == SocialPlatform.LINKEDIN) {
            return prefs.getString(KEY_LI_REFRESH, "");
        }

        return "";
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
        prefs.edit().putString(KEY_THREADS_TOKEN, token != null ? token.trim() : "").apply();
    }

    public String getThreadsToken() {
        return prefs.getString(KEY_THREADS_TOKEN, "");
    }

    public void clearThreadsToken() {
        prefs.edit()
                .remove(KEY_THREADS_TOKEN)
                .apply();
    }
}
