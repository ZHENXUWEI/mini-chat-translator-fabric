package cn.islys.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CustomConfigScreen {

    public static Screen create(Screen parent) {
        ModConfig config = ModConfig.get();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("§6§l迷你聊天翻译配置"))
                // ===== 通用设置分类 =====
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("§a⚙️ 通用设置"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("启用翻译"))
                                .description(OptionDescription.of(Component.literal("开启或关闭所有翻译功能")))
                                .binding(true, () -> config.enabled, val -> { config.enabled = val; ModConfig.save(); })
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<TranslationEngine>createBuilder()
                                .name(Component.literal("翻译引擎"))
                                .description(OptionDescription.of(Component.literal("选择使用的翻译服务")))
                                .binding(TranslationEngine.LOCAL, () -> config.translationEngine, val -> {
                                    config.translationEngine = val;
                                    ModConfig.save();
                                })
                                .controller(opt -> EnumControllerBuilder.create(opt)
                                        .enumClass(TranslationEngine.class)
                                        .valueFormatter(engine -> {
                                            switch (engine) {
                                                case LOCAL: return Component.literal("🌐 本地模型 (离线免费)");
                                                case BAIDU: return Component.literal("🔵 百度翻译 (在线)");
                                                case GOOGLE_FREE: return Component.literal("🟢 谷歌翻译 (免费版)");
                                                case GOOGLE_OFFICIAL: return Component.literal("🔴 谷歌翻译 (官方API)");
                                                default: return Component.literal(engine.name());
                                            }
                                        }))
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("中译英发送"))
                                .description(OptionDescription.of(Component.literal("将你要发送的中文消息翻译成英文后发出")))
                                .binding(true, () -> config.chineseToEnglish, val -> { config.chineseToEnglish = val; ModConfig.save(); })
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("翻译自己的消息"))
                                .description(OptionDescription.of(Component.literal("是否翻译自己收到的其他玩家消息")))
                                .binding(true, () -> config.translateOwn, val -> { config.translateOwn = val; ModConfig.save(); })
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .build())
                // ===== 百度翻译配置分类 =====
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("🔵 百度翻译配置"))
                        .option(Option.<String>createBuilder()
                                .name(Component.literal("APP ID"))
                                .description(OptionDescription.of(Component.literal("在百度翻译开放平台申请的 APP ID")))
                                .binding("", () -> config.baiduAppId, val -> { config.baiduAppId = val; ModConfig.save(); })
                                .controller(StringControllerBuilder::create)
                                .build())
                        .option(Option.<String>createBuilder()
                                .name(Component.literal("密钥"))
                                .description(OptionDescription.of(Component.literal("在百度翻译开放平台申请的密钥")))
                                .binding("", () -> config.baiduSecretKey, val -> { config.baiduSecretKey = val; ModConfig.save(); })
                                .controller(StringControllerBuilder::create)
                                .build())
                        .build())
                // ===== 谷歌翻译配置分类 =====
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("🟢 谷歌翻译配置"))
                        .option(Option.<String>createBuilder()
                                .name(Component.literal("官方 API 密钥"))
                                .description(OptionDescription.of(Component.literal("从 Google Cloud 控制台获取的 API 密钥\n（免费版无需配置）")))
                                .binding("", () -> config.googleApiKey, val -> { config.googleApiKey = val; ModConfig.save(); })
                                .controller(StringControllerBuilder::create)
                                .build())
                        .build())
                .build()
                .generateScreen(parent);
    }
}