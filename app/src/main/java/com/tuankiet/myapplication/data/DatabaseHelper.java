package com.tuankiet.myapplication.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.tuankiet.myapplication.model.AgentLog;
import com.tuankiet.myapplication.model.AnalyticsData;
import com.tuankiet.myapplication.model.CommentItem;
import com.tuankiet.myapplication.model.Post;
import com.tuankiet.myapplication.model.SocialAccount;
import com.tuankiet.myapplication.model.SocialPlatform;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "social_ai.db";
    private static final int DATABASE_VERSION = 4;

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

    private static void createAnalyticsHistoryTable(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS analytics_history (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "timestamp INTEGER NOT NULL, " +
                        "platform TEXT NOT NULL, " +
                        "reach INTEGER DEFAULT 0, " +
                        "likes INTEGER DEFAULT 0, " +
                        "comments INTEGER DEFAULT 0, " +
                        "shares INTEGER DEFAULT 0, " +
                        "follower_count INTEGER DEFAULT 0" +
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
                "content_threads TEXT, " +
                "content_li TEXT, " +
                "status TEXT, " +
                "target_fb INTEGER, " +
                "target_threads INTEGER DEFAULT 0, " +
                "target_li INTEGER, " +
                "created_at INTEGER, " +
                "image_url TEXT, " +
                "image_prompt TEXT, " +
                "reach INTEGER, " +
                "likes INTEGER, " +
                "comments INTEGER, " +
                "shares INTEGER, " +
                "idempotency_key TEXT, " +
                "fb_post_url TEXT, " +
                "threads_post_url TEXT, " +
                "publish_error TEXT, " +
                "retry_count INTEGER DEFAULT 0, " +
                "content_threads TEXT, " +
                "target_threads INTEGER DEFAULT 0, " +
                "published_fb INTEGER DEFAULT 0, " +
                "published_threads INTEGER DEFAULT 0)");

        // Table Comments
        db.execSQL("CREATE TABLE comments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "platform TEXT, " +
                "post_title TEXT, " +
                "post_content TEXT, " +
                "author_name TEXT, " +
                "author_initial TEXT, " +
                "content TEXT, " +
                "time_ago TEXT, " +
                "external_comment_id TEXT, " +
                "category TEXT, " +
                "is_answered INTEGER, " +
                "reply_text TEXT, " +
                "ai_suggested_reply TEXT, " +
                "is_negative_flagged INTEGER, " +
                "risk_warning TEXT, " +
                "is_faq INTEGER DEFAULT 0, " +
                "has_personal_info INTEGER DEFAULT 0, " +
                "ai_confidence REAL DEFAULT 1.0, " +
                "auto_replied INTEGER DEFAULT 0, " +
                "auto_reply_blocked_reason TEXT)");

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
                "duration_ms INTEGER, " +
                "model_name TEXT)");

        createThreadsPublicationsTable(db);
        createAnalyticsHistoryTable(db);
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
        if (oldVersion < 3) {
            createAnalyticsHistoryTable(db);
            addColumnIfMissing(db, "posts", "content_threads", "TEXT");
            addColumnIfMissing(db, "posts", "target_threads", "INTEGER DEFAULT 0");
            addColumnIfMissing(db, "comments", "post_content", "TEXT");
            addColumnIfMissing(db, "comments", "external_comment_id", "TEXT");
            addColumnIfMissing(db, "comments", "is_faq", "INTEGER DEFAULT 0");
            addColumnIfMissing(db, "comments", "has_personal_info", "INTEGER DEFAULT 0");
            addColumnIfMissing(db, "comments", "ai_confidence", "REAL DEFAULT 1.0");
            addColumnIfMissing(db, "comments", "auto_replied", "INTEGER DEFAULT 0");
            addColumnIfMissing(db, "comments", "auto_reply_blocked_reason", "TEXT");
            // Migrate legacy seeded LinkedIn comments to Threads so Inbox reflects Facebook & Threads scope
            db.execSQL("UPDATE comments SET platform = 'THREADS' WHERE platform = 'LINKEDIN'");
        }
        if (oldVersion < 4) {
            // Ensure comments columns exist even if upgrading from v3 created by another branch
            addColumnIfMissing(db, "comments", "post_content", "TEXT");
            addColumnIfMissing(db, "comments", "external_comment_id", "TEXT");
            addColumnIfMissing(db, "comments", "is_faq", "INTEGER DEFAULT 0");
            addColumnIfMissing(db, "comments", "has_personal_info", "INTEGER DEFAULT 0");
            addColumnIfMissing(db, "comments", "ai_confidence", "REAL DEFAULT 1.0");
            addColumnIfMissing(db, "comments", "auto_replied", "INTEGER DEFAULT 0");
            addColumnIfMissing(db, "comments", "auto_reply_blocked_reason", "TEXT");
            db.execSQL("UPDATE comments SET platform = 'THREADS' WHERE platform = 'LINKEDIN'");

            // Add new columns to posts table for F5 Offline Queue & Idempotency
            addColumnIfMissing(db, "posts", "idempotency_key", "TEXT");
            addColumnIfMissing(db, "posts", "fb_post_url", "TEXT");
            addColumnIfMissing(db, "posts", "threads_post_url", "TEXT");
            addColumnIfMissing(db, "posts", "publish_error", "TEXT");
            addColumnIfMissing(db, "posts", "retry_count", "INTEGER DEFAULT 0");
            addColumnIfMissing(db, "posts", "content_threads", "TEXT");
            addColumnIfMissing(db, "posts", "target_threads", "INTEGER DEFAULT 0");
            addColumnIfMissing(db, "posts", "published_fb", "INTEGER DEFAULT 0");
            addColumnIfMissing(db, "posts", "published_threads", "INTEGER DEFAULT 0");
        }
    }

    private static void addColumnIfMissing(SQLiteDatabase db, String table, String columnName, String columnType) {
        boolean exists = false;
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            int nameIdx = cursor.getColumnIndex("name");
            while (cursor.moveToNext()) {
                if (nameIdx >= 0 && columnName.equalsIgnoreCase(cursor.getString(nameIdx))) {
                    exists = true;
                    break;
                }
            }
        }
        if (!exists) {
            db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + columnName + " " + columnType);
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
        cv.put("content_threads", post.getContentThreads());
        cv.put("status", post.getStatus());
        cv.put("target_fb", post.isTargetFacebook() ? 1 : 0);
        cv.put("target_li", post.isTargetLinkedIn() ? 1 : 0);
        cv.put("target_threads", post.isTargetThreads() ? 1 : 0);
        cv.put("created_at", post.getCreatedAt());
        cv.put("image_url", post.getImageUrl());
        cv.put("image_prompt", post.getImagePrompt());
        cv.put("reach", post.getReachCount());
        cv.put("likes", post.getLikeCount());
        cv.put("comments", post.getCommentCount());
        cv.put("shares", post.getShareCount());
        cv.put("idempotency_key", post.getIdempotencyKey());
        cv.put("fb_post_url", post.getFbPostUrl());
        cv.put("threads_post_url", post.getThreadsPostUrl());
        cv.put("publish_error", post.getPublishError());
        cv.put("retry_count", post.getRetryCount());
        cv.put("published_fb", post.isPublishedFacebook() ? 1 : 0);
        cv.put("published_threads", post.isPublishedThreads() ? 1 : 0);
        long id = db.insert("posts", null, cv);
        if (id > 0 && post.getId() == 0) {
            post.setId(id);
        }
        return id;
    }

    private Post cursorToPost(Cursor cursor) {
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

        int colContentThreads = cursor.getColumnIndex("content_threads");
        if (colContentThreads != -1 && !cursor.isNull(colContentThreads)) {
            post.setContentThreads(cursor.getString(colContentThreads));
        }

        int colTargetThreads = cursor.getColumnIndex("target_threads");
        if (colTargetThreads != -1 && !cursor.isNull(colTargetThreads)) {
            post.setTargetThreads(cursor.getInt(colTargetThreads) == 1);
        }

        int colPubFb = cursor.getColumnIndex("published_fb");
        if (colPubFb != -1 && !cursor.isNull(colPubFb)) {
            post.setPublishedFacebook(cursor.getInt(colPubFb) == 1);
        }

        int colPubThreads = cursor.getColumnIndex("published_threads");
        if (colPubThreads != -1 && !cursor.isNull(colPubThreads)) {
            post.setPublishedThreads(cursor.getInt(colPubThreads) == 1);
        }

        int colIdempotency = cursor.getColumnIndex("idempotency_key");
        if (colIdempotency != -1 && !cursor.isNull(colIdempotency)) {
            post.setIdempotencyKey(cursor.getString(colIdempotency));
        }

        int colFbUrl = cursor.getColumnIndex("fb_post_url");
        if (colFbUrl != -1 && !cursor.isNull(colFbUrl)) {
            post.setFbPostUrl(cursor.getString(colFbUrl));
        }

        int colThreadsUrl = cursor.getColumnIndex("threads_post_url");
        if (colThreadsUrl != -1 && !cursor.isNull(colThreadsUrl)) {
            post.setThreadsPostUrl(cursor.getString(colThreadsUrl));
        }

        int colError = cursor.getColumnIndex("publish_error");
        if (colError != -1 && !cursor.isNull(colError)) {
            post.setPublishError(cursor.getString(colError));
        }

        int colRetry = cursor.getColumnIndex("retry_count");
        if (colRetry != -1 && !cursor.isNull(colRetry)) {
            post.setRetryCount(cursor.getInt(colRetry));
        }

        return post;
    }

    public List<Post> getAllPosts() {
        List<Post> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM posts ORDER BY created_at DESC", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(cursorToPost(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public Post getPostById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM posts WHERE id = ?", new String[]{String.valueOf(id)});
        Post post = null;
        if (cursor != null && cursor.moveToFirst()) {
            post = cursorToPost(cursor);
            cursor.close();
        }
        return post;
    }

    public Post getPostByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) return null;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM posts WHERE idempotency_key = ?", new String[]{idempotencyKey.trim()});
        Post post = null;
        if (cursor != null && cursor.moveToFirst()) {
            post = cursorToPost(cursor);
            cursor.close();
        }
        return post;
    }

    public List<Post> getPendingPosts() {
        List<Post> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM posts WHERE status = ? ORDER BY created_at ASC", new String[]{Post.STATUS_PENDING});
        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(cursorToPost(cursor));
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
        cv.put("content_threads", post.getContentThreads());
        cv.put("status", post.getStatus());
        cv.put("target_fb", post.isTargetFacebook() ? 1 : 0);
        cv.put("target_threads", post.isTargetThreads() ? 1 : 0);
        cv.put("target_li", post.isTargetLinkedIn() ? 1 : 0);
        cv.put("image_url", post.getImageUrl());
        cv.put("image_prompt", post.getImagePrompt());
        cv.put("reach", post.getReachCount());
        cv.put("likes", post.getLikeCount());
        cv.put("comments", post.getCommentCount());
        cv.put("shares", post.getShareCount());
        cv.put("idempotency_key", post.getIdempotencyKey());
        cv.put("fb_post_url", post.getFbPostUrl());
        cv.put("threads_post_url", post.getThreadsPostUrl());
        cv.put("publish_error", post.getPublishError());
        cv.put("retry_count", post.getRetryCount());
        cv.put("published_fb", post.isPublishedFacebook() ? 1 : 0);
        cv.put("published_threads", post.isPublishedThreads() ? 1 : 0);
        db.update("posts", cv, "id = ?", new String[]{String.valueOf(post.getId())});
    }

    // --- Comments Operations ---
    public long insertComment(CommentItem item) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("platform", item.getPlatform() != null ? item.getPlatform().name() : SocialPlatform.FACEBOOK.name());
        cv.put("post_title", item.getPostTitle());
        cv.put("post_content", item.getPostContent());
        cv.put("author_name", item.getAuthorName());
        cv.put("author_initial", item.getAuthorInitial());
        cv.put("content", item.getContent());
        cv.put("time_ago", item.getTimeAgo());
        cv.put("external_comment_id", item.getExternalCommentId());
        cv.put("category", item.getCategory());
        cv.put("is_answered", item.isAnswered() ? 1 : 0);
        cv.put("reply_text", item.getReplyText());
        cv.put("ai_suggested_reply", item.getAiSuggestedReply());
        cv.put("is_negative_flagged", item.isNegativeFlagged() ? 1 : 0);
        cv.put("risk_warning", item.getRiskWarning());
        cv.put("is_faq", item.isFaq() ? 1 : 0);
        cv.put("has_personal_info", item.hasPersonalInfo() ? 1 : 0);
        cv.put("ai_confidence", item.getAiConfidence());
        cv.put("auto_replied", item.isAutoReplied() ? 1 : 0);
        cv.put("auto_reply_blocked_reason", item.getAutoReplyBlockedReason());
        long rowId = db.insert("comments", null, cv);
        if (rowId > 0) {
            item.setId(rowId);
        }
        return rowId;
    }

    public List<CommentItem> getAllComments() {
        List<CommentItem> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT * FROM comments ORDER BY id DESC", null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    list.add(readCommentFromCursor(cursor));
                } while (cursor.moveToNext());
            }
        }
        return list;
    }

    public CommentItem getCommentById(long commentId) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT * FROM comments WHERE id = ? LIMIT 1",
                new String[]{String.valueOf(commentId)})) {
            if (cursor != null && cursor.moveToFirst()) {
                return readCommentFromCursor(cursor);
            }
        }
        return null;
    }

    public boolean commentExistsByExternalId(String externalCommentId) {
        if (externalCommentId == null || externalCommentId.trim().isEmpty()) {
            return false;
        }
        SQLiteDatabase db = getReadableDatabase();
        String cleanId = externalCommentId.trim();
        try (Cursor cursor = db.rawQuery(
                "SELECT 1 FROM comments WHERE external_comment_id = ? OR time_ago LIKE ? LIMIT 1",
                new String[]{cleanId, "%ID:" + cleanId + "%"})) {
            return cursor != null && cursor.moveToFirst();
        }
    }

    public String findPostContentByTitle(String postTitle) {
        if (postTitle == null || postTitle.trim().isEmpty()) {
            return null;
        }
        String normalized = postTitle.trim()
                .replaceFirst("^\\[Threads\\]\\s*", "")
                .replaceFirst("^\\[Facebook\\]\\s*", "");
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT content_fb, content_li FROM posts WHERE topic = ? LIMIT 1",
                new String[]{normalized})) {
            if (cursor != null && cursor.moveToFirst()) {
                String fb = cursor.getString(0);
                if (fb != null && !fb.trim().isEmpty()) return fb;
                String li = cursor.getString(1);
                if (li != null && !li.trim().isEmpty()) return li;
            }
        }
        return null;
    }

    private CommentItem readCommentFromCursor(Cursor cursor) {
        CommentItem item = new CommentItem();
        item.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
        String platStr = cursor.getString(cursor.getColumnIndexOrThrow("platform"));
        try {
            item.setPlatform(SocialPlatform.valueOf(platStr));
        } catch (Exception e) {
            item.setPlatform(SocialPlatform.FACEBOOK);
        }
        item.setPostTitle(cursor.getString(cursor.getColumnIndexOrThrow("post_title")));
        int postContentIdx = cursor.getColumnIndex("post_content");
        if (postContentIdx >= 0) item.setPostContent(cursor.getString(postContentIdx));
        item.setAuthorName(cursor.getString(cursor.getColumnIndexOrThrow("author_name")));
        item.setAuthorInitial(cursor.getString(cursor.getColumnIndexOrThrow("author_initial")));
        item.setContent(cursor.getString(cursor.getColumnIndexOrThrow("content")));
        item.setTimeAgo(cursor.getString(cursor.getColumnIndexOrThrow("time_ago")));
        int extIdIdx = cursor.getColumnIndex("external_comment_id");
        if (extIdIdx >= 0) item.setExternalCommentId(cursor.getString(extIdIdx));
        item.setCategory(cursor.getString(cursor.getColumnIndexOrThrow("category")));
        item.setAnswered(cursor.getInt(cursor.getColumnIndexOrThrow("is_answered")) == 1);
        item.setReplyText(cursor.getString(cursor.getColumnIndexOrThrow("reply_text")));
        item.setAiSuggestedReply(cursor.getString(cursor.getColumnIndexOrThrow("ai_suggested_reply")));
        item.setNegativeFlagged(cursor.getInt(cursor.getColumnIndexOrThrow("is_negative_flagged")) == 1);
        item.setRiskWarning(cursor.getString(cursor.getColumnIndexOrThrow("risk_warning")));

        int faqIdx = cursor.getColumnIndex("is_faq");
        if (faqIdx >= 0) item.setFaq(cursor.getInt(faqIdx) == 1);
        int piiIdx = cursor.getColumnIndex("has_personal_info");
        if (piiIdx >= 0) item.setHasPersonalInfo(cursor.getInt(piiIdx) == 1);
        int confIdx = cursor.getColumnIndex("ai_confidence");
        if (confIdx >= 0 && !cursor.isNull(confIdx)) {
            item.setAiConfidence(cursor.getDouble(confIdx));
        } else {
            item.setAiConfidence(0.90);
        }
        int autoIdx = cursor.getColumnIndex("auto_replied");
        if (autoIdx >= 0) item.setAutoReplied(cursor.getInt(autoIdx) == 1);
        int blockedIdx = cursor.getColumnIndex("auto_reply_blocked_reason");
        if (blockedIdx >= 0) item.setAutoReplyBlockedReason(cursor.getString(blockedIdx));

        return item;
    }

    public void updateCommentReply(long commentId, String replyText) {
        updateCommentReply(commentId, replyText, false);
    }

    public void updateCommentReply(long commentId, String replyText, boolean autoReplied) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("is_answered", 1);
        cv.put("reply_text", replyText);
        cv.put("auto_replied", autoReplied ? 1 : 0);
        db.update("comments", cv, "id = ?", new String[]{String.valueOf(commentId)});
    }

    public void updateCommentAnalysis(CommentItem item) {
        if (item == null || item.getId() <= 0) return;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("category", item.getCategory());
        cv.put("ai_suggested_reply", item.getAiSuggestedReply());
        cv.put("is_negative_flagged", item.isNegativeFlagged() ? 1 : 0);
        cv.put("risk_warning", item.getRiskWarning());
        cv.put("is_faq", item.isFaq() ? 1 : 0);
        cv.put("has_personal_info", item.hasPersonalInfo() ? 1 : 0);
        cv.put("ai_confidence", item.getAiConfidence());
        cv.put("auto_reply_blocked_reason", item.getAutoReplyBlockedReason());
        db.update("comments", cv, "id = ?", new String[]{String.valueOf(item.getId())});
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
    private void ensureAgentLogModelColumn(SQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE agent_logs ADD COLUMN model_name TEXT");
        } catch (Exception ignored) {
        }
    }

    public long insertLog(AgentLog log) {
        SQLiteDatabase db = getWritableDatabase();
        ensureAgentLogModelColumn(db);
        ContentValues cv = new ContentValues();
        cv.put("timestamp", log.getTimestamp());
        cv.put("step_name", log.getStepName());
        cv.put("agent_role", log.getAgentRole());
        cv.put("prompt_used", log.getPromptUsed());
        cv.put("output_snippet", log.getOutputSnippet());
        cv.put("tokens_used", log.getTokensUsed());
        cv.put("cost_estimate", log.getCostEstimate());
        cv.put("duration_ms", log.getDurationMs());
        cv.put("model_name", log.getModelName() != null ? log.getModelName() : "gemini-3.1-flash-lite");
        return db.insert("agent_logs", null, cv);
    }

    public List<AgentLog> getAllLogs() {
        List<AgentLog> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        ensureAgentLogModelColumn(db);
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
                int modelIdx = cursor.getColumnIndex("model_name");
                if (modelIdx != -1 && !cursor.isNull(modelIdx)) {
                    log.setModelName(cursor.getString(modelIdx));
                } else {
                    log.setModelName("gemini-3.1-flash-lite");
                }
                list.add(log);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public void clearAllLogs() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("agent_logs", null, null);
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

    public long upsertDailyAnalyticsSnapshot(String platform, int reach, int likes, int comments, int shares, int followerCount) {
        String targetPlatform = platform != null ? platform : "ALL";
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        long startOfDay = cal.getTimeInMillis();

        ContentValues cv = new ContentValues();
        cv.put("timestamp", System.currentTimeMillis());
        cv.put("platform", targetPlatform);
        cv.put("reach", reach);
        cv.put("likes", likes);
        cv.put("comments", comments);
        cv.put("shares", shares);
        cv.put("follower_count", followerCount);

        SQLiteDatabase db = getWritableDatabase();
        long existingId = -1L;
        try (Cursor cursor = db.rawQuery(
                "SELECT id FROM analytics_history WHERE platform = ? AND timestamp >= ? ORDER BY timestamp DESC LIMIT 1",
                new String[]{targetPlatform, String.valueOf(startOfDay)})) {
            if (cursor.moveToFirst()) {
                existingId = cursor.getLong(0);
            }
        }
        if (existingId > 0) {
            db.update("analytics_history", cv, "id = ?", new String[]{String.valueOf(existingId)});
            return existingId;
        }
        return db.insert("analytics_history", null, cv);
    }

    public long insertAnalyticsSnapshot(String platform, int reach, int likes, int comments, int shares, int followerCount) {
        ContentValues cv = new ContentValues();
        cv.put("timestamp", System.currentTimeMillis());
        cv.put("platform", platform != null ? platform : "ALL");
        cv.put("reach", reach);
        cv.put("likes", likes);
        cv.put("comments", comments);
        cv.put("shares", shares);
        cv.put("follower_count", followerCount);
        SQLiteDatabase db = getWritableDatabase();
        return db.insert("analytics_history", null, cv);
    }

    public List<AnalyticsData.DayMetric> getAnalyticsHistory() {
        List<AnalyticsData.DayMetric> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT likes, comments FROM analytics_history ORDER BY timestamp DESC LIMIT 7", null);

        List<Integer> dbTotalsDesc = new ArrayList<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int likes = cursor.getInt(cursor.getColumnIndexOrThrow("likes"));
                int comments = cursor.getInt(cursor.getColumnIndexOrThrow("comments"));
                dbTotalsDesc.add(likes + comments);
            } while (cursor.moveToNext());
            cursor.close();
        }

        // Order chronological oldest -> newest ending at CN, scaling missing days smoothly around latest total
        String[] days = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
        double[] baselineScale = {0.42, 0.54, 0.68, 0.61, 0.78, 0.90, 1.00};
        int latestTotal = !dbTotalsDesc.isEmpty() ? Math.max(100, dbTotalsDesc.get(0)) : 500;
        int offset = 7 - dbTotalsDesc.size();

        for (int i = 0; i < 7; i++) {
            int dayTotal;
            if (i >= offset) {
                int descIdx = (dbTotalsDesc.size() - 1) - (i - offset);
                dayTotal = Math.max(80, dbTotalsDesc.get(descIdx));
            } else {
                dayTotal = Math.max(80, (int) (latestTotal * baselineScale[i]));
            }
            int fbEng = Math.max(45, (int) (dayTotal * 0.55));
            int thEng = Math.max(35, (int) (dayTotal * 0.45));
            list.add(new AnalyticsData.DayMetric(days[i], fbEng, thEng));
        }
        return list;
    }
}

