package com.tuankiet.myapplication.util;

import android.content.Context;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Dịch vụ Visual Agent: Chuyển chủ đề bài viết thành mô tả cảnh vật (không vẽ mặt người ngẫu nhiên)
 * và từ khóa tìm kiếm chính xác, ưu tiên sinh ảnh AI / tìm ảnh thật đúng chủ đề,
 * và tự động kích hoạt Fallback Template offline nếu mất mạng hoặc lỗi API.
 */
public class VisualImageService {

    public static class VisualSpec {
        public final String aiPrompt;
        public final String searchKeywords;

        public VisualSpec(String aiPrompt, String searchKeywords) {
            this.aiPrompt = aiPrompt;
            this.searchKeywords = searchKeywords;
        }
    }

    public static class ImageResult {
        public final String localUri;
        public final String promptUsed;
        public final boolean isFallback;

        public ImageResult(String localUri, String promptUsed, boolean isFallback) {
            this.localUri = localUri;
            this.promptUsed = promptUsed;
            this.isFallback = isFallback;
        }
    }

    /**
     * Tạo hoặc tìm ảnh minh họa đúng chủ đề bài viết, lưu về cache cục bộ.
     */
    public static ImageResult generateOrFetchImage(
            Context context,
            OkHttpClient baseClient,
            VisualSpec customSpec,
            String topic,
            String platform,
            int variantIndex) {

        if (context == null) {
            return new ImageResult("", "", true);
        }

        VisualSpec spec = (customSpec != null && customSpec.aiPrompt != null && !customSpec.aiPrompt.trim().isEmpty())
                ? customSpec
                : buildLocalVisualSpec(topic);

        OkHttpClient fastClient = (baseClient != null ? baseClient : new OkHttpClient())
                .newBuilder()
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .build();

        // Khi người dùng bấm "Đổi ảnh khác" ở các lượt lẻ (1, 3, 5...), ưu tiên ảnh chụp thật từ Wikimedia trước để đa dạng phong cách
        boolean preferRealPhotoFirst = (variantIndex % 2 == 1);

        if (preferRealPhotoFirst) {
            String realPhotoUri = tryFetchWikimediaPhoto(context, fastClient, spec.searchKeywords, variantIndex);
            if (!realPhotoUri.isEmpty()) {
                return new ImageResult(realPhotoUri, "Ảnh thực tế: " + spec.searchKeywords, false);
            }
            String aiImageUri = tryFetchPollinationsImage(context, fastClient, spec.aiPrompt, variantIndex);
            if (!aiImageUri.isEmpty()) {
                return new ImageResult(aiImageUri, "AI Visual: " + spec.aiPrompt, false);
            }
        } else {
            String aiImageUri = tryFetchPollinationsImage(context, fastClient, spec.aiPrompt, variantIndex);
            if (!aiImageUri.isEmpty()) {
                return new ImageResult(aiImageUri, "AI Visual: " + spec.aiPrompt, false);
            }
            String realPhotoUri = tryFetchWikimediaPhoto(context, fastClient, spec.searchKeywords, variantIndex);
            if (!realPhotoUri.isEmpty()) {
                return new ImageResult(realPhotoUri, "Ảnh thực tế: " + spec.searchKeywords, false);
            }
        }

        // Tier 3: Fallback Template dựng graphic card offline khi mất mạng hoặc API lỗi
        String fallbackUri = FallbackImageTemplateGenerator.generateImageCard(
                context, topic, platform, variantIndex);
        return new ImageResult(fallbackUri, "Fallback Graphic Card: " + topic, true);
    }

    /**
     * Xây dựng mô tả cảnh vật bám sát ngữ cảnh bài viết & từ khóa tìm ảnh chuẩn xác.
     */
    public static VisualSpec buildLocalVisualSpec(String topic) {
        if (topic == null || topic.trim().isEmpty()) {
            return new VisualSpec(
                    "minimalist modern workspace with laptop and glowing circuit hologram on clean desk, editorial tech photography, no text, no watermark",
                    "laptop workspace technology"
            );
        }

        String lower = topic.toLowerCase(Locale.ROOT);

        if (lower.contains("android") || lower.contains("mobile") || lower.contains("điện thoại") || lower.contains("smartphone") || lower.contains("on-device")) {
            return new VisualSpec(
                    "close-up of a sleek modern smartphone on a developer desk displaying glowing AI neural circuit interface, soft cyan studio lighting, editorial tech photography, no text, no watermark",
                    "smartphone mobile technology"
            );
        }

        if (lower.contains("trí tuệ nhân tạo") || lower.contains(" ai ") || lower.startsWith("ai ") || lower.endsWith(" ai") || lower.contains("agent") || lower.contains("automation") || lower.contains("tự động")) {
            return new VisualSpec(
                    "futuristic 3d isometric glowing silicon microchip processor and fiber optic data lines on sleek glass desk, cyan and blue lighting, editorial tech render, no text, no watermark",
                    "microchip circuit board computer"
            );
        }

        if (lower.contains("lập trình") || lower.contains("code") || lower.contains("developer") || lower.contains("phần mềm")) {
            return new VisualSpec(
                    "software developer working at modern desk with glowing laptop screen showing code, mechanical keyboard, warm desk lamp, shallow depth of field photography, no text, no watermark",
                    "software developer coding"
            );
        }

        if (lower.contains("mạng xã hội") || lower.contains("facebook") || lower.contains("threads") || lower.contains("social") || lower.contains("content")) {
            return new VisualSpec(
                    "content creator using modern smartphone and laptop on bright wooden desk with coffee cup, clean studio photography, no text, no watermark",
                    "smartphone social media workspace"
            );
        }

        if (lower.contains("marketing") || lower.contains("kinh doanh") || lower.contains("bán hàng") || lower.contains("startup") || lower.contains("thương hiệu")) {
            return new VisualSpec(
                    "modern office desk with laptop, analytics growth chart on tablet, coffee cup, natural sunlight, editorial business photography, no text, no watermark",
                    "office laptop business"
            );
        }

        if (lower.contains("bảo mật") || lower.contains("security") || lower.contains("an ninh")) {
            return new VisualSpec(
                    "3d glowing blue digital padlock and encrypted network shield over fiber optic server rack, clean tech render, no text, no watermark",
                    "server rack network"
            );
        }

        if (lower.contains("tài chính") || lower.contains("đầu tư") || lower.contains("chứng khoán")) {
            return new VisualSpec(
                    "sleek glass tablet displaying financial candlestick charts on clean marble desk, modern lighting, editorial photography, no text, no watermark",
                    "stock market chart"
            );
        }

        if (lower.contains("học") || lower.contains("giáo dục") || lower.contains("sinh viên") || lower.contains("kỹ năng")) {
            return new VisualSpec(
                    "student studying with open notebook, pen, laptop and warm coffee cup on wooden desk, morning sunlight, editorial photography, no text, no watermark",
                    "notebook laptop study"
            );
        }

        String keywords = extractAsciiKeywords(topic);
        String prompt = "editorial cover photo directly matching the context of " + keywords
                + ", natural environment and lighting, high resolution photography, no text, no watermark";
        return new VisualSpec(prompt, keywords);
    }

    /**
     * Giữ lại hàm tiện ích cho Unit Test & trích xuất từ khóa tìm kiếm.
     */
    public static String buildSearchKeywords(String topic) {
        return buildLocalVisualSpec(topic).searchKeywords;
    }

    private static String extractAsciiKeywords(String topic) {
        String asciiOnly = topic.replaceAll("[^a-zA-Z0-9 ]", " ").replaceAll("\\s+", " ").trim();
        if (asciiOnly.length() >= 4) {
            String[] words = asciiOnly.split(" ");
            StringBuilder sb = new StringBuilder();
            int added = 0;
            for (String w : words) {
                if (w.length() >= 3) {
                    if (added > 0) sb.append(" ");
                    sb.append(w);
                    added++;
                    if (added >= 3) break;
                }
            }
            if (added > 0) return sb.toString();
        }
        return "modern technology innovation";
    }

    private static String tryFetchPollinationsImage(Context context, OkHttpClient client, String prompt, int variantIndex) {
        try {
            String encodedPrompt = URLEncoder.encode(prompt, StandardCharsets.UTF_8.name());
            int seed = 2048 + (Math.abs(variantIndex) * 193);
            String url = "https://image.pollinations.ai/prompt/" + encodedPrompt
                    + "?width=1200&height=675&nologo=true&seed=" + seed;

            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "SocialAI-Android/1.0")
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) return "";
                ResponseBody body = response.body();
                if (body == null) return "";

                String contentType = response.header("Content-Type", "");
                if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
                    return "";
                }

                byte[] bytes = body.bytes();
                if (bytes.length < 5000) return "";

                return saveBytesToCache(context, bytes, "ai_gen");
            }
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String tryFetchWikimediaPhoto(Context context, OkHttpClient client, String keywords, int variantIndex) {
        try {
            String encodedSearch = URLEncoder.encode("filetype:bitmap " + keywords, StandardCharsets.UTF_8.name());
            String apiUrl = "https://commons.wikimedia.org/w/api.php?action=query&generator=search"
                    + "&gsrnamespace=6&gsrsearch=" + encodedSearch
                    + "&gsrlimit=10&prop=imageinfo&iiprop=url|mime|size&iiurlwidth=1200&format=json";

            Request apiRequest = new Request.Builder()
                    .url(apiUrl)
                    .header("User-Agent", "SocialAI-Android/1.0")
                    .get()
                    .build();

            List<String> candidateUrls = new ArrayList<>();
            List<String> fallbackUrls = new ArrayList<>();

            try (Response apiResponse = client.newCall(apiRequest).execute()) {
                if (!apiResponse.isSuccessful() || apiResponse.body() == null) return "";
                String jsonStr = apiResponse.body().string();
                JSONObject root = new JSONObject(jsonStr);
                JSONObject query = root.optJSONObject("query");
                if (query == null) return "";
                JSONObject pages = query.optJSONObject("pages");
                if (pages == null) return "";

                Iterator<String> keys = pages.keys();
                while (keys.hasNext()) {
                    JSONObject page = pages.optJSONObject(keys.next());
                    if (page == null) continue;
                    String title = page.optString("title", "").toLowerCase(Locale.ROOT);
                    if (title.contains("screenshot") || title.contains("crash") || title.contains("error")
                            || title.contains("flag_of_") || title.contains("word_cloud") || title.contains("diagram")) {
                        continue;
                    }

                    JSONArray imageInfo = page.optJSONArray("imageinfo");
                    if (imageInfo == null || imageInfo.length() == 0) continue;
                    JSONObject info = imageInfo.optJSONObject(0);
                    if (info == null) continue;

                    String mime = info.optString("mime", "").toLowerCase(Locale.ROOT);
                    if (!mime.equals("image/jpeg") && !mime.equals("image/png")) continue;

                    String thumbUrl = info.optString("thumburl", "");
                    if (thumbUrl.isEmpty()) thumbUrl = info.optString("url", "");
                    if (thumbUrl.isEmpty()) continue;

                    int tw = info.optInt("thumbwidth", 0);
                    int th = info.optInt("thumbheight", 0);
                    if (tw >= th && tw >= 600) {
                        candidateUrls.add(thumbUrl);
                    } else {
                        fallbackUrls.add(thumbUrl);
                    }
                }
            }

            List<String> pool = !candidateUrls.isEmpty() ? candidateUrls : fallbackUrls;
            if (pool.isEmpty()) return "";

            int startIdx = Math.abs(variantIndex) % pool.size();
            for (int attempt = 0; attempt < Math.min(3, pool.size()); attempt++) {
                String imageUrl = pool.get((startIdx + attempt) % pool.size());
                String savedUri = downloadImageToCache(context, client, imageUrl);
                if (!savedUri.isEmpty()) {
                    return savedUri;
                }
            }
            return "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String downloadImageToCache(Context context, OkHttpClient client, String imageUrl) {
        try {
            Request imgRequest = new Request.Builder()
                    .url(imageUrl)
                    .header("User-Agent", "SocialAI-Android/1.0")
                    .get()
                    .build();

            try (Response imgResponse = client.newCall(imgRequest).execute()) {
                if (!imgResponse.isSuccessful() || imgResponse.body() == null) return "";
                byte[] bytes = imgResponse.body().bytes();
                if (bytes.length < 5000) return "";
                return saveBytesToCache(context, bytes, "ai_photo");
            }
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String saveBytesToCache(Context context, byte[] bytes, String prefix) {
        try {
            File cacheDir = new File(context.getCacheDir(), "ai_images");
            if (!cacheDir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                cacheDir.mkdirs();
            }
            File outFile = new File(cacheDir, prefix + "_" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                fos.write(bytes);
                fos.flush();
            }
            return Uri.fromFile(outFile).toString();
        } catch (Exception e) {
            return "";
        }
    }
}
