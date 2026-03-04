package cn.islys.util;

import cn.islys.config.ModConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class Translator {
    private static final String BAIDU_API_URL = "https://fanyi-api.baidu.com/api/trans/vip/translate";
    private static final OkHttpClient CLIENT = new OkHttpClient();

    /**
     * 翻译文本（自动检测源语言，翻译成中文）
     * @param text 要翻译的英文文本
     * @return 翻译后的中文，如果失败则返回 null
     */
    public static String translate(String text) {
        String appId = ModConfig.getAppId();
        String secretKey = ModConfig.getSecretKey();

        // 检查配置是否填写
        if (appId.isEmpty() || secretKey.isEmpty()) {
            return "[请先在配置文件中填写百度翻译 APP ID 和密钥]";
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
