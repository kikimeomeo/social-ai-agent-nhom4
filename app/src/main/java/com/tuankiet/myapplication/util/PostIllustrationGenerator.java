package com.tuankiet.myapplication.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.widget.ImageView;

import com.tuankiet.myapplication.R;
import com.tuankiet.myapplication.model.SocialPlatform;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Saves AI-generated illustration images, renders rich channel-sized on-device fallback
 * templates when image APIs are unavailable/in Demo Mode, and asynchronously loads
 * local or remote post images into ImageViews.
 */
public final class PostIllustrationGenerator {

    public static class GeneratedIllustration {
        public final String imageUri;
        public final String imagePrompt;
        public final String sourceLabel;
        public final boolean generatedByRealImageApi;
        public final int variantIndex;

        public GeneratedIllustration(String imageUri,
                                     String imagePrompt,
                                     String sourceLabel,
                                     boolean generatedByRealImageApi,
                                     int variantIndex) {
            this.imageUri = imageUri;
            this.imagePrompt = imagePrompt;
            this.sourceLabel = sourceLabel;
            this.generatedByRealImageApi = generatedByRealImageApi;
            this.variantIndex = variantIndex;
        }
    }

    private static final int[][] PALETTES = {
            // 0: Ocean Cyan Glass
            {Color.parseColor("#0369A1"), Color.parseColor("#0284C7"), Color.parseColor("#22D3EE"), Color.parseColor("#E0F2FE")},
            // 1: Deep Indigo Neural
            {Color.parseColor("#1E1B4B"), Color.parseColor("#4338CA"), Color.parseColor("#06B6D4"), Color.parseColor("#E0E7FF")},
            // 2: Emerald Tech Editorial
            {Color.parseColor("#064E3B"), Color.parseColor("#0D9488"), Color.parseColor("#34D399"), Color.parseColor("#CCFBF1")},
            // 3: Warm Sunset Studio
            {Color.parseColor("#7C2D12"), Color.parseColor("#EA580C"), Color.parseColor("#F59E0B"), Color.parseColor("#FFEDD5")},
            // 4: Midnight Cyber Slate
            {Color.parseColor("#0F172A"), Color.parseColor("#1E293B"), Color.parseColor("#38BDF8"), Color.parseColor("#E2E8F0")},
            // 5: Royal Violet Prism
            {Color.parseColor("#3B0764"), Color.parseColor("#7C3AED"), Color.parseColor("#EC4899"), Color.parseColor("#F3E8FF")}
    };

    private static final String[] STYLE_NAMES = {
            "Ocean Glass",
            "Indigo Neural",
            "Emerald Editorial",
            "Warm Studio",
            "Cyber Blueprint",
            "Violet Prism"
    };

    private static final ExecutorService IMAGE_LOADER_EXECUTOR = Executors.newFixedThreadPool(2);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final OkHttpClient IMAGE_HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    private PostIllustrationGenerator() {
    }

    /**
     * Validates raw image bytes returned from an AI Image API and persists them locally as PNG.
     * Returns a file:// URI string, or null if the bytes are not a valid decodable image.
     */
    public static String saveImageBytes(Context context, byte[] imageBytes, String prefix) {
        if (context == null || imageBytes == null || imageBytes.length < 128) {
            return null;
        }
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        if (bitmap == null) {
            return null;
        }
        try {
            return saveBitmapToInternalStorage(context, bitmap, prefix);
        } finally {
            bitmap.recycle();
        }
    }

    /**
     * Renders a studio-quality, channel-proportioned illustration directly on-device
     * (16:9 landscape for Facebook Page, 1:1 square for Threads) with a distinct visual
     * theme per variantIndex, then saves it to app internal storage.
     */
    public static GeneratedIllustration renderTemplateIllustration(Context context,
                                                                   String topic,
                                                                   String postContent,
                                                                   String imagePrompt,
                                                                   SocialPlatform platform,
                                                                   int variantIndex) {
        SocialPlatform safePlatform = platform != null ? platform : SocialPlatform.FACEBOOK;
        boolean isThreads = (safePlatform == SocialPlatform.THREADS);

        int width = isThreads ? 900 : 1200;
        int height = isThreads ? 900 : 675;
        int paletteIdx = Math.abs(variantIndex) % PALETTES.length;
        int[] palette = PALETTES[paletteIdx];
        String styleName = STYLE_NAMES[paletteIdx];

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // 1. Rich diagonal gradient background
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setShader(new LinearGradient(
                0, 0, width, height,
                new int[]{palette[0], palette[1], palette[2]},
                new float[]{0f, 0.58f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(0, 0, width, height, bgPaint);

        // 2. Distinct geometric art composition per variant
        drawGeometricArtwork(canvas, width, height, palette, paletteIdx);

        // 3. Glassmorphic content card overlay
        float marginX = width * 0.07f;
        float marginY = height * 0.10f;
        RectF cardRect = new RectF(marginX, marginY, width - marginX, height - marginY);
        Paint glassFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        glassFill.setColor(Color.argb(46, 255, 255, 255));
        canvas.drawRoundRect(cardRect, 42f, 42f, glassFill);

        Paint glassBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        glassBorder.setStyle(Paint.Style.STROKE);
        glassBorder.setStrokeWidth(3f);
        glassBorder.setColor(Color.argb(115, 255, 255, 255));
        canvas.drawRoundRect(cardRect, 42f, 42f, glassBorder);

        // 4. Channel badge pill at top-left inside card
        String badgeText = isThreads
                ? "THREADS  •  1:1  •  " + styleName.toUpperCase(Locale.ROOT)
                : "FACEBOOK PAGE  •  16:9  •  " + styleName.toUpperCase(Locale.ROOT);
        Paint badgeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgeTextPaint.setColor(Color.WHITE);
        badgeTextPaint.setTextSize(isThreads ? 24f : 22f);
        badgeTextPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));

        float badgePadX = 28f;
        float badgeTextW = badgeTextPaint.measureText(badgeText);
        float badgeLeft = cardRect.left + 44f;
        float badgeTop = cardRect.top + 44f;
        RectF badgeRect = new RectF(badgeLeft, badgeTop, badgeLeft + badgeTextW + badgePadX * 2f, badgeTop + 56f);

        Paint badgeBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgeBg.setColor(Color.argb(85, 15, 23, 42));
        canvas.drawRoundRect(badgeRect, 28f, 28f, badgeBg);
        canvas.drawText(badgeText, badgeLeft + badgePadX, badgeTop + 37f, badgeTextPaint);

        // 5. Main headline (Topic or first line of post)
        String headline = resolveHeadline(topic, postContent);
        TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextSize(isThreads ? 54f : 52f);
        titlePaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        titlePaint.setShadowLayer(12f, 0f, 4f, Color.argb(90, 0, 0, 0));

        int textWidth = (int) (cardRect.width() - 88f);
        StaticLayout titleLayout = StaticLayout.Builder
                .obtain(headline, 0, headline.length(), titlePaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(10f, 1.0f)
                .setMaxLines(isThreads ? 4 : 3)
                .build();

        canvas.save();
        float titleTop = badgeRect.bottom + (isThreads ? 54f : 38f);
        canvas.translate(cardRect.left + 44f, titleTop);
        titleLayout.draw(canvas);
        canvas.restore();

        // 6. Visual concept subtitle (excerpt of prompt or post summary)
        String subtitle = resolveSubtitle(imagePrompt, postContent);
        TextPaint subPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        subPaint.setColor(palette[3]);
        subPaint.setTextSize(isThreads ? 26f : 24f);
        subPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));

        StaticLayout subLayout = StaticLayout.Builder
                .obtain(subtitle, 0, subtitle.length(), subPaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(6f, 1.0f)
                .setMaxLines(3)
                .build();

        canvas.save();
        float subTop = Math.min(titleTop + titleLayout.getHeight() + 28f, cardRect.bottom - 180f);
        canvas.translate(cardRect.left + 44f, subTop);
        subLayout.draw(canvas);
        canvas.restore();

        // 7. Bottom accent bar & SocialAI Visual Agent signature
        Paint accentLine = new Paint(Paint.ANTI_ALIAS_FLAG);
        accentLine.setColor(Color.argb(140, 255, 255, 255));
        accentLine.setStrokeWidth(3f);
        float footerY = cardRect.bottom - 68f;
        canvas.drawLine(cardRect.left + 44f, footerY - 26f, cardRect.right - 44f, footerY - 26f, accentLine);

        Paint footerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        footerPaint.setColor(Color.argb(230, 255, 255, 255));
        footerPaint.setTextSize(22f);
        footerPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        canvas.drawText("SocialAI Visual Agent  •  Concept #" + (Math.abs(variantIndex) + 1), cardRect.left + 44f, footerY + 12f, footerPaint);

        String savedUri;
        try {
            savedUri = saveBitmapToInternalStorage(context, bitmap, isThreads ? "threads_tpl_" : "fb_tpl_");
        } finally {
            bitmap.recycle();
        }

        String ratioTag = isThreads ? "1:1" : "16:9";
        String sourceLabel = "Visual Template • " + styleName + " (" + ratioTag + ")";
        return new GeneratedIllustration(savedUri, imagePrompt, sourceLabel, false, variantIndex);
    }

    private static void drawGeometricArtwork(Canvas canvas, int width, int height, int[] palette, int variant) {
        Paint orbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        orbPaint.setStyle(Paint.Style.FILL);

        Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(4f);
        ringPaint.setColor(Color.argb(55, 255, 255, 255));

        switch (variant % 6) {
            case 0:
                // Luminous overlapping spheres
                orbPaint.setColor(Color.argb(65, 255, 255, 255));
                canvas.drawCircle(width * 0.85f, height * 0.22f, height * 0.34f, orbPaint);
                orbPaint.setColor(Color.argb(50, Color.red(palette[3]), Color.green(palette[3]), Color.blue(palette[3])));
                canvas.drawCircle(width * 0.14f, height * 0.86f, height * 0.28f, orbPaint);
                canvas.drawCircle(width * 0.85f, height * 0.22f, height * 0.44f, ringPaint);
                break;
            case 1:
                // Neural network mesh nodes
                float[][] pts = {
                        {width * 0.12f, height * 0.18f},
                        {width * 0.35f, height * 0.10f},
                        {width * 0.78f, height * 0.16f},
                        {width * 0.90f, height * 0.55f},
                        {width * 0.72f, height * 0.88f},
                        {width * 0.20f, height * 0.84f}
                };
                for (int i = 0; i < pts.length; i++) {
                    float[] a = pts[i];
                    float[] b = pts[(i + 1) % pts.length];
                    canvas.drawLine(a[0], a[1], b[0], b[1], ringPaint);
                    orbPaint.setColor(Color.argb(95, 255, 255, 255));
                    canvas.drawCircle(a[0], a[1], 14f, orbPaint);
                }
                orbPaint.setColor(Color.argb(55, 34, 211, 238));
                canvas.drawCircle(width * 0.84f, height * 0.78f, height * 0.30f, orbPaint);
                break;
            case 2:
                // Architectural diagonal stripes & crisp grid
                for (int i = -2; i < 10; i++) {
                    float x = i * (width / 7f);
                    canvas.drawLine(x, 0, x + height * 0.55f, height, ringPaint);
                }
                orbPaint.setColor(Color.argb(55, 255, 255, 255));
                canvas.drawCircle(width * 0.88f, height * 0.20f, height * 0.24f, orbPaint);
                break;
            case 3:
                // Concentric studio waves
                for (int r = 1; r <= 4; r++) {
                    canvas.drawCircle(width * 0.88f, height * 0.82f, height * 0.16f * r, ringPaint);
                }
                orbPaint.setColor(Color.argb(60, 255, 255, 255));
                canvas.drawCircle(width * 0.16f, height * 0.20f, height * 0.25f, orbPaint);
                break;
            case 4:
                // Blueprint dot matrix
                orbPaint.setColor(Color.argb(55, 255, 255, 255));
                int step = 72;
                for (int x = 36; x < width; x += step) {
                    for (int y = 36; y < height; y += step) {
                        canvas.drawCircle(x, y, 4f, orbPaint);
                    }
                }
                orbPaint.setColor(Color.argb(65, 56, 189, 248));
                canvas.drawCircle(width * 0.82f, height * 0.25f, height * 0.28f, orbPaint);
                break;
            default:
                // Prism layered rounded rectangles
                orbPaint.setColor(Color.argb(45, 255, 255, 255));
                canvas.save();
                canvas.rotate(-18f, width * 0.75f, height * 0.30f);
                canvas.drawRoundRect(new RectF(width * 0.55f, -60f, width * 1.05f, height * 0.60f), 48f, 48f, orbPaint);
                canvas.restore();
                canvas.save();
                canvas.rotate(14f, width * 0.20f, height * 0.82f);
                canvas.drawRoundRect(new RectF(-40f, height * 0.58f, width * 0.42f, height * 1.12f), 48f, 48f, orbPaint);
                canvas.restore();
                break;
        }
    }

    private static String resolveHeadline(String topic, String postContent) {
        if (topic != null && !topic.trim().isEmpty()) {
            String t = topic.trim();
            return t.length() > 82 ? t.substring(0, 79).trim() + "..." : t;
        }
        if (postContent != null && !postContent.trim().isEmpty()) {
            String firstLine = postContent.trim().split("\n")[0].trim();
            return firstLine.length() > 82 ? firstLine.substring(0, 79).trim() + "..." : firstLine;
        }
        return "SocialAI Content Studio";
    }

    private static String resolveSubtitle(String imagePrompt, String postContent) {
        if (imagePrompt != null && !imagePrompt.trim().isEmpty()) {
            String p = imagePrompt.trim();
            return p.length() > 140 ? p.substring(0, 137).trim() + "..." : p;
        }
        if (postContent != null && !postContent.trim().isEmpty()) {
            String clean = postContent.replace("\n", " ").replaceAll("\\s{2,}", " ").trim();
            return clean.length() > 140 ? clean.substring(0, 137).trim() + "..." : clean;
        }
        return "Human-in-the-loop visual illustration optimized for social engagement.";
    }

    private static String saveBitmapToInternalStorage(Context context, Bitmap bitmap, String prefix) {
        File dir = new File(context.getApplicationContext().getFilesDir(), "generated_images");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        File outFile = new File(dir, prefix + System.currentTimeMillis() + ".png");
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 92, fos);
            fos.flush();
            return "file://" + outFile.getAbsolutePath();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Loads a local file:// URI, filesystem path, or remote http(s) URL asynchronously into an ImageView.
     */
    public static void loadIntoImageView(Context context,
                                         String imageUri,
                                         ImageView imageView,
                                         Runnable onFinish) {
        if (imageView == null) {
            if (onFinish != null) MAIN_HANDLER.post(onFinish);
            return;
        }

        if (imageUri == null || imageUri.trim().isEmpty()) {
            imageView.setTag(null);
            imageView.setImageResource(R.drawable.bg_image_placeholder);
            if (onFinish != null) MAIN_HANDLER.post(onFinish);
            return;
        }

        final String targetUri = imageUri.trim();
        imageView.setTag(targetUri);

        IMAGE_LOADER_EXECUTOR.execute(() -> {
            Bitmap loaded = null;
            try {
                if (targetUri.startsWith("file://")) {
                    String path = targetUri.substring("file://".length());
                    loaded = decodeSampledFile(path, 1200, 900);
                } else if (targetUri.startsWith("/")) {
                    loaded = decodeSampledFile(targetUri, 1200, 900);
                } else if (targetUri.startsWith("http://") || targetUri.startsWith("https://")) {
                    Request req = new Request.Builder().url(targetUri).get().build();
                    try (Response resp = IMAGE_HTTP_CLIENT.newCall(req).execute()) {
                        if (resp.isSuccessful() && resp.body() != null) {
                            byte[] bytes = resp.body().bytes();
                            loaded = decodeSampledBytes(bytes, 1200, 900);
                        }
                    }
                }
            } catch (Exception ignored) {
                loaded = null;
            }

            final Bitmap finalBitmap = loaded;
            MAIN_HANDLER.post(() -> {
                if (targetUri.equals(imageView.getTag())) {
                    if (finalBitmap != null) {
                        imageView.setImageBitmap(finalBitmap);
                    } else {
                        imageView.setImageResource(R.drawable.bg_image_placeholder);
                    }
                }
                if (onFinish != null) {
                    onFinish.run();
                }
            });
        });
    }

    private static Bitmap decodeSampledFile(String path, int reqWidth, int reqHeight) {
        File file = new File(path);
        if (!file.exists()) return null;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, opts);
        opts.inSampleSize = calculateInSampleSize(opts, reqWidth, reqHeight);
        opts.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, opts);
    }

    private static Bitmap decodeSampledBytes(byte[] data, int reqWidth, int reqHeight) {
        if (data == null || data.length == 0) return null;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, opts);
        opts.inSampleSize = calculateInSampleSize(opts, reqWidth, reqHeight);
        opts.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(data, 0, data.length, opts);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return Math.max(1, inSampleSize);
    }
}
