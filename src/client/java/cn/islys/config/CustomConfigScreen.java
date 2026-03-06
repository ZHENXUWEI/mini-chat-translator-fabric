package cn.islys.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.EnumListEntry;
import me.shedaniel.clothconfig2.gui.entries.StringListEntry;
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
                        return Text.literal(((TranslationEngine) anEnum).getDisplayName());
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

        // ===== 可折叠的谷歌翻译配置 =====
        SubCategoryBuilder googleSubBuilder = entryBuilder.startSubCategory(
                Text.literal("§6§l🟢 谷歌翻译配置"));

        // 谷歌官方API密钥输入框
        googleSubBuilder.add(entryBuilder.startStrField(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.googleApiKey"),
                        config.getGoogleApiKey())
                .setDefaultValue("")
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.googleApiKey.@Tooltip"))
                .setSaveConsumer(config::setGoogleApiKey)
                .build());

        mainCategory.addEntry(googleSubBuilder.build());

        // 设置保存回调
        builder.setSavingRunnable(() -> AutoConfig.getConfigHolder(ClothConfig.class).save());

        return builder.build();
    }
}