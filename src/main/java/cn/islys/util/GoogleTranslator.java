package cn.islys.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class GoogleTranslator {
    private static final Logger LOGGER = LoggerFactory.getLogger("MiniChatTranslator");
    private static final OkHttpClient CLIENT = new OkHttpClient();

    /**
     * 免费版谷歌翻译（无需API密钥）
     * 使用 translate.googleapis.com 的免费接口
     */
    public static CompletableFuture<String> translateFree(String text, String targetLang) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
                String url = String.format(
                        "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=%s&dt=t&q=%s",
                        targetLang, encodedText
                );

                Request request = new Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .build();

                try (Response response = CLIENT.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        return "[谷歌翻译免费版响应错误]";
                    }

                    String responseBody = response.body().string();
                    // 解析谷歌返回的JSON数组
                    JsonArray array = JsonParser.parseString(responseBody).getAsJsonArray();
                    JsonArray sentences = array.get(0).getAsJsonArray();

                    StringBuilder result = new StringBuilder();
                    for (int i = 0; i < sentences.size(); i++) {
                        result.append(sentences.get(i).getAsJsonArray().get(0).getAsString());
                    }

                    return result.toString();
                }

            } catch (IOException e) {
                LOGGER.error("谷歌翻译免费版请求失败", e);
                return "[谷歌翻译免费版网络错误]";
            } catch (Exception e) {
                LOGGER.error("谷歌翻译免费版解析失败", e);
                return "[谷歌翻译免费版解析错误]";
            }
        });
    }

    /**
     * 官方版谷歌翻译（需要API密钥）
     * 使用 Google Cloud Translation API
     */
    public static CompletableFuture<String> translateOfficial(String text, String targetLang, String apiKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (apiKey == null || apiKey.isEmpty()) {
                    return "[请配置谷歌官方API密钥]";
                }

                String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
                String url = String.format(
                        "https://translation.googleapis.com/language/translate/v2?q=%s&target=%s&key=%s&format=text",
                        encodedText, targetLang, apiKey
                );

                Request request = new Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0")
                        .build();

                try (Response response = CLIENT.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        return "[谷歌官方API响应错误]";
                    }

                    String responseBody = response.body().string();
                    // 解析官方API返回的JSON
                    JsonArray translations = JsonParser.parseString(responseBody)
                            .getAsJsonObject()
                            .getAsJsonObject("data")
                            .getAsJsonArray("translations");

                    return translations.get(0)
                            .getAsJsonObject()
                            .get("translatedText")
                            .getAsString();
                }

            } catch (IOException e) {
                LOGGER.error("谷歌官方API请求失败", e);
                return "[谷歌官方API网络错误]";
            } catch (Exception e) {
                LOGGER.error("谷歌官方API解析失败", e);
                return "[谷歌官方API解析错误]";
            }
        });
    }
}