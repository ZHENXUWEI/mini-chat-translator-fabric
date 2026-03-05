package cn.islys.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;

@Config(name = "mini-chat-translator")
public class ClothConfig implements ConfigData {
    private String appId = "";
    private String secretKey = "";
    private boolean enabled = true;
    private boolean translateOwn = true;

    // 是否将中文翻译成英文发送
    private boolean chineseToEnglish = true;

    public static void init() {
        AutoConfig.register(ClothConfig.class, Toml4jConfigSerializer::new);
    }

    public static ClothConfig get() {
        return AutoConfig.getConfigHolder(ClothConfig.class).getConfig();
    }

    // Getter 和 Setter
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isTranslateOwn() { return translateOwn; }
    public void setTranslateOwn(boolean translateOwn) { this.translateOwn = translateOwn; }

    public boolean isChineseToEnglish() { return chineseToEnglish; }
    public void setChineseToEnglish(boolean chineseToEnglish) { this.chineseToEnglish = chineseToEnglish; }
}
