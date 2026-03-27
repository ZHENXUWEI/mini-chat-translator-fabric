package cn.islys;

import cn.islys.config.ModConfig;
import cn.islys.config.TranslationEngine;
import cn.islys.translator.PythonManager;
import cn.islys.translator.TranslationServer;
import cn.islys.util.GoogleTranslator;
import cn.islys.util.Translator;
import net.minecraft.client.Minecraft;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiniChatTranslatorClient implements ClientModInitializer {
    public static final String MOD_ID = "mini-chat-translator";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static boolean isSendingTranslated = false;
    private static final Pattern CHAT_MESSAGE_PATTERN = Pattern.compile(
            "(?:\\[.*?\\]\\s*)*?(?:<.*?>\\s*)?([a-zA-Z0-9_]+)?:\\s+(.*)$"
    );

    @Override
    public void onInitializeClient() {
        ModConfig.load();
        LOGGER.info("聊天翻译模组已加载！");

        ModConfig config = ModConfig.get();
        LOGGER.info("初始配置 - 翻译引擎: {}",
                config.translationEngine);

        if (config.translationEngine == TranslationEngine.LOCAL) {
            initializeLocalTranslator();
        }

        setupMessageListeners();
    }

    private void initializeLocalTranslator() {
        // 发送首次使用提示（使用 player.sendSystemMessage）
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(
                    Component.literal("§e[翻译] 如果第一次使用本地翻译，需要下载模型文件")
            );
            Minecraft.getInstance().player.sendSystemMessage(
                    Component.literal("§e[翻译] 下载期间您可以继续游戏，模型加载完成后会自动启用")
            );
            Minecraft.getInstance().player.sendSystemMessage(
                    Component.literal("§4[翻译] 请耐心等待本地翻译模型启动")
            );
        }

        PythonManager.initialize().thenAccept(success -> {
            if (success) {
                PythonManager.startServer().thenAccept(port -> {
                    if (port > 0) {
                        TranslationServer.setPort(port);
                        LOGGER.info("✅ 本地翻译服务器已启动，端口: {}", port);

                        Minecraft.getInstance().execute(() -> {
                            if (Minecraft.getInstance().player != null) {
                                Minecraft.getInstance().player.sendSystemMessage(
                                        Component.literal("§a[翻译] 本地翻译服务器已就绪")
                                );
                            }
                        });
                    }
                });
            } else {
                LOGGER.error("❌ Python环境初始化失败");
                Minecraft.getInstance().execute(() -> {
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.sendSystemMessage(
                                Component.literal("§c[翻译] 本地翻译初始化失败，请查看日志")
                        );
                    }
                });
            }
        });
    }

    private void setupMessageListeners() {
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (ModConfig.get().enabled) {
                processReceivedMessage(message, "GAME");
            }
            return true;
        });

        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            if (ModConfig.get().enabled) {
                processReceivedMessage(message, "CHAT");
            }
            return true;
        });

        ClientSendMessageEvents.ALLOW_CHAT.register((message) -> {
            if (isSendingTranslated) return true;

            ModConfig config = ModConfig.get();
            if (config.enabled && config.chineseToEnglish) {
                processSendingMessage(message);
                return false;
            }
            return true;
        });
    }

    private void processReceivedMessage(Component message, String source) {
        String originalText = message.getString();
        String cleanMessage = extractChatMessage(originalText);

        if (!ModConfig.get().translateOwn || containsChinese(cleanMessage)) {
            return;
        }

        translateAsync(cleanMessage, "auto", "zh")
                .thenAcceptAsync(translatedText -> {
                    if (isTranslationValid(cleanMessage, translatedText)) {
                        if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().player.sendSystemMessage(
                                    Component.literal("§7[翻译] " + translatedText)
                            );
                        }
                    }
                }, Minecraft.getInstance()::execute);
    }

    private void processSendingMessage(String message) {
        if (!containsChinese(message)) {
            sendTranslatedMessage(message);
            return;
        }

        translateAsync(message, "zh", "en")
                .thenAcceptAsync(translatedText -> {
                    if (isTranslationValid(message, translatedText)) {
                        sendTranslatedMessage(translatedText);
                        if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().player.sendSystemMessage(
                                    Component.literal("§7[中→英] " + translatedText)
                            );
                        }
                    } else {
                        sendTranslatedMessage(message);
                    }
                }, Minecraft.getInstance()::execute);
    }

    private CompletableFuture<String> translateAsync(String text, String from, String to) {
        ModConfig config = ModConfig.get();

        switch (config.translationEngine) {
            case LOCAL:
                if (PythonManager.isServerRunning()) {
                    return TranslationServer.translate(text, from, to);
                } else {
                    LOGGER.warn("本地翻译服务器未运行，回退到百度翻译");
                    return Translator.translateAsync(text, from, to);
                }
            case BAIDU:
                return Translator.translateAsync(text, from, to);
            case GOOGLE_FREE:
                return GoogleTranslator.translateFree(text, to);
            case GOOGLE_OFFICIAL:
                return GoogleTranslator.translateOfficial(text, to, config.googleApiKey);
            default:
                return CompletableFuture.completedFuture("[未选择翻译引擎]");
        }
    }

    private void sendTranslatedMessage(String message) {
        isSendingTranslated = true;
        try {
            if (Minecraft.getInstance().getConnection() != null) {
                Minecraft.getInstance().getConnection().sendChat(message);
            }
        } finally {
            isSendingTranslated = false;
        }
    }

    private String extractChatMessage(String formattedMessage) {
        Matcher matcher = CHAT_MESSAGE_PATTERN.matcher(formattedMessage);
        if (matcher.find()) {
            String extracted = matcher.group(2);
            if (extracted != null && !extracted.isEmpty()) {
                return extracted.trim();
            }
        }
        int colonIndex = formattedMessage.lastIndexOf(": ");
        if (colonIndex != -1 && colonIndex < formattedMessage.length() - 2) {
            return formattedMessage.substring(colonIndex + 2).trim();
        }
        return formattedMessage;
    }

    private boolean containsChinese(String str) {
        for (char c : str.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }

    private boolean isTranslationValid(String original, String translated) {
        if (translated == null || translated.isEmpty()) return false;
        if (translated.contains("DaSheepa") || translated.equals("达西帕")) return false;
        if (translated.equals(original)) return false;
        return !translated.matches("[\\p{Punct}\\d\\s]+");
    }
}