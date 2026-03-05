package cn.islys;

// import cn.islys.config.ModConfig;
import cn.islys.config.ClothConfig;
import cn.islys.util.Translator;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class MiniChatTranslatorClient implements ClientModInitializer {
    public static final String MOD_ID = "mini-chat-translator";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // 新增：标记位，防止递归
    private static boolean isSendingTranslated = false;

    // 新增：预编译正则表达式，提高性能
    private static final Pattern CHAT_MESSAGE_PATTERN = Pattern.compile(
            "(?:\\[.*?\\]\\s*)*?(?:<.*?>\\s*)?([a-zA-Z0-9_]+)?:\\s+(.*)$"
    );

    @Override
    public void onInitializeClient() {
        LOGGER.info("聊天翻译模组已加载！");
        ClothConfig.init();

        ClothConfig config = ClothConfig.get();
        LOGGER.info("初始配置 - enabled: {}, translateOwn: {}, chineseToEnglish: {}",
                config.isEnabled(), config.isTranslateOwn(), config.isChineseToEnglish());

        // 1. 监听收到的系统消息（ALLOW_GAME）
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            LOGGER.info("========== ALLOW_GAME 事件触发（系统消息） ==========");
            LOGGER.info("消息内容: {}", message.getString());
            LOGGER.info("消息类: {}", message.getClass().getName());

            ClothConfig config2 = ClothConfig.get();
            if (config2.isEnabled()) {
                processReceivedMessage(message, "GAME");
            }
            return true;
        });

        // 2. 监听收到的玩家消息（ALLOW_CHAT）
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            LOGGER.info("========== ALLOW_CHAT 事件触发（收到玩家消息） ==========");
            LOGGER.info("消息内容: {}", message.getString());
            LOGGER.info("发送者: {}", sender);

            ClothConfig config2 = ClothConfig.get();
            if (config2.isEnabled()) {
                processReceivedMessage(message, "CHAT");
            }
            return true;
        });

        // 3. 监听自己发送的消息（中译英）
        ClientSendMessageEvents.ALLOW_CHAT.register((message) -> {
            LOGGER.info("========== ALLOW_CHAT 事件触发（发送消息） ==========");
            LOGGER.info("原始消息: {}", message);

            if (isSendingTranslated) {
                LOGGER.info("是翻译后的消息，直接放行");
                return true;
            }

            ClothConfig config3 = ClothConfig.get();
            if (config3.isEnabled() && config3.isChineseToEnglish()) {
                LOGGER.info("开始处理发送消息");
                processSendingMessage(message);
                return false;
            }
            return true;
        });
    }

    /**
     * 处理收到的消息（英译中）
     * @param message 收到的消息
     * @param source 消息来源 ("GAME" 或 "CHAT")
     */
    private void processReceivedMessage(Text message, String source) {
        LOGGER.info(">>> processReceivedMessage 被调用！来源: {}", source);
        String originalText = message.getString();
        LOGGER.info("收到原始消息: {}", originalText);

        ClothConfig config = ClothConfig.get();
        LOGGER.info("翻译开关: {}, 英译中配置: {}", config.isEnabled(), config.isTranslateOwn());

        // 1. 先检查总开关
        if (!config.isEnabled()) {
            LOGGER.info("翻译功能已关闭");
            return;
        }

        // 2. 检查是否启用收到的消息翻译
        if (!config.isTranslateOwn()) {
            LOGGER.info("收到的消息翻译已关闭");
            return;
        }

        // 3. 尝试提取纯消息内容
        String cleanMessage = extractChatMessage(originalText);
        LOGGER.info("提取后的消息: {}", cleanMessage);

        // 4. 检查提取后的消息是否包含中文
        if (containsChinese(cleanMessage)) {
            LOGGER.info("消息包含中文，不翻译");
            return;
        }

        LOGGER.info("开始翻译: {}", cleanMessage);
        Translator.translateEnToZhAsync(cleanMessage).thenAcceptAsync(translatedText -> {
            if (isTranslationValid(cleanMessage, translatedText)) {
                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                        Text.literal("§7[英→中] " + translatedText)
                );
                LOGGER.info("翻译完成: {}", translatedText);
            } else {
                LOGGER.warn("翻译结果无效: {}", translatedText);
            }
        }, MinecraftClient.getInstance()::execute).exceptionally(throwable -> {
            LOGGER.error("翻译异常", throwable);
            return null;
        });
    }

    /**
     * 处理发送的消息（中译英）
     */
    private void processSendingMessage(String message) {
        if (!containsChinese(message)) {
            sendTranslatedMessage(message);
            return;
        }

        Translator.translateZhToEnAsync(message).thenAcceptAsync(translatedText -> {
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

    /**
     * 新增：专门用于发送翻译后的消息
     */
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

    /**
     * 新增：从服务器格式化消息中提取纯聊天内容
     * 格式示例: [Italy|Calvi] Koyaan: u got played
     * 或: [NC] [Ghana] 练习近身平砍 anyuanqwq: zen me le
     */
    private String extractChatMessage(String formattedMessage) {
        Matcher matcher = CHAT_MESSAGE_PATTERN.matcher(formattedMessage);

        if (matcher.find()) {
            // group(2) 是冒号后面的消息内容
            String extracted = matcher.group(2);
            if (extracted != null && !extracted.isEmpty()) {
                return extracted.trim();
            }
        }

        // 备用方案：如果正则匹配失败，尝试找最后一个冒号
        int colonIndex = formattedMessage.lastIndexOf(": ");
        if (colonIndex != -1 && colonIndex < formattedMessage.length() - 2) {
            return formattedMessage.substring(colonIndex + 2).trim();
        }

        // 如果都失败，返回原消息
        LOGGER.warn("消息提取失败，使用原消息: {}", formattedMessage);
        return formattedMessage;
    }

    private boolean containsChinese(String str) {
        for (char c : str.toCharArray()) {
            Character.UnicodeScript sc = Character.UnicodeScript.of(c);
            if (sc == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }

    private boolean isTranslationValid(String original, String translated) {
        if (translated == null || translated.isEmpty()) return false;
        if (translated.contains("DaSheepa") || translated.equals("达西帕")) return false;
        if (translated.equals(original)) return false;
        if (translated.matches("[\\p{Punct}\\d\\s]+")) return false;
        return true;
    }
}