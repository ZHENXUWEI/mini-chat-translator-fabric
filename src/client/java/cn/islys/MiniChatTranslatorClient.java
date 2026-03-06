package cn.islys;

import cn.islys.config.ClothConfig;
import cn.islys.config.TranslationEngine;
import cn.islys.translator.PythonManager;
import cn.islys.translator.TranslationServer;
import cn.islys.util.Translator;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        ClothConfig.init();
        LOGGER.info("聊天翻译模组已加载！");

        ClothConfig config = ClothConfig.get();
        LOGGER.info("初始配置 - 翻译引擎: {}, 自动下载模型: {}",
                config.getTranslationEngine(), config.isAutoDownloadModel());

        // 如果启用本地翻译，初始化 Python 环境
        if (config.getTranslationEngine() == TranslationEngine.LOCAL && config.isAutoDownloadModel()) {
            initializeLocalTranslator();
        }

        setupMessageListeners();
    }

    private void initializeLocalTranslator() {
        LOGGER.info("开始初始化本地翻译...");

        PythonManager.initialize().thenAccept(success -> {
            if (success) {
                LOGGER.info("Python环境初始化成功，启动服务器...");
                PythonManager.startServer().thenAccept(port -> {
                    if (port > 0) {
                        TranslationServer.setPort(port);
                        LOGGER.info("✅ 本地翻译服务器已启动，端口: {}", port);

                        MinecraftClient.getInstance().execute(() -> {
                            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                                    Text.literal("§a[翻译] 本地翻译服务器已就绪")
                            );
                        });
                    } else {
                        LOGGER.error("❌ 服务器启动失败");
                    }
                });
            } else {
                LOGGER.error("❌ Python环境初始化失败");
                MinecraftClient.getInstance().execute(() -> {
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                            Text.literal("§c[翻译] 本地翻译初始化失败，请查看日志")
                    );
                });
            }
        });
    }

    private void setupMessageListeners() {
        // 系统消息
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (ClothConfig.get().isEnabled()) {
                processReceivedMessage(message, "GAME");
            }
            return true;
        });

        // 玩家消息
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            if (ClothConfig.get().isEnabled()) {
                processReceivedMessage(message, "CHAT");
            }
            return true;
        });

        // 发送消息
        ClientSendMessageEvents.ALLOW_CHAT.register((message) -> {
            if (isSendingTranslated) return true;

            ClothConfig config = ClothConfig.get();
            if (config.isEnabled() && config.isChineseToEnglish()) {
                processSendingMessage(message);
                return false;
            }
            return true;
        });
    }

    private void processReceivedMessage(Text message, String source) {
        String originalText = message.getString();
        String cleanMessage = extractChatMessage(originalText);

        if (!ClothConfig.get().isTranslateOwn() || containsChinese(cleanMessage)) {
            return;
        }

        translateAsync(cleanMessage, "auto", "zh")
                .thenAcceptAsync(translatedText -> {
                    if (isTranslationValid(cleanMessage, translatedText)) {
                        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                                Text.literal("§7[翻译] " + translatedText)
                        );
                    }
                }, MinecraftClient.getInstance()::execute);
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
                        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                                Text.literal("§7[中→英] " + translatedText)
                        );
                    } else {
                        sendTranslatedMessage(message);
                    }
                }, MinecraftClient.getInstance()::execute);
    }

    private CompletableFuture<String> translateAsync(String text, String from, String to) {
        ClothConfig config = ClothConfig.get();

        // 根据选择的翻译引擎决定使用哪个翻译服务
        switch (config.getTranslationEngine()) {
            case LOCAL:
                if (PythonManager.isServerRunning()) {
                    return TranslationServer.translate(text, from, to);
                } else {
                    LOGGER.warn("本地翻译服务器未运行，回退到百度翻译");
                    return Translator.translateAsync(text, from, to);
                }
            case TENCENT:
                // TODO: 实现腾讯翻译
                return CompletableFuture.completedFuture("[腾讯翻译开发中]");
            case ALIYUN:
                // TODO: 实现阿里翻译
                return CompletableFuture.completedFuture("[阿里翻译开发中]");
            case BAIDU:
            default:
                return Translator.translateAsync(text, from, to);
        }
    }

    private void sendTranslatedMessage(String message) {
        isSendingTranslated = true;
        try {
            if (MinecraftClient.getInstance().getNetworkHandler() != null) {
                MinecraftClient.getInstance().getNetworkHandler().sendChatMessage(message);
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