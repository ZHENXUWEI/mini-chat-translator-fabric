package cn.islys.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.EnumListEntry;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
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
        mainCategory.addEntry(entryBuilder.startBooleanToggle(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.enabled"),
                        config.isEnabled())
                .setDefaultValue(true)
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.enabled.@Tooltip"))
                .setSaveConsumer(config::setEnabled)
                .build());

        // 翻译引擎选择
        mainCategory.addEntry(entryBuilder.startEnumSelector(
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
                .build());

        // 启用中译英输出
        mainCategory.addEntry(entryBuilder.startBooleanToggle(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.chineseToEnglish"),
                        config.isChineseToEnglish())
                .setDefaultValue(true)
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.chineseToEnglish.@Tooltip"))
                .setSaveConsumer(config::setChineseToEnglish)
                .build());

        // 是否翻译自己的消息
        mainCategory.addEntry(entryBuilder.startBooleanToggle(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.translateOwn"),
                        config.isTranslateOwn())
                .setDefaultValue(true)
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.translateOwn.@Tooltip"))
                .setSaveConsumer(config::setTranslateOwn)
                .build());

        // ===== 可折叠的百度翻译配置 =====
        SubCategoryBuilder baiduSubBuilder = entryBuilder.startSubCategory(
                Text.literal("§6§l🔵 百度翻译配置"));

        baiduSubBuilder.add(entryBuilder.startStrField(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.baiduAppId"),
                        config.getBaiduAppId())
                .setDefaultValue("")
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.baiduAppId.@Tooltip"))
                .setSaveConsumer(config::setBaiduAppId)
                .build());

        baiduSubBuilder.add(entryBuilder.startStrField(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.baiduSecretKey"),
                        config.getBaiduSecretKey())
                .setDefaultValue("")
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.baiduSecretKey.@Tooltip"))
                .setSaveConsumer(config::setBaiduSecretKey)
                .build());

        mainCategory.addEntry(baiduSubBuilder.build());

        // ===== 可折叠的腾讯翻译配置 =====
        SubCategoryBuilder tencentSubBuilder = entryBuilder.startSubCategory(
                Text.literal("§6§l🐧 腾讯混元配置"));

        tencentSubBuilder.add(entryBuilder.startStrField(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.tencentSecretId"),
                        config.getTencentSecretId())
                .setDefaultValue("")
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.tencentSecretId.@Tooltip"))
                .setSaveConsumer(config::setTencentSecretId)
                .build());

        tencentSubBuilder.add(entryBuilder.startStrField(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.tencentSecretKey"),
                        config.getTencentSecretKey())
                .setDefaultValue("")
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.tencentSecretKey.@Tooltip"))
                .setSaveConsumer(config::setTencentSecretKey)
                .build());

        mainCategory.addEntry(tencentSubBuilder.build());

        // ===== 可折叠的阿里翻译配置 =====
        SubCategoryBuilder aliyunSubBuilder = entryBuilder.startSubCategory(
                Text.literal("§6§l☁️ 阿里通义配置"));

        aliyunSubBuilder.add(entryBuilder.startStrField(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.aliyunApiKey"),
                        config.getAliyunApiKey())
                .setDefaultValue("")
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.aliyunApiKey.@Tooltip"))
                .setSaveConsumer(config::setAliyunApiKey)
                .build());

        mainCategory.addEntry(aliyunSubBuilder.build());

        // 设置保存回调
        builder.setSavingRunnable(() -> AutoConfig.getConfigHolder(ClothConfig.class).save());

        return builder.build();
    }
}