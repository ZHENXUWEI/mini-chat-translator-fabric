package cn.islys.config;

import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ModConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("mini-chat-translator.properties");
    private static Properties properties = new Properties();

    private static final String DEFAULT_APP_ID = "";
    private static final String DEFAULT_SECRET_KEY = "";

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (var input = Files.newInputStream(CONFIG_PATH)) {
                properties.load(input);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // 创建默认配置文件
            properties.setProperty("app_id", DEFAULT_APP_ID);
            properties.setProperty("secret_key", DEFAULT_SECRET_KEY);
            save();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (var output = Files.newOutputStream(CONFIG_PATH)) {
                properties.store(output, "Mini Chat Translator Config\n请填写你的百度翻译 APP ID 和密钥");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getAppId() {
        return properties.getProperty("app_id", DEFAULT_APP_ID);
    }

    public static String getSecretKey() {
        return properties.getProperty("secret_key", DEFAULT_SECRET_KEY);
    }

    public static void setAppId(String appId) {
        properties.setProperty("app_id", appId);
        save();
    }

    public static void setSecretKey(String secretKey) {
        properties.setProperty("secret_key", secretKey);
        save();
    }
}
