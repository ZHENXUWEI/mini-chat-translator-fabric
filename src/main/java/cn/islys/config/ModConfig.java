package cn.islys.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("mini-chat-translator.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // 配置字段（public 以便直接访问）
    public boolean enabled = true;
    public TranslationEngine translationEngine = TranslationEngine.LOCAL;
    public boolean chineseToEnglish = true;
    public boolean translateOwn = true;
    public String baiduAppId = "";
    public String baiduSecretKey = "";
    public String googleApiKey = "";

    private static ModConfig INSTANCE;

    public static ModConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                INSTANCE = GSON.fromJson(Files.newBufferedReader(CONFIG_PATH), ModConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
                INSTANCE = new ModConfig();
            }
        } else {
            INSTANCE = new ModConfig();
            save();
        }
    }

    public static void save() {
        try {
            Files.write(CONFIG_PATH, GSON.toJson(INSTANCE).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}