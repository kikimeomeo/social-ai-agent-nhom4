package com.tuankiet.myapplication.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Utility tạo ảnh graphic card offline chuẩn kích thước bằng Android Canvas.
 * Đáp ứng yêu cầu Fallback Template khi mất mạng hoặc lỗi API sinh ảnh (Epic F2).
 */
public class FallbackImageTemplateGenerator {

    // 3 bộ màu chuyển sắc gradient cao cấp (dùng hằng số int thuần túy)
    public static final int[][] COLOR_PALETTES = {
            {0xFF0284C7, 0xFF22D3EE}, // 0: Ngọc Biển & Cyan (Brand Theme)
            {0xFF0F172A, 0xFF334155}, // 1: Dark Slate & Steel (Chuyên gia/Công nghệ)
            {0xFFC2410C, 0xFFF59E0B}  // 2: Sunset Coral & Amber (Sáng tạo/Viral)
    };

    /**
     * Dựng ảnh Graphic Card offline và lưu vào bộ nhớ cache của thiết bị.
     *
     * @param context    Application Context
     * @param topic      Chủ đề bài viết người dùng đã nhập
     * @param platform   Tên kênh ("Facebook" hoặc "Threads")
     * @param styleIndex Chỉ số phong cách màu (0, 1, 2)
     * @return Đường dẫn file URI cục bộ dạng "file:///..."
     */
    public static String generateImageCard(Context context, String topic, String platform, int styleIndex) {
        if (context == null) return "";
        String safeTopic = (topic != null && !topic.trim().isEmpty()) ? topic.trim() : "SocialAI Content Highlights";
        String safePlatform = (platform != null && !platform.trim().isEmpty()) ? platform.trim() : "SocialAI";

        int width = 1200;
        int height = 675; // Tỉ lệ 16:9 chuẩn đồ họa mạng xã hội, khớp tuyệt đối khung hiển thị điện thoại

        try {
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            // 1. Vẽ nền Gradient mượt mà
            int paletteIdx = Math.abs(styleIndex) % COLOR_PALETTES.length;
            int[] colors = COLOR_PALETTES[paletteIdx];
            Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bgPaint.setShader(new LinearGradient(0, 0, width, height, colors[0], colors[1], Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, width, height, bgPaint);

            // 2. Vẽ viền bo tròn Glassmorphism
            Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(3f);
            borderPaint.setColor(Color.WHITE);
            borderPaint.setAlpha(45);
            RectF cardRect = new RectF(45, 45, width - 45, height - 45);
            canvas.drawRoundRect(cardRect, 36, 36, borderPaint);

            // 3. Vẽ Huy hiệu nền tảng (Platform Badge) ở góc trên với khoảng đệm an toàn
            Paint badgeBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            badgeBgPaint.setColor(Color.WHITE);
            badgeBgPaint.setAlpha(40);
            RectF badgeRect = new RectF(85, 80, 430, 142);
            canvas.drawRoundRect(badgeRect, 28, 28, badgeBgPaint);

            Paint badgeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            badgeTextPaint.setColor(Color.WHITE);
            badgeTextPaint.setTextSize(26);
            badgeTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText("✦ " + safePlatform.toUpperCase() + " AI CARD", 110, 121, badgeTextPaint);

            // 4. Vẽ Dấu ngoặc kép biểu tượng (Quote Mark)
            Paint quotePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            quotePaint.setColor(Color.WHITE);
            quotePaint.setAlpha(65);
            quotePaint.setTextSize(100);
            quotePaint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
            canvas.drawText("“", 85, 235, quotePaint);

            // 5. Vẽ Tiêu đề bài viết (Topic) tự động ngắt dòng căn giữa
            TextPaint topicPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            topicPaint.setColor(Color.WHITE);
            topicPaint.setTextSize(46);
            topicPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            topicPaint.setShadowLayer(6f, 0f, 3f, Color.parseColor("#40000000"));

            int textWidth = width - 180;
            StaticLayout staticLayout;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                staticLayout = StaticLayout.Builder.obtain(safeTopic, 0, safeTopic.length(), topicPaint, textWidth)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(10f, 1.15f)
                        .setMaxLines(4)
                        .build();
            } else {
                staticLayout = new StaticLayout(safeTopic, topicPaint, textWidth,
                        Layout.Alignment.ALIGN_NORMAL, 1.15f, 10f, false);
            }

            canvas.save();
            int textY = 228;
            canvas.translate(90, textY);
            staticLayout.draw(canvas);
            canvas.restore();

            // 6. Đường phân cách mảnh ở dưới
            Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dividerPaint.setColor(Color.WHITE);
            dividerPaint.setAlpha(35);
            dividerPaint.setStrokeWidth(2f);
            canvas.drawLine(85, height - 95, width - 85, height - 95, dividerPaint);

            // 7. Watermark & Branding
            Paint footerBrand = new Paint(Paint.ANTI_ALIAS_FLAG);
            footerBrand.setColor(Color.WHITE);
            footerBrand.setTextSize(26);
            footerBrand.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText("SocialAI Studio", 85, height - 52, footerBrand);

            Paint footerSub = new Paint(Paint.ANTI_ALIAS_FLAG);
            footerSub.setColor(Color.WHITE);
            footerSub.setAlpha(170);
            footerSub.setTextSize(22);
            canvas.drawText("• Human-in-the-loop Verified", 300, height - 52, footerSub);

            // 8. Lưu file ra cache cục bộ thiết bị
            File cacheDir = new File(context.getCacheDir(), "ai_templates");
            if (!cacheDir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                cacheDir.mkdirs();
            }
            File imageFile = new File(cacheDir, "template_" + safePlatform.toLowerCase() + "_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos);
            fos.flush();
            fos.close();

            return Uri.fromFile(imageFile).toString();
        } catch (Exception e) {
            return "";
        }
    }
}
