package cn.islys.util;

import cn.islys.config.ClothConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TranslatorFactory {
    private static final OkHttpClient CLIENT = new OkHttpClient();

    /**
     * 根据配置选择翻译引擎
     */
    public static CompletableFuture<String> translate(String text, String from, String to) {
        ClothConfig config = ClothConfig.get();

        switch (config.getTranslationEngine()) {
            case BAIDU:
                return translateBaidu(text, from, to);
            case TENCENT:
                return translateTencent(text, from, to);
            case ALIYUN:
                return translateAliyun(text, from, to);
            case LOCAL:
                return translateLocal(text, from, to);
            default:
                return CompletableFuture.completedFuture("[未选择翻译引擎]");
        }
    }

    /**
     * 百度翻译实现（你原有的代码）
     */
    private static CompletableFuture<String> translateBaidu(String text, String from, String to) {
        return CompletableFuture.supplyAsync(() -> {
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
                        .url("https://fanyi-api.baidu.com/api/trans/vip/translate")
                        .post(body)
                        .build();

                try (Response response = CLIENT.newCall(request).execute()) {
                    if (!response.isSuccessful()) return "[翻译服务响应错误]";

                    String responseBody = response.body().string();
                    JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

                    if (json.has("error_code")) {
                        return "[翻译错误: " + json.get("error_code").getAsString() + "]";
                    }

                    if (json.has("trans_result")) {
                        var result = json.getAsJsonArray("trans_result").get(0).getAsJsonObject();
                        return result.get("dst").getAsString();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "[翻译异常: " + e.getMessage() + "]";
            }

            return null;
        });
    }

    /**
     * 腾讯混元翻译
     * 参考：腾讯云官方文档
     */
    private static CompletableFuture<String> translateTencent(String text, String from, String to) {
        return CompletableFuture.supplyAsync(() -> {
            ClothConfig config = ClothConfig.get();
            String secretId = config.getTencentSecretId();
            String secretKey = config.getTencentSecretKey();

            if (secretId.isEmpty() || secretKey.isEmpty()) {
                return "[请在配置中填写腾讯云密钥]";
            }

            try {
                // 腾讯云 API 需要构建签名，这里简化处理
                // 实际实现需要参考腾讯云官方文档
                // https://cloud.tencent.com/document/product/551/73822

                // 这里假设有一个腾讯混元的API网关地址
                String url = "https://hunyuan.tencentcloudapi.com";

                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("ModelName", "hunyuan-translate");
                requestBody.addProperty("SourceText", text);
                requestBody.addProperty("SourceLanguage", from);
                requestBody.addProperty("TargetLanguage", to);

                // 构建签名请求...
                // 由于腾讯云签名较复杂，建议集成腾讯云 Java SDK

                return "[腾讯混元翻译暂未实现]"; // 临时返回

            } catch (Exception e) {
                e.printStackTrace();
                return "[腾讯翻译异常: " + e.getMessage() + "]";
            }
        });
    }

    /**
     * 阿里通义翻译
     */
    private static CompletableFuture<String> translateAliyun(String text, String from, String to) {
        return CompletableFuture.supplyAsync(() -> {
            ClothConfig config = ClothConfig.get();
            String apiKey = config.getAliyunApiKey();

            if (apiKey.isEmpty()) {
                return "[请在配置中填写阿里云 API Key]";
            }

            try {
                // 阿里云通义千问 API
                String url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

                JsonObject messages = new JsonObject();
                messages.addProperty("role", "user");
                messages.addProperty("content", "请将以下" + from + "文本翻译成" + to + "：" + text);

                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("model", "qwen-plus");
                requestBody.add("messages", JsonParser.parseString("[{\"role\":\"user\",\"content\":\"" +
                        "请将以下" + from + "文本翻译成" + to + "：" + text + "\"}]"));

                Request request = new Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .post(RequestBody.create(requestBody.toString(),
                                MediaType.parse("application/json")))
                        .build();

                // ... 处理响应

                return "[阿里翻译暂未完全实现]";

            } catch (Exception e) {
                e.printStackTrace();
                return "[阿里翻译异常: " + e.getMessage() + "]";
            }
        });
    }

    /**
     * 本地模型翻译
     */
    private static CompletableFuture<String> translateLocal(String text, String from, String to) {
        return CompletableFuture.supplyAsync(() -> {
            ClothConfig config = ClothConfig.get();

            // 检查模型是否存在
            if (!LocalModelManager.modelExists()) {
                if (config.isAutoDownloadModel()) {
                    LocalModelManager.startDownload();
                    return "[翻译模型正在下载中，请稍候...]";
                } else {
                    return "[未找到本地翻译模型，请在配置中设置模型路径]";
                }
            }

            // 加载模型并翻译
            return LocalModelManager.translate(text, from, to);
        });
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