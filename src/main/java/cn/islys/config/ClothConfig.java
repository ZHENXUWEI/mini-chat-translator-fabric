package cn.islys.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;

@Config(name = "mini-chat-translator")
public class ClothConfig implements ConfigData {
    // 新增：翻译引擎选择
    private TranslationEngine translationEngine = TranslationEngine.BAIDU;

    // 百度翻译配置
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.PrefixText
    private String baiduAppId = "";

    @ConfigEntry.Gui.Tooltip
    private String baiduSecretKey = "";

    // 腾讯混元配置
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.PrefixText
    private String tencentSecretId = "";

    @ConfigEntry.Gui.Tooltip
    private String tencentSecretKey = "";

    // 阿里通义配置
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.PrefixText
    private String aliyunApiKey = "";

    // 本地模型配置
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.PrefixText
    private boolean autoDownloadModel = true;  // 首次启动自动下载

    @ConfigEntry.Gui.Tooltip
    private String localModelPath = "";  // 可自定义模型路径

    // 原有功能开关
    private boolean enabled = true;
    private boolean translateOwn = true;
    private boolean chineseToEnglish = true;

    public static void init() {
        AutoConfig.register(ClothConfig.class, Toml4jConfigSerializer::new);
    }

    public static ClothConfig get() {
        return AutoConfig.getConfigHolder(ClothConfig.class).getConfig();
    }

    /**
     * 获取当前选中的翻译引擎对应的 APP ID
     * 根据 translationEngine 返回对应的 ID
     */
    public String getAppId() {
        return switch (translationEngine) {
            case BAIDU -> baiduAppId;
            case TENCENT -> tencentSecretId;  // 腾讯用的是 SecretId
            case ALIYUN -> aliyunApiKey;     // 阿里用的是 API Key
            case LOCAL -> "";                // 本地模型不需要
            default -> "";
        };
    }

    /**
     * 获取当前选中的翻译引擎对应的 Secret Key
     */
    public String getSecretKey() {
        return switch (translationEngine) {
            case BAIDU -> baiduSecretKey;
            case TENCENT -> tencentSecretKey;
            case ALIYUN -> aliyunApiKey;      // 阿里只有 API Key
            case LOCAL -> "";
            default -> "";
        };
    }

    // Getter 和 Setter 方法
    public TranslationEngine getTranslationEngine() { return translationEngine; }
    public void setTranslationEngine(TranslationEngine engine) { this.translationEngine = engine; }

    public String getBaiduAppId() { return baiduAppId; }
    public void setBaiduAppId(String appId) { this.baiduAppId = appId; }

    public String getBaiduSecretKey() { return baiduSecretKey; }
    public void setBaiduSecretKey(String secretKey) { this.baiduSecretKey = secretKey; }

    public String getTencentSecretId() { return tencentSecretId; }
    public void setTencentSecretId(String secretId) { this.tencentSecretId = secretId; }

    public String getTencentSecretKey() { return tencentSecretKey; }
    public void setTencentSecretKey(String secretKey) { this.tencentSecretKey = secretKey; }

    public String getAliyunApiKey() { return aliyunApiKey; }
    public void setAliyunApiKey(String apiKey) { this.aliyunApiKey = apiKey; }

    public boolean isAutoDownloadModel() { return autoDownloadModel; }
    public void setAutoDownloadModel(boolean auto) { this.autoDownloadModel = auto; }

    public String getLocalModelPath() { return localModelPath; }
    public void setLocalModelPath(String path) { this.localModelPath = path; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isTranslateOwn() { return translateOwn; }
    public void setTranslateOwn(boolean translateOwn) { this.translateOwn = translateOwn; }

    public boolean isChineseToEnglish() { return chineseToEnglish; }
    public void setChineseToEnglish(boolean chineseToEnglish) { this.chineseToEnglish = chineseToEnglish; }
}