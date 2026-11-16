package com.tuankiet.myapplication.data;

import android.content.Context;
import android.database.Cursor;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ThreadsPublicationStorageTest {

    @Test
    public void testSaveThreadsPublication() {

        Context context = InstrumentationRegistry
                .getInstrumentation()
                .getTargetContext();

        DatabaseHelper dbHelper =
                DatabaseHelper.getInstance(context);

        // Tạo dữ liệu giả để kiểm thử SQLite, không gọi Threads API
        String fakePostId =
                "F5_LOCAL_TEST_" + System.currentTimeMillis();

        String fakePermalink =
                "https://example.invalid/threads/" + fakePostId;

        String fakeContent =
                "Đây là bài viết kiểm thử chức năng lưu SQLite của F5.";

        try {
            // Bước 1: Lưu dữ liệu giả vào SQLite
            long rowId = dbHelper.saveThreadsPublication(
                    fakePostId,
                    fakePermalink,
                    fakeContent
            );

            assertTrue("Lưu SQLite thất bại", rowId > 0);

            // Bước 2: Đọc bản ghi vừa lưu
            try (Cursor cursor =
                         dbHelper.getReadableDatabase().query(
                                 "threads_publications",
                                 new String[]{
                                         "threads_post_id",
                                         "permalink",
                                         "content",
                                         "published_at"
                                 },
                                 "threads_post_id = ?",
                                 new String[]{fakePostId},
                                 null,
                                 null,
                                 null
                         )) {

                assertEquals(1, cursor.getCount());
                assertTrue(cursor.moveToFirst());

                // Bước 3: Xác nhận dữ liệu được lưu chính xác
                assertEquals(fakePostId, cursor.getString(0));
                assertEquals(fakePermalink, cursor.getString(1));
                assertEquals(fakeContent, cursor.getString(2));

                assertTrue(
                        "Thời gian xuất bản không hợp lệ",
                        cursor.getLong(3) > 0
                );
            }

        } finally {
            // Bước 4: Xóa dữ liệu kiểm thử sau khi chạy
            dbHelper.getWritableDatabase().delete(
                    "threads_publications",
                    "threads_post_id = ?",
                    new String[]{fakePostId}
            );
        }
    }
}
