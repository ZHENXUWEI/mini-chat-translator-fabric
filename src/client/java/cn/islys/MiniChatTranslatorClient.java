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

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            processAndTranslateAsync(message);  // 改成异步版本
            return true;
        });

        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            processAndTranslateAsync(message);  // 改成异步版本
            return true;
        });
    }

    // 新增：异步处理方法
    private void processAndTranslateAsync(Text message) {
        String originalText = message.getString();

        // 简单判断是否包含中文
        if (containsChinese(originalText)) {
            return;
        }

        // 在后台线程翻译，完成后回到主线程显示
        Translator.translateAsync(originalText).thenAcceptAsync(translatedText -> {
            // 这个回调会在主线程执行（因为 MinecraftClient.getInstance() 需要主线程）
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                    Text.literal("§7[翻译] " + translatedText)
            );
        }, MinecraftClient.getInstance()::execute);  // 使用 Minecraft 的 execute 方法回到主线程
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
