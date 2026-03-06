package cn.islys.translator;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class TranslationServer {
    private static final Logger LOGGER = LoggerFactory.getLogger("MiniChatTranslator");
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    private static final Gson GSON = new Gson();

    private static int port = -1;

    /**
     * 设置服务器端口
     */
    public static void setPort(int port) {
        TranslationServer.port = port;
    }

    /**
     * 翻译文本
     */
    public static CompletableFuture<String> translate(String text, String sourceLang, String targetLang) {
        if (port == -1) {
            return CompletableFuture.completedFuture("[翻译服务器未启动]");
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject request = new JsonObject();
                request.addProperty("text", text);
                request.addProperty("source", sourceLang);
                request.addProperty("target", targetLang);

                RequestBody body = RequestBody.create(
                        GSON.toJson(request),
                        MediaType.parse("application/json; charset=utf-8")
                );

                Request httpRequest = new Request.Builder()
                        .url("http://localhost:" + port)
                        .post(body)
                        .build();

                try (Response response = CLIENT.newCall(httpRequest).execute()) {
                    if (!response.isSuccessful()) {
                        return "[翻译服务器错误]";
                    }

                    String responseBody = response.body().string();
                    JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

                    if (json.get("success").getAsBoolean()) {
                        return json.get("result").getAsString();
                    } else {
                        return "[翻译失败: " + json.get("error").getAsString() + "]";
                    }
                }

            } catch (IOException e) {
                LOGGER.error("翻译请求失败", e);
                return "[网络错误: " + e.getMessage() + "]";
            }
        });
    }
}