package cn.islys.config;

public enum TranslationEngine {
    BAIDU("百度翻译（在线）", "百度翻译API，需配置APP ID和密钥，有免费额度"),
    TENCENT("腾讯混元（在线）", "腾讯云大模型，需配置API密钥，新用户100万Token免费"),
    ALIYUN("阿里通义（在线）", "阿里云通义千问，需配置API密钥，新用户100万Token免费"),
    LOCAL("本地模型（离线）", "完全离线翻译，首次使用需下载约470MB模型文件");

    private final String displayName;
    private final String description;

    TranslationEngine(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
