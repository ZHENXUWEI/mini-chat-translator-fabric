package cn.islys.util;

import cn.islys.config.ClothConfig;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;

public class LocalModelManager {
    private static final Path DEFAULT_MODEL_DIR = Paths.get("translations/m2m100");
    private static final String MODEL_URL = "https://mirrors.tuna.tsinghua.edu.cn/hugging-face-models/facebook/m2m100_418M/pytorch_model.bin";
    private static boolean isDownloading = false;
    private static int downloadProgress = 0;

    public static boolean modelExists() {
        ClothConfig config = ClothConfig.get();
        Path modelPath = config.getLocalModelPath().isEmpty() ?
                DEFAULT_MODEL_DIR : Paths.get(config.getLocalModelPath());

        return Files.exists(modelPath.resolve("model.pt")) &&
                Files.exists(modelPath.resolve("vocab.txt"));
    }

    public static void startDownload() {
        if (isDownloading) return;

        isDownloading = true;
        CompletableFuture.runAsync(() -> {
            try {
                // 通知玩家
                notifyPlayer("§e[翻译模组] 开始下载翻译模型（约470MB）...");

                Path tempFile = Files.createTempFile("model", ".zip");
                URL url = new URL(MODEL_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "Minecraft");
                connection.connect();

                int fileSize = connection.getContentLength();
                InputStream in = connection.getInputStream();

                // 带进度的下载
                try (FileOutputStream out = new FileOutputStream(tempFile.toFile())) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalBytes = 0;

                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        totalBytes += bytesRead;
                        if (fileSize > 0) {
                            downloadProgress = (int) (totalBytes * 100 / fileSize);
                            if (downloadProgress % 10 == 0) {
                                notifyPlayer("§e下载进度: " + downloadProgress + "%");
                            }
                        }
                    }
                }

                // 解压
                notifyPlayer("§e正在解压模型文件...");
                ClothConfig config = ClothConfig.get();
                Path modelPath = config.getLocalModelPath().isEmpty() ?
                        DEFAULT_MODEL_DIR : Paths.get(config.getLocalModelPath());
                Files.createDirectories(modelPath);

                unzip(tempFile, modelPath);
                Files.delete(tempFile);

                notifyPlayer("§a翻译模型下载完成！");

            } catch (Exception e) {
                e.printStackTrace();
                notifyPlayer("§c模型下载失败: " + e.getMessage());
            } finally {
                isDownloading = false;
            }
        });
    }

    private static void unzip(Path zipFile, Path targetDir) throws IOException {
        try (java.util.zip.ZipInputStream zis =
                     new java.util.zip.ZipInputStream(Files.newInputStream(zipFile))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path outPath = targetDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(outPath);
                } else {
                    Files.createDirectories(outPath.getParent());
                    Files.copy(zis, outPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    private static void notifyPlayer(String message) {
        net.minecraft.client.MinecraftClient.getInstance().execute(() -> {
            net.minecraft.client.MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                    net.minecraft.text.Text.literal(message)
            );
        });
    }

    public static String translate(String text, String from, String to) {
        // 实际调用本地模型进行翻译
        // 这里需要集成 DJL 或其他 Java 深度学习库
        return "[本地模型翻译实现]";
    }

    public static boolean isDownloading() { return isDownloading; }
    public static int getDownloadProgress() { return downloadProgress; }
}