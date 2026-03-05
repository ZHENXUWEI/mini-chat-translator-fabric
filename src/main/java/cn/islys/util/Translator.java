package cn.islys.util;

import cn.islys.config.ClothConfig;
import cn.islys.config.ModConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Translator {
    private static final String BAIDU_API_URL = "https://fanyi-api.baidu.com/api/trans/vip/translate";
    private static final OkHttpClient CLIENT = new OkHttpClient();

    /**
     * 翻译文本（自动检测源语言，翻译成目标语言）
     * @param text 要翻译的文本
     * @param from 源语言代码 (如 "en", "zh", "auto")
     * @param to 目标语言代码
     * @return 翻译后的文本，失败返回错误信息
     */
    public static String translate(String text, String from, String to) {
        ClothConfig config = ClothConfig.get();
        String appId = config.getAppId();
        String secretKey = config.getSecretKey();

        if (appId.isEmpty() || secretKey.isEmpty()) {
            return "[请在配置中填写百度翻译 API 密钥]";
        }

        String salt = UUID.randomUUID().toString().substring(0, 8);

        // 使用 UTF-8 字节数组计算签名
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
                String errorMsg = json.get("error_msg").getAsString();
                return "[翻译错误: " + errorCode + " - " + errorMsg + "]";
            }

            if (json.has("trans_result")) {
                var result = json.getAsJsonArray("trans_result").get(0).getAsJsonObject();
                return result.get("dst").getAsString();
            }

            return null;

        } catch (IOException e) {
            e.printStackTrace();
            return "[网络错误: " + e.getMessage() + "]";
        }
    }

    /**
     * 英译中（保留原方法）
     */
    public static String translateEnToZh(String text) {
        return translate(text, "en", "zh");
    }

    /**
     * 中译英（新方法）
     */
    public static String translateZhToEn(String text) {
        return translate(text, "zh", "en");
    }

    /**
     * 异步英译中
     */
    public static CompletableFuture<String> translateEnToZhAsync(String text) {
        return CompletableFuture.supplyAsync(() -> translateEnToZh(text));
    }

    /**
     * 异步中译英
     */
    public static CompletableFuture<String> translateZhToEnAsync(String text) {
        return CompletableFuture.supplyAsync(() -> translateZhToEn(text));
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
