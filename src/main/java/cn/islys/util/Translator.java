package cn.islys.util;

import cn.islys.config.ClothConfig;
import cn.islys.config.ModConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Translator {
    private static final String BAIDU_API_URL = "https://fanyi-api.baidu.com/api/trans/vip/translate";
    private static final OkHttpClient CLIENT = new OkHttpClient();

    /**
     * 同步翻译（会阻塞调用线程）
     * @param text 要翻译的英文文本
     * @return 翻译后的中文，如果失败则返回 null
     */
    public static String translate(String text) {
        ClothConfig config = ClothConfig.get();
        String appId = config.getAppId();
        String secretKey = config.getSecretKey();

        if (appId.isEmpty() || secretKey.isEmpty()) {
            return "[请在 Mod Menu 中配置百度翻译 API 密钥]";
        }

        // 生成随机数
        String salt = UUID.randomUUID().toString().substring(0, 8);

        // 计算签名 sign = appId + text + salt + secretKey 的 MD5
        String sign = md5(appId + text + salt + secretKey);

        // 构建请求
        RequestBody body = new FormBody.Builder()
                .add("q", text)
                .add("from", "en")      // 源语言：英文
                .add("to", "zh")         // 目标语言：中文
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

            // 检查是否有错误
            if (json.has("error_code")) {
                String errorCode = json.get("error_code").getAsString();
                String errorMsg = json.get("error_msg").getAsString();
                return "[翻译错误: " + errorCode + " - " + errorMsg + "]";
            }

            // 解析翻译结果
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
     * 异步翻译 - 在新线程中执行，不会阻塞调用线程
     * @param text 要翻译的文本
     * @return CompletableFuture，完成后包含翻译结果
     */
    public static CompletableFuture<String> translateAsync(String text) {
        return CompletableFuture.supplyAsync(() -> translate(text));
    }

    /**
     * 计算 MD5
     */
    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
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
