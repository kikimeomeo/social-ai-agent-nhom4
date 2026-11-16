package com.tuankiet.myapplication.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.tuankiet.myapplication.model.AgentLog;
import com.tuankiet.myapplication.model.CommentItem;
import com.tuankiet.myapplication.model.Post;
import com.tuankiet.myapplication.model.SocialAccount;
import com.tuankiet.myapplication.model.SocialPlatform;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "social_ai.db";
    private static final int DATABASE_VERSION = 2;

    private static DatabaseHelper instance;

    private static void createThreadsPublicationsTable(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS threads_publications (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "threads_post_id TEXT NOT NULL UNIQUE, " +
                        "permalink TEXT, " +
                        "content TEXT NOT NULL, " +
                        "published_at INTEGER NOT NULL" +
                        ")"
        );
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Table Posts
        db.execSQL("CREATE TABLE posts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "topic TEXT, " +
                "tone TEXT, " +
                "content_fb TEXT, " +
                "content_li TEXT, " +
                "status TEXT, " +
                "target_fb INTEGER, " +
                "target_li INTEGER, " +
                "created_at INTEGER, " +
                "image_url TEXT, " +
                "image_prompt TEXT, " +
                "reach INTEGER, " +
                "likes INTEGER, " +
                "comments INTEGER, " +
                "shares INTEGER)");

        // Table Comments
        db.execSQL("CREATE TABLE comments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "platform TEXT, " +
                "post_title TEXT, " +
                "author_name TEXT, " +
                "author_initial TEXT, " +
                "content TEXT, " +
                "time_ago TEXT, " +
                "category TEXT, " +
                "is_answered INTEGER, " +
                "reply_text TEXT, " +
                "ai_suggested_reply TEXT, " +
                "is_negative_flagged INTEGER, " +
                "risk_warning TEXT)");

        // Table Accounts
        db.execSQL("CREATE TABLE accounts (" +
                "id TEXT PRIMARY KEY, " +
                "platform TEXT, " +
                "account_name TEXT, " +
                "handle TEXT, " +
                "is_connected INTEGER, " +
                "follower_count INTEGER, " +
                "avatar_initial TEXT, " +
                "status_message TEXT)");

        // Table Agent Logs
        db.execSQL("CREATE TABLE agent_logs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "timestamp INTEGER, " +
                "step_name TEXT, " +
                "agent_role TEXT, " +
                "prompt_used TEXT, " +
                "output_snippet TEXT, " +
                "tokens_used INTEGER, " +
                "cost_estimate REAL, " +
                "duration_ms INTEGER)");

        createThreadsPublicationsTable(db);
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion
    ) {
        if (oldVersion < 2) {
            createThreadsPublicationsTable(db);
        }
    }

    public long saveThreadsPublication(
            String threadsPostId,
            String permalink,
            String content
    ) {
        if (threadsPostId == null || threadsPostId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Threads Post ID không được để trống"
            );
        }

        ContentValues cv = new ContentValues();

        cv.put("threads_post_id", threadsPostId.trim());
        cv.put("permalink", permalink != null ? permalink.trim() : "");
        cv.put("content", content != null ? content : "");
        cv.put("published_at", System.currentTimeMillis());

        SQLiteDatabase db = getWritableDatabase();

        return db.insertWithOnConflict(
                "threads_publications",
                null,
                cv,
                SQLiteDatabase.CONFLICT_REPLACE
        );
    }

    // --- Posts Operations ---
    public long insertPost(Post post) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("topic", post.getTopic());
        cv.put("tone", post.getTone());
        cv.put("content_fb", post.getContentFacebook());
        cv.put("content_li", post.getContentLinkedIn());
        cv.put("status", post.getStatus());
        cv.put("target_fb", post.isTargetFacebook() ? 1 : 0);
        cv.put("target_li", post.isTargetLinkedIn() ? 1 : 0);
        cv.put("created_at", post.getCreatedAt());
        cv.put("image_url", post.getImageUrl());
        cv.put("image_prompt", post.getImagePrompt());
        cv.put("reach", post.getReachCount());
        cv.put("likes", post.getLikeCount());
        cv.put("comments", post.getCommentCount());
        cv.put("shares", post.getShareCount());
        return db.insert("posts", null, cv);
    }

    public List<Post> getAllPosts() {
        List<Post> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM posts ORDER BY created_at DESC", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Post post = new Post();
                post.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
                post.setTopic(cursor.getString(cursor.getColumnIndexOrThrow("topic")));
                post.setTone(cursor.getString(cursor.getColumnIndexOrThrow("tone")));
                post.setContentFacebook(cursor.getString(cursor.getColumnIndexOrThrow("content_fb")));
                post.setContentLinkedIn(cursor.getString(cursor.getColumnIndexOrThrow("content_li")));
                post.setStatus(cursor.getString(cursor.getColumnIndexOrThrow("status")));
                post.setTargetFacebook(cursor.getInt(cursor.getColumnIndexOrThrow("target_fb")) == 1);
                post.setTargetLinkedIn(cursor.getInt(cursor.getColumnIndexOrThrow("target_li")) == 1);
                post.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow("created_at")));
                post.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow("image_url")));
                post.setImagePrompt(cursor.getString(cursor.getColumnIndexOrThrow("image_prompt")));
                post.setReachCount(cursor.getInt(cursor.getColumnIndexOrThrow("reach")));
                post.setLikeCount(cursor.getInt(cursor.getColumnIndexOrThrow("likes")));
                post.setCommentCount(cursor.getInt(cursor.getColumnIndexOrThrow("comments")));
                post.setShareCount(cursor.getInt(cursor.getColumnIndexOrThrow("shares")));
                list.add(post);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public void updatePost(Post post) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("topic", post.getTopic());
        cv.put("tone", post.getTone());
        cv.put("content_fb", post.getContentFacebook());
        cv.put("content_li", post.getContentLinkedIn());
        cv.put("status", post.getStatus());
        cv.put("target_fb", post.isTargetFacebook() ? 1 : 0);
        cv.put("target_li", post.isTargetLinkedIn() ? 1 : 0);
        cv.put("reach", post.getReachCount());
        cv.put("likes", post.getLikeCount());
        cv.put("comments", post.getCommentCount());
        cv.put("shares", post.getShareCount());
        db.update("posts", cv, "id = ?", new String[]{String.valueOf(post.getId())});
    }

    // --- Comments Operations ---
    public long insertComment(CommentItem item) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("platform", item.getPlatform().name());
        cv.put("post_title", item.getPostTitle());
        cv.put("author_name", item.getAuthorName());
        cv.put("author_initial", item.getAuthorInitial());
        cv.put("content", item.getContent());
        cv.put("time_ago", item.getTimeAgo());
        cv.put("category", item.getCategory());
        cv.put("is_answered", item.isAnswered() ? 1 : 0);
        cv.put("reply_text", item.getReplyText());
        cv.put("ai_suggested_reply", item.getAiSuggestedReply());
        cv.put("is_negative_flagged", item.isNegativeFlagged() ? 1 : 0);
        cv.put("risk_warning", item.getRiskWarning());
        return db.insert("comments", null, cv);
    }

    public List<CommentItem> getAllComments() {
        List<CommentItem> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM comments ORDER BY id DESC", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                CommentItem item = new CommentItem();
                item.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
                String platStr = cursor.getString(cursor.getColumnIndexOrThrow("platform"));
                item.setPlatform(SocialPlatform.valueOf(platStr));
                item.setPostTitle(cursor.getString(cursor.getColumnIndexOrThrow("post_title")));
                item.setAuthorName(cursor.getString(cursor.getColumnIndexOrThrow("author_name")));
                item.setAuthorInitial(cursor.getString(cursor.getColumnIndexOrThrow("author_initial")));
                item.setContent(cursor.getString(cursor.getColumnIndexOrThrow("content")));
                item.setTimeAgo(cursor.getString(cursor.getColumnIndexOrThrow("time_ago")));
                item.setCategory(cursor.getString(cursor.getColumnIndexOrThrow("category")));
                item.setAnswered(cursor.getInt(cursor.getColumnIndexOrThrow("is_answered")) == 1);
                item.setReplyText(cursor.getString(cursor.getColumnIndexOrThrow("reply_text")));
                item.setAiSuggestedReply(cursor.getString(cursor.getColumnIndexOrThrow("ai_suggested_reply")));
                item.setNegativeFlagged(cursor.getInt(cursor.getColumnIndexOrThrow("is_negative_flagged")) == 1);
                item.setRiskWarning(cursor.getString(cursor.getColumnIndexOrThrow("risk_warning")));
                list.add(item);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public void updateCommentReply(long commentId, String replyText) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("is_answered", 1);
        cv.put("reply_text", replyText);
        db.update("comments", cv, "id = ?", new String[]{String.valueOf(commentId)});
    }

    // --- Accounts Operations ---
    public void insertOrUpdateAccount(SocialAccount account) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("id", account.getId());
        cv.put("platform", account.getPlatform().name());
        cv.put("account_name", account.getAccountName());
        cv.put("handle", account.getHandle());
        cv.put("is_connected", account.isConnected() ? 1 : 0);
        cv.put("follower_count", account.getFollowerCount());
        cv.put("avatar_initial", account.getAvatarInitial());
        cv.put("status_message", account.getStatusMessage());
        db.insertWithOnConflict("accounts", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<SocialAccount> getAllAccounts() {
        List<SocialAccount> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM accounts", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                SocialAccount acc = new SocialAccount();
                acc.setId(cursor.getString(cursor.getColumnIndexOrThrow("id")));
                acc.setPlatform(SocialPlatform.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("platform"))));
                acc.setAccountName(cursor.getString(cursor.getColumnIndexOrThrow("account_name")));
                acc.setHandle(cursor.getString(cursor.getColumnIndexOrThrow("handle")));
                acc.setConnected(cursor.getInt(cursor.getColumnIndexOrThrow("is_connected")) == 1);
                acc.setFollowerCount(cursor.getInt(cursor.getColumnIndexOrThrow("follower_count")));
                acc.setAvatarInitial(cursor.getString(cursor.getColumnIndexOrThrow("avatar_initial")));
                acc.setStatusMessage(cursor.getString(cursor.getColumnIndexOrThrow("status_message")));
                list.add(acc);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    // --- Agent Logs Operations ---
    public long insertLog(AgentLog log) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("timestamp", log.getTimestamp());
        cv.put("step_name", log.getStepName());
        cv.put("agent_role", log.getAgentRole());
        cv.put("prompt_used", log.getPromptUsed());
        cv.put("output_snippet", log.getOutputSnippet());
        cv.put("tokens_used", log.getTokensUsed());
        cv.put("cost_estimate", log.getCostEstimate());
        cv.put("duration_ms", log.getDurationMs());
        return db.insert("agent_logs", null, cv);
    }

    public List<AgentLog> getAllLogs() {
        List<AgentLog> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM agent_logs ORDER BY timestamp DESC", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                AgentLog log = new AgentLog();
                log.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
                log.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")));
                log.setStepName(cursor.getString(cursor.getColumnIndexOrThrow("step_name")));
                log.setAgentRole(cursor.getString(cursor.getColumnIndexOrThrow("agent_role")));
                log.setPromptUsed(cursor.getString(cursor.getColumnIndexOrThrow("prompt_used")));
                log.setOutputSnippet(cursor.getString(cursor.getColumnIndexOrThrow("output_snippet")));
                log.setTokensUsed(cursor.getInt(cursor.getColumnIndexOrThrow("tokens_used")));
                log.setCostEstimate(cursor.getDouble(cursor.getColumnIndexOrThrow("cost_estimate")));
                log.setDurationMs(cursor.getLong(cursor.getColumnIndexOrThrow("duration_ms")));
                list.add(log);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public boolean isDatabaseEmpty() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM accounts", null);
        boolean empty = true;
        if (cursor != null && cursor.moveToFirst()) {
            empty = cursor.getInt(0) == 0;
            cursor.close();
        }
        return empty;
    }
}

