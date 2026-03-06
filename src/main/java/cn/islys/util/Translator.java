package cn.islys.util;

import cn.islys.config.ClothConfig;
import cn.islys.config.TranslationEngine;
import cn.islys.translator.TranslationServer;
import cn.islys.translator.PythonManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Translator {
    private static final Logger LOGGER = LoggerFactory.getLogger("MiniChatTranslator");
    private static final String BAIDU_API_URL = "https://fanyi-api.baidu.com/api/trans/vip/translate";
    private static final OkHttpClient CLIENT = new OkHttpClient();

    /**
     * 统一的翻译入口 - 根据配置自动选择翻译引擎
     */
    public static CompletableFuture<String> translateAsync(String text, String from, String to) {
        ClothConfig config = ClothConfig.get();
        TranslationEngine engine = config.getTranslationEngine();

        LOGGER.debug("使用翻译引擎: {}", engine);

        switch (engine) {
            case LOCAL:
                return translateLocalAsync(text, from, to);
            case BAIDU:
                return translateBaiduAsync(text, from, to);
            case GOOGLE_FREE:
                return GoogleTranslator.translateFree(text, to);
            case GOOGLE_OFFICIAL:
                return GoogleTranslator.translateOfficial(text, to, config.getGoogleApiKey());
            default:
                return CompletableFuture.completedFuture("[未选择翻译引擎]");
        }
    }

    /**
     * 本地翻译实现
     */
    private static CompletableFuture<String> translateLocalAsync(String text, String from, String to) {
        if (!PythonManager.isServerRunning()) {
            LOGGER.warn("本地翻译服务器未运行，尝试启动...");
            return CompletableFuture.completedFuture("[本地翻译服务器未启动,请耐心等待,或使用在线翻译]");
        }
        return TranslationServer.translate(text, from, to);
    }

    /**
     * 百度翻译异步实现
     */
    private static CompletableFuture<String> translateBaiduAsync(String text, String from, String to) {
        return CompletableFuture.supplyAsync(() -> translateBaidu(text, from, to));
    }

    /**
     * 百度翻译实现（同步）
     */
    private static String translateBaidu(String text, String from, String to) {
        ClothConfig config = ClothConfig.get();
        String appId = config.getBaiduAppId();
        String secretKey = config.getBaiduSecretKey();

        if (appId.isEmpty() || secretKey.isEmpty()) {
            return "[请在配置中填写百度翻译 API 密钥]";
        }

        try {
            String salt = UUID.randomUUID().toString().substring(0, 8);
            String signStr = appId + text + salt + secretKey;
            String sign = md5(signStr.getBytes(StandardCharsets.UTF_8));

            RequestBody body = new FormBody.Builder()
                    .add("q", text)
                    .add("from", from)
                    .add("to", to)
                    .add("appid", appId)
                    .add("salt", salt)
                    .add("sign", sign)
                    .build();

            Request request = new Request.Builder()
                    .url(BAIDU_API_URL)
                    .post(body)
                    .build();

            try (Response response = CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return "[翻译服务响应错误]";
                }

                String responseBody = response.body().string();
                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

                if (json.has("error_code")) {
                    String errorCode = json.get("error_code").getAsString();
                    return "[翻译错误: " + errorCode + "]";
                }

                if (json.has("trans_result")) {
                    var result = json.getAsJsonArray("trans_result").get(0).getAsJsonObject();
                    return result.get("dst").getAsString();
                }
            }
        } catch (Exception e) {
            LOGGER.error("百度翻译异常", e);
            return "[翻译异常: " + e.getMessage() + "]";
        }

        return null;
    }

    // ========== 便捷方法 ==========

    /**
     * 英译中异步
     */
    public static CompletableFuture<String> translateEnToZhAsync(String text) {
        return translateAsync(text, "en", "zh");
    }

    /**
     * 中译英异步
     */
    public static CompletableFuture<String> translateZhToEnAsync(String text) {
        return translateAsync(text, "zh", "en");
    }

    /**
     * 同步翻译方法（保留向后兼容，使用百度）
     */
    public static String translate(String text, String from, String to) {
        return translateBaidu(text, from, to);
    }

    /**
     * 英译中同步方法
     */
    public static String translateEnToZh(String text) {
        return translate(text, "en", "zh");
    }

    /**
     * 中译英同步方法
     */
    public static String translateZhToEn(String text) {
        return translate(text, "zh", "en");
    }

    private static String md5(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}