package cn.islys.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;

@Config(name = "mini-chat-translator")
public class ClothConfig implements ConfigData {

    // ========== 基础设置 ==========
    private boolean enabled = true;
    private TranslationEngine translationEngine = TranslationEngine.LOCAL;
    private boolean chineseToEnglish = true;
    private boolean translateOwn = true;

    // ========== 百度翻译配置 ==========
    private String baiduAppId = "";
    private String baiduSecretKey = "";

    // ========== 谷歌翻译配置 ==========
    private String googleApiKey = "";  // 官方API密钥
    // 免费版无需配置，直接可用

    public static void init() {
        AutoConfig.register(ClothConfig.class, Toml4jConfigSerializer::new);
    }

    public static ClothConfig get() {
        return AutoConfig.getConfigHolder(ClothConfig.class).getConfig();
    }

    // ========== Getter/Setter ==========
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public TranslationEngine getTranslationEngine() { return translationEngine; }
    public void setTranslationEngine(TranslationEngine engine) { this.translationEngine = engine; }

    public boolean isChineseToEnglish() { return chineseToEnglish; }
    public void setChineseToEnglish(boolean cte) { this.chineseToEnglish = cte; }

    public boolean isTranslateOwn() { return translateOwn; }
    public void setTranslateOwn(boolean translateOwn) { this.translateOwn = translateOwn; }

    public String getBaiduAppId() { return baiduAppId; }
    public void setBaiduAppId(String appId) { this.baiduAppId = appId; }

    public String getBaiduSecretKey() { return baiduSecretKey; }
    public void setBaiduSecretKey(String secretKey) { this.baiduSecretKey = secretKey; }

    public String getGoogleApiKey() { return googleApiKey; }
    public void setGoogleApiKey(String apiKey) { this.googleApiKey = apiKey; }
}