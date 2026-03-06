package cn.islys.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.TextListEntry;
import me.shedaniel.clothconfig2.gui.entries.StringListEntry;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.EnumListEntry;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class CustomConfigScreen {

    public static Screen create(Screen parent) {
        ClothConfig config = ClothConfig.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("text.autoconfig.mini-chat-translator.title"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // 主分类
        ConfigCategory mainCategory = builder.getOrCreateCategory(
                Text.translatable("text.autoconfig.mini-chat-translator.category.general"));

        // ===== 基础设置 =====
        mainCategory.addEntry(entryBuilder.startTextDescription(
                Text.literal("§6§l⚙️ 基础设置")).build());

        // 启用翻译开关
        BooleanListEntry enabledEntry = entryBuilder.startBooleanToggle(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.enabled"),
                        config.isEnabled())
                .setDefaultValue(true)
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.enabled.@Tooltip"))
                .setSaveConsumer(config::setEnabled)
                .build();
        mainCategory.addEntry(enabledEntry);

        // ===== 翻译引擎选择 =====
        EnumListEntry<TranslationEngine> engineEntry = entryBuilder.startEnumSelector(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.engine"),
                        TranslationEngine.class,
                        config.getTranslationEngine())
                .setDefaultValue(TranslationEngine.LOCAL)
                .setEnumNameProvider(anEnum -> {
                    if (anEnum instanceof TranslationEngine) {
                        return switch ((TranslationEngine) anEnum) {
                            case LOCAL -> Text.literal("🌐 本地模型 (离线免费)");
                            case BAIDU -> Text.literal("🔵 百度翻译 (在线)");
                            case TENCENT -> Text.literal("🐧 腾讯混元 (在线)");
                            case ALIYUN -> Text.literal("☁️ 阿里通义 (在线)");
                        };
                    }
                    return Text.literal(anEnum.toString());
                })
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.engine.@Tooltip"))
                .setSaveConsumer(config::setTranslationEngine)
                .build();
        mainCategory.addEntry(engineEntry);

        // 启用中译英输出
        BooleanListEntry cteEntry = entryBuilder.startBooleanToggle(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.chineseToEnglish"),
                        config.isChineseToEnglish())
                .setDefaultValue(true)
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.chineseToEnglish.@Tooltip"))
                .setSaveConsumer(config::setChineseToEnglish)
                .build();
        mainCategory.addEntry(cteEntry);

        // 是否翻译自己的消息
        BooleanListEntry translateOwnEntry = entryBuilder.startBooleanToggle(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.translateOwn"),
                        config.isTranslateOwn())
                .setDefaultValue(true)
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.translateOwn.@Tooltip"))
                .setSaveConsumer(config::setTranslateOwn)
                .build();
        mainCategory.addEntry(translateOwnEntry);

        // ===== 百度翻译配置 =====
        TextListEntry baiduTitle = entryBuilder.startTextDescription(
                Text.literal("§6§l🔵 百度翻译配置")).build();

        StringListEntry baiduAppIdEntry = entryBuilder.startStrField(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.baiduAppId"),
                        config.getBaiduAppId())
                .setDefaultValue("")
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.baiduAppId.@Tooltip"))
                .setSaveConsumer(config::setBaiduAppId)
                .build();

        StringListEntry baiduSecretKeyEntry = entryBuilder.startStrField(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.baiduSecretKey"),
                        config.getBaiduSecretKey())
                .setDefaultValue("")
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.baiduSecretKey.@Tooltip"))
                .setSaveConsumer(config::setBaiduSecretKey)
                .build();

        // ===== 腾讯翻译配置 =====
        TextListEntry tencentTitle = entryBuilder.startTextDescription(
                Text.literal("§6§l🐧 腾讯混元配置")).build();

        StringListEntry tencentSecretIdEntry = entryBuilder.startStrField(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.tencentSecretId"),
                        config.getTencentSecretId())
                .setDefaultValue("")
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.tencentSecretId.@Tooltip"))
                .setSaveConsumer(config::setTencentSecretId)
                .build();

        StringListEntry tencentSecretKeyEntry = entryBuilder.startStrField(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.tencentSecretKey"),
                        config.getTencentSecretKey())
                .setDefaultValue("")
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.tencentSecretKey.@Tooltip"))
                .setSaveConsumer(config::setTencentSecretKey)
                .build();

        // ===== 阿里翻译配置 =====
        TextListEntry aliyunTitle = entryBuilder.startTextDescription(
                Text.literal("§6§l☁️ 阿里通义配置")).build();

        StringListEntry aliyunApiKeyEntry = entryBuilder.startStrField(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.aliyunApiKey"),
                        config.getAliyunApiKey())
                .setDefaultValue("")
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.aliyunApiKey.@Tooltip"))
                .setSaveConsumer(config::setAliyunApiKey)
                .build();

        // ===== 使用简单的布尔条件设置可见性 =====
        // 注意：Cloth Config 会自动处理条件更新，不需要手动调用 rebuild
        // 当 engineEntry 的值改变时，这些条件会被重新评估

        // 百度相关条目 - 当引擎为 BAIDU 时显示
        baiduTitle.setRequirement(() -> config.getTranslationEngine() == TranslationEngine.BAIDU);
        baiduAppIdEntry.setRequirement(() -> config.getTranslationEngine() == TranslationEngine.BAIDU);
        baiduSecretKeyEntry.setRequirement(() -> config.getTranslationEngine() == TranslationEngine.BAIDU);

        // 腾讯相关条目 - 当引擎为 TENCENT 时显示
        tencentTitle.setRequirement(() -> config.getTranslationEngine() == TranslationEngine.TENCENT);
        tencentSecretIdEntry.setRequirement(() -> config.getTranslationEngine() == TranslationEngine.TENCENT);
        tencentSecretKeyEntry.setRequirement(() -> config.getTranslationEngine() == TranslationEngine.TENCENT);

        // 阿里相关条目 - 当引擎为 ALIYUN 时显示
        aliyunTitle.setRequirement(() -> config.getTranslationEngine() == TranslationEngine.ALIYUN);
        aliyunApiKeyEntry.setRequirement(() -> config.getTranslationEngine() == TranslationEngine.ALIYUN);

        // 将所有条目添加到主分类
        mainCategory.addEntry(baiduTitle);
        mainCategory.addEntry(baiduAppIdEntry);
        mainCategory.addEntry(baiduSecretKeyEntry);

        mainCategory.addEntry(tencentTitle);
        mainCategory.addEntry(tencentSecretIdEntry);
        mainCategory.addEntry(tencentSecretKeyEntry);

        mainCategory.addEntry(aliyunTitle);
        mainCategory.addEntry(aliyunApiKeyEntry);

        // 设置保存回调
        builder.setSavingRunnable(() -> AutoConfig.getConfigHolder(ClothConfig.class).save());

        return builder.build();
    }
}