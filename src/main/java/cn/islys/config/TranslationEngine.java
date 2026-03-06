package cn.islys.config;

public enum TranslationEngine {
    LOCAL("🌐 本地模型 (离线免费)"),
    BAIDU("🔵 百度翻译 (在线)"),
    GOOGLE_FREE("🟢 谷歌翻译 (免费版)"),
    GOOGLE_OFFICIAL("🔴 谷歌翻译 (官方API)");

    private final String displayName;

    TranslationEngine(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}