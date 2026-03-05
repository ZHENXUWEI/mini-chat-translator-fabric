package cn.islys.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class CustomConfigScreen {

    public static Screen create(Screen parent) {
        // 1. 获取当前的配置实例
        ClothConfig config = ClothConfig.get();

        // 2. 创建构建器
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("text.autoconfig.mini-chat-translator.title"));

        // 3. 获取条目构建器
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // 4. 创建配置分类（这里我们只用一个主分类）
        ConfigCategory mainCategory = builder.getOrCreateCategory(Text.translatable("text.autoconfig.mini-chat-translator.category.general"));

        // 5. 开始添加自定义条目，彻底告别 @PrefixText

        // API 设置分类 (可以用条目本身作为视觉分隔)
        mainCategory.addEntry(entryBuilder.startTextDescription(Text.literal("§6§l百度翻译 API 设置")).build());

        // APP ID 输入框
        mainCategory.addEntry(entryBuilder.startStrField(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.appId"),
                        config.getAppId()
                ).setDefaultValue("")
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.appId.@Tooltip"))
                .setSaveConsumer(config::setAppId)
                .build());

        // 密钥输入框 (这里我们用密码框)
        mainCategory.addEntry(entryBuilder.startStrField(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.secretKey"),
                        config.getSecretKey()
                ).setDefaultValue("")
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.secretKey.@Tooltip"))
                .setSaveConsumer(config::setSecretKey)
                .build());

        // 添加一个空白行作为分隔
        mainCategory.addEntry(entryBuilder.startTextDescription(Text.literal("")).build());

        // 功能设置分类
        mainCategory.addEntry(entryBuilder.startTextDescription(Text.literal("§6§l功能设置")).build());

        // 启用翻译开关
        mainCategory.addEntry(entryBuilder.startBooleanToggle(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.enabled"),
                        config.isEnabled()
                ).setDefaultValue(true)
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.enabled.@Tooltip"))
                .setSaveConsumer(config::setEnabled)
                .build());

        // 翻译自己的消息开关
        mainCategory.addEntry(entryBuilder.startBooleanToggle(
                        Text.translatable("text.autoconfig.mini-chat-translator.option.translateOwn"),
                        config.isTranslateOwn()
                ).setDefaultValue(true)
                .setTooltip(Text.translatable("text.autoconfig.mini-chat-translator.option.translateOwn.@Tooltip"))
                .setSaveConsumer(config::setTranslateOwn)
                .build());

        // 6. 设置保存回调
        builder.setSavingRunnable(() -> {
            // Cloth Config 会自动调用上面设置的 saveConsumer 来更新 config 对象
            // 但我们需要手动保存到文件
            me.shedaniel.autoconfig.AutoConfig.getConfigHolder(ClothConfig.class).save();
        });

        // 7. 返回构建好的屏幕
        return builder.build();
    }
}