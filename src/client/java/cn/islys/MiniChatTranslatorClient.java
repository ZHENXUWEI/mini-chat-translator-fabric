package cn.islys;

// import cn.islys.config.ModConfig;
import cn.islys.config.ClothConfig;
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

        // 初始化配置
        ClothConfig.init();

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (ClothConfig.get().isEnabled()) {
                processAndTranslateAsync(message);
            }
            return true;
        });

        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            if (ClothConfig.get().isEnabled() && ClothConfig.get().isTranslateOwn()) {
                processAndTranslateAsync(message);
            }
            return true;
        });
    }

    private void processAndTranslateAsync(Text message) {
        String originalText = message.getString();

        if (containsChinese(originalText)) {
            return;
        }

        Translator.translateAsync(originalText).thenAcceptAsync(translatedText -> {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                    Text.literal("§7[翻译] " + translatedText)
            );
        }, MinecraftClient.getInstance()::execute);
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
