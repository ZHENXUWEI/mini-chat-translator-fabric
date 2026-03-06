package cn.islys.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;

@Config(name = "mini-chat-translator")
public class ClothConfig implements ConfigData {

    // ========== 基础设置 ==========
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.PrefixText
    private boolean enabled = true;

    @ConfigEntry.Gui.Tooltip
    private TranslationEngine translationEngine = TranslationEngine.LOCAL;

    @ConfigEntry.Gui.Tooltip
    private boolean chineseToEnglish = true;

    @ConfigEntry.Gui.Tooltip
    private boolean translateOwn = true;

    // ========== 百度翻译配置（仅在选择百度时显示）==========
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.CollapsibleObject
    private BaiduConfig baidu = new BaiduConfig();

    // ========== 腾讯翻译配置（仅在选择腾讯时显示）==========
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.CollapsibleObject
    private TencentConfig tencent = new TencentConfig();

    // ========== 阿里翻译配置（仅在选择阿里时显示）==========
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.CollapsibleObject
    private AliyunConfig aliyun = new AliyunConfig();

    public static class BaiduConfig {
        @ConfigEntry.Gui.Tooltip
        public String appId = "";

        @ConfigEntry.Gui.Tooltip
        public String secretKey = "";
    }

    public static class TencentConfig {
        @ConfigEntry.Gui.Tooltip
        public String secretId = "";

        @ConfigEntry.Gui.Tooltip
        public String secretKey = "";
    }

    public static class AliyunConfig {
        @ConfigEntry.Gui.Tooltip
        public String apiKey = "";
    }

    public static void init() {
        AutoConfig.register(ClothConfig.class, Toml4jConfigSerializer::new);
    }

    public static ClothConfig get() {
        return AutoConfig.getConfigHolder(ClothConfig.class).getConfig();
    }

    // Getter 和 Setter
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public TranslationEngine getTranslationEngine() { return translationEngine; }
    public void setTranslationEngine(TranslationEngine engine) { this.translationEngine = engine; }

    public boolean isChineseToEnglish() { return chineseToEnglish; }
    public void setChineseToEnglish(boolean cte) { this.chineseToEnglish = cte; }

    public boolean isTranslateOwn() { return translateOwn; }
    public void setTranslateOwn(boolean translateOwn) { this.translateOwn = translateOwn; }

    // 百度配置
    public String getBaiduAppId() { return baidu.appId; }
    public void setBaiduAppId(String appId) { this.baidu.appId = appId; }

    public String getBaiduSecretKey() { return baidu.secretKey; }
    public void setBaiduSecretKey(String secretKey) { this.baidu.secretKey = secretKey; }

    // 腾讯配置
    public String getTencentSecretId() { return tencent.secretId; }
    public void setTencentSecretId(String secretId) { this.tencent.secretId = secretId; }

    public String getTencentSecretKey() { return tencent.secretKey; }
    public void setTencentSecretKey(String secretKey) { this.tencent.secretKey = secretKey; }

    // 阿里配置
    public String getAliyunApiKey() { return aliyun.apiKey; }
    public void setAliyunApiKey(String apiKey) { this.aliyun.apiKey = apiKey; }

    /**
     * 获取当前选中的翻译引擎对应的 APP ID
     */
    public String getAppId() {
        return switch (translationEngine) {
            case BAIDU -> baidu.appId;
            case TENCENT -> tencent.secretId;
            case ALIYUN -> aliyun.apiKey;
            default -> "";
        };
    }

    /**
     * 获取当前选中的翻译引擎对应的 Secret Key
     */
    public String getSecretKey() {
        return switch (translationEngine) {
            case BAIDU -> baidu.secretKey;
            case TENCENT -> tencent.secretKey;
            case ALIYUN -> aliyun.apiKey;
            default -> "";
        };
    }
}