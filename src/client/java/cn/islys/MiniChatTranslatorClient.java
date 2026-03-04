package cn.islys;

import cn.islys.config.ModConfig;
import cn.islys.util.Translator;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MiniChatTranslatorClient implements ClientModInitializer {
    public static final String MOD_ID = "mini-chat-translator";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("聊天翻译模组已加载！");

        // 加载配置文件
        ModConfig.load();

        // 1. 监听来自服务器的消息（其他玩家发的）
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            processAndTranslate(message);
            return true;
        });

        // 2. 监听自己发送的消息
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            processAndTranslate(message);
            return true;
        });
    }

    private void processAndTranslate(Text message) {
        String originalText = message.getString();

        // 简单判断是否包含中文（如果已经是中文就不翻译）
        if (containsChinese(originalText)) {
            return;
        }

        // 调用百度翻译 API 获取真正的翻译结果
        String translatedText = Translator.translate(originalText);

        // 如果翻译失败，translatedText 会返回错误信息
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                Text.literal("§7[翻译] " + translatedText)
        );
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
}
