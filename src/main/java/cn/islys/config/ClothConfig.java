package cn.islys.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;

@Config(name = "mini-chat-translator")
public class ClothConfig implements ConfigData {

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.PrefixText
    private String appId = "";

    @ConfigEntry.Gui.Tooltip
    private String secretKey = "";

    // 开关翻译功能
    @ConfigEntry.Gui.Tooltip
    private boolean enabled = true;

    // 是否翻译自己的消息
    @ConfigEntry.Gui.Tooltip
    private boolean translateOwn = true;

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
}
