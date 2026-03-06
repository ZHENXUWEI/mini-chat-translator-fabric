package cn.islys.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
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

        // 总开关
        mainCategory.addEntry(entryBuilder.startBooleanToggle(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.enabled"),
                        config.isEnabled())
                .setDefaultValue(true)
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.enabled.@Tooltip"))
                .setSaveConsumer(config::setEnabled)
                .build());

        // 翻译引擎选择（下拉框）
        mainCategory.addEntry(entryBuilder.startEnumSelector(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.engine"),
                        TranslationEngine.class,
                        config.getTranslationEngine())
                .setDefaultValue(TranslationEngine.BAIDU)
                .setEnumNameProvider(anEnum -> {
                    if (anEnum instanceof TranslationEngine) {
                        return Text.literal(((TranslationEngine) anEnum).getDisplayName());
                    }
                    return Text.literal(anEnum.toString());
                })
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.engine.@Tooltip"))
                .setSaveConsumer(config::setTranslationEngine)
                .build());

        // 添加分隔线
        mainCategory.addEntry(entryBuilder.startTextDescription(Text.literal("")).build());

        // ===== 百度翻译配置 =====
        SubCategoryBuilder baiduSub = entryBuilder.startSubCategory(
                Text.literal("§6§l🌐 百度翻译配置"));

        baiduSub.add(entryBuilder.startStrField(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.baiduAppId"),
                        config.getBaiduAppId())
                .setDefaultValue("")
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.baiduAppId.@Tooltip"))
                .setSaveConsumer(config::setBaiduAppId)
                .build());

        baiduSub.add(entryBuilder.startStrField(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.baiduSecretKey"),
                        config.getBaiduSecretKey())
                .setDefaultValue("")
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.baiduSecretKey.@Tooltip"))
                .setSaveConsumer(config::setBaiduSecretKey)
                .build());

        mainCategory.addEntry(baiduSub.build());

        // ===== 腾讯混元配置 =====
        SubCategoryBuilder tencentSub = entryBuilder.startSubCategory(
                Text.literal("§6§l☁️ 腾讯混元配置"));

        tencentSub.add(entryBuilder.startStrField(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.tencentSecretId"),
                        config.getTencentSecretId())
                .setDefaultValue("")
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.tencentSecretId.@Tooltip"))
                .setSaveConsumer(config::setTencentSecretId)
                .build());

        tencentSub.add(entryBuilder.startStrField(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.tencentSecretKey"),
                        config.getTencentSecretKey())
                .setDefaultValue("")
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.tencentSecretKey.@Tooltip"))
                .setSaveConsumer(config::setTencentSecretKey)
                .build());

        mainCategory.addEntry(tencentSub.build());

        // ===== 阿里通义配置 =====
        SubCategoryBuilder aliyunSub = entryBuilder.startSubCategory(
                Text.literal("§6§l🌀 阿里通义配置"));

        aliyunSub.add(entryBuilder.startStrField(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.aliyunApiKey"),
                        config.getAliyunApiKey())
                .setDefaultValue("")
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.aliyunApiKey.@Tooltip"))
                .setSaveConsumer(config::setAliyunApiKey)
                .build());

        mainCategory.addEntry(aliyunSub.build());

        // ===== 本地模型配置 =====
        SubCategoryBuilder localSub = entryBuilder.startSubCategory(
                Text.literal("§6§l💻 本地模型配置"));

        localSub.add(entryBuilder.startBooleanToggle(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.autoDownloadModel"),
                        config.isAutoDownloadModel())
                .setDefaultValue(true)
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.autoDownloadModel.@Tooltip"))
                .setSaveConsumer(config::setAutoDownloadModel)
                .build());

        localSub.add(entryBuilder.startStrField(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.localModelPath"),
                        config.getLocalModelPath())
                .setDefaultValue("")
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.localModelPath.@Tooltip"))
                .setSaveConsumer(config::setLocalModelPath)
                .build());

        mainCategory.addEntry(localSub.build());

        // 设置保存回调
        builder.setSavingRunnable(() -> {
            AutoConfig.getConfigHolder(ClothConfig.class).save();
        });

        return builder.build();
    }
}