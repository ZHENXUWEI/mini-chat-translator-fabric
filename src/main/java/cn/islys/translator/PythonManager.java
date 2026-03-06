package cn.islys.translator;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class PythonManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("MiniChatTranslator");

    // 存储路径
    private static final Path GAME_DIR = FabricLoader.getInstance().getGameDir();
    private static final Path PYTHON_DIR = GAME_DIR.resolve("translator/python");
    private static final Path MODEL_DIR = GAME_DIR.resolve("translator/models");

    // Python 可执行文件路径
    private static Path pythonExecutable = PYTHON_DIR.resolve(
            System.getProperty("os.name").toLowerCase().contains("win")
                    ? "python.exe" : "bin/python3"
    );

    // 翻译服务器进程
    private static Process serverProcess;
    private static int serverPort = -1;
    private static AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * 初始化 Python 环境
     */
    public static CompletableFuture<Boolean> initialize() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.info("开始初始化 Python 环境...");

                // 创建目录
                Files.createDirectories(PYTHON_DIR);
                Files.createDirectories(MODEL_DIR);
                LOGGER.info("目录创建完成: {}", PYTHON_DIR);

                // 检查 Python 是否已安装
                if (!Files.exists(pythonExecutable)) {
                    LOGGER.info("未找到嵌入式 Python，开始下载...");
                    if (!downloadEmbeddedPython()) {
                        LOGGER.error("Python 下载失败");
                        return false;
                    }
                } else {
                    LOGGER.info("Python 已存在: {}", pythonExecutable);
                }

                // 安装依赖
                LOGGER.info("开始安装 Python 依赖...");
                if (!installDependencies()) {
                    LOGGER.error("依赖安装失败");
                    return false;
                }

                // 复制 Python 脚本
                copyPythonScript();
                LOGGER.info("Python 脚本已复制");

                LOGGER.info("Python 环境初始化成功");
                return true;

            } catch (Exception e) {
                LOGGER.error("Python 环境初始化失败", e);
                return false;
            }
        });
    }

    /**
     * 下载嵌入式 Python（修复版）
     */
    private static boolean downloadEmbeddedPython() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String url;
            String zipName;

            if (os.contains("win")) {
                url = "https://www.python.org/ftp/python/3.11.9/python-3.11.9-embed-amd64.zip";
                zipName = "python-embed.zip";
            } else if (os.contains("mac")) {
                LOGGER.error("Mac 系统暂不支持自动安装，请手动安装 Python 3.11");
                return false;
            } else {
                // Linux
                url = "https://www.python.org/ftp/python/3.11.9/Python-3.11.9.tgz";
                zipName = "python.tgz";
            }

            Path zipPath = PYTHON_DIR.resolve(zipName);
            LOGGER.info("下载嵌入式 Python 从: {}", url);

            // 下载文件
            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, zipPath, StandardCopyOption.REPLACE_EXISTING);
            }

            // 解压
            LOGGER.info("解压 Python...");
            if (os.contains("win")) {
                unzipWindows(zipPath, PYTHON_DIR);
            } else {
                // Linux/Mac 需要 tar 命令
                extractTarball(zipPath, PYTHON_DIR);
            }

            // 删除压缩包
            Files.deleteIfExists(zipPath);

            // 验证 Python 可执行文件
            if (!Files.exists(pythonExecutable)) {
                LOGGER.error("Python 解压后未找到可执行文件");
                return false;
            }

            LOGGER.info("Python 安装成功: {}", pythonExecutable);
            return true;

        } catch (Exception e) {
            LOGGER.error("下载 Python 失败", e);
            return false;
        }
    }


    /**
     * Windows 解压 ZIP
     */
    private static void unzipWindows(Path zipPath, Path targetDir) throws IOException {
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
                Files.newInputStream(zipPath))) {
            java.util.zip.ZipEntry entry;
            byte[] buffer = new byte[8192];

            while ((entry = zis.getNextEntry()) != null) {
                Path outPath = targetDir.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(outPath);
                } else {
                    Files.createDirectories(outPath.getParent());
                    try (OutputStream out = Files.newOutputStream(outPath)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            out.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    /**
     * 解压 tar.gz (Linux/Mac)
     */
    private static void extractTarball(Path tarPath, Path targetDir) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "tar", "xzf", tarPath.toString(), "-C", targetDir.toString()
        );
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("解压失败，退出码: " + exitCode);
        }
    }

    /**
     * 安装依赖
     */
    private static boolean installDependencies() {
        try {
            // 复制 requirements.txt
            Path reqPath = PYTHON_DIR.resolve("requirements.txt");

            // 如果资源文件不存在，创建默认的 requirements.txt
            if (PythonManager.class.getResourceAsStream("/python/requirements.txt") == null) {
                Files.write(reqPath,
                        "transformers>=4.30.0\nsentencepiece>=0.1.99\n".getBytes());
            } else {
                try (InputStream in = PythonManager.class.getResourceAsStream("/python/requirements.txt")) {
                    Files.copy(in, reqPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            // 第一步：下载 get-pip.py
            LOGGER.info("下载 get-pip.py...");
            Path getPipPath = PYTHON_DIR.resolve("get-pip.py");

            // 使用国内镜像下载 get-pip.py
            String getPipUrl = "https://bootstrap.pypa.io/get-pip.py";
            try (InputStream in = new URL(getPipUrl).openStream()) {
                Files.copy(in, getPipPath, StandardCopyOption.REPLACE_EXISTING);
            }

            // 第二步：安装 pip
            LOGGER.info("安装 pip...");
            ProcessBuilder installPip = new ProcessBuilder(
                    pythonExecutable.toString(),
                    getPipPath.toString(),
                    "--no-warn-script-location",
                    "--target", PYTHON_DIR.resolve("Lib/site-packages").toString()  // 指定目标路径
            );
            installPip.directory(PYTHON_DIR.toFile());
            installPip.redirectErrorStream(true);

            Process pipProcess = installPip.start();

            int pipExitCode = pipProcess.waitFor();
            if (pipExitCode != 0) {
                LOGGER.error("pip 安装失败，退出码: {}", pipExitCode);
                return false;
            }

            ProcessBuilder checkPip = new ProcessBuilder(
                    pythonExecutable.toString(),
                    "-c",
                    "import sys; sys.path.append(r'" + PYTHON_DIR.resolve("Lib/site-packages").toString().replace("\\", "\\\\") + "'); import pip; print('pip location:', pip.__file__)"
            );
            checkPip.directory(PYTHON_DIR.toFile());
            checkPip.redirectErrorStream(true);

            Process checkProcess = checkPip.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(checkProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOGGER.info("[pip-check] {}", line);
                    if (line.contains("pip location:")) {
                        LOGGER.info("pip 安装成功！");
                    }
                }
            }


            // 在验证 pip 成功后，创建 sitecustomize.py 来启用 site-packages
            LOGGER.info("配置 Python 路径...");
            Path sitePackagesDir = PYTHON_DIR.resolve("Lib/site-packages");
            Path siteCustomizePath = sitePackagesDir.resolve("sitecustomize.py");

            String siteCustomizeContent =
                    "import site\n" +
                            "import sys\n" +
                            "site_packages = r'" + sitePackagesDir.toString().replace("\\", "\\\\") + "'\n" +
                            "if site_packages not in sys.path:\n" +
                            "    sys.path.insert(0, site_packages)\n" +
                            "    site.addsitedir(site_packages)\n";

            Files.write(siteCustomizePath, siteCustomizeContent.getBytes());
            LOGGER.info("sitecustomize.py 已创建");

            // 创建一个 .pth 文件来确保 pip 可用
            Path pthPath = sitePackagesDir.resolve("pip_loader.pth");
            String pthContent = sitePackagesDir.toString();
            Files.write(pthPath, pthContent.getBytes());

            // 第三步：安装依赖
            LOGGER.info("安装 Python 依赖...");
            ProcessBuilder pb = new ProcessBuilder(
                    pythonExecutable.toString(),
                    "-c",
                    "import sys; sys.path.append(r'" + sitePackagesDir.toString().replace("\\", "\\\\") + "'); " +
                            "from pip._internal.cli.main import main; sys.exit(main(['install', '--no-cache-dir', " +
                            "'-r', 'requirements.txt', '-i', 'https://pypi.tuna.tsinghua.edu.cn/simple']))"
            );
            pb.directory(PYTHON_DIR.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // 读取输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOGGER.info("[pip-install] {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                LOGGER.info("依赖安装成功");
            } else {
                LOGGER.error("依赖安装失败，退出码: {}", exitCode);
            }
            return exitCode == 0;

        } catch (Exception e) {
            LOGGER.error("安装依赖失败", e);
            return false;
        }
    }

    /**
     * 复制 Python 脚本
     */
    private static void copyPythonScript() {
        try {
            Path scriptPath = PYTHON_DIR.resolve("translator_server.py");
            try (InputStream in = PythonManager.class.getResourceAsStream("/python/translator_server.py")) {
                Files.copy(in, scriptPath, StandardCopyOption.REPLACE_EXISTING);
            }
            LOGGER.info("Python 脚本已复制到: {}", scriptPath);
        } catch (Exception e) {
            LOGGER.error("复制 Python 脚本失败", e);
        }
    }

    /**
     * 启动翻译服务器
     */
    public static CompletableFuture<Integer> startServer() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (isRunning.get()) {
                    LOGGER.info("服务器已在运行，端口: {}", serverPort);
                    return serverPort;
                }
                LOGGER.info("启动 Python 翻译服务器...");

                ProcessBuilder pb = new ProcessBuilder(
                        pythonExecutable.toString(),
                        PYTHON_DIR.resolve("translator_server.py").toString()
                        // 注意：不要传递 --model-path 参数，因为手动测试时没传也能正常工作
                );
                pb.environment().put("PYTHONPATH", PYTHON_DIR.resolve("Lib/site-packages").toString());
                pb.environment().put("PYTHONIOENCODING", "utf-8");
                pb.environment().put("PYTHONUTF8", "1");
                pb.environment().put("PYTHONUNBUFFERED", "1");  // 新增：禁用输出缓冲
                pb.directory(PYTHON_DIR.toFile());

                // 合并错误流到标准输出
                pb.redirectErrorStream(true);

                // 设置环境变量，确保 Python 输出使用 UTF-8
                pb.environment().put("PYTHONIOENCODING", "utf-8");
                pb.environment().put("PYTHONUTF8", "1");


                LOGGER.info("执行命令: {}", String.join(" ", pb.command()));

                serverProcess = pb.start();
                LOGGER.info("Python 进程已启动，PID: {}", serverProcess.pid());

                // 读取输出，寻找 SERVER_PORT
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(serverProcess.getInputStream(), "UTF-8"))) {

                    String line;
                    long startTime = System.currentTimeMillis();
                    long timeout = 120000; // 120秒超时

                    while ((line = reader.readLine()) != null) {
                        LOGGER.info("[Python] {}", line);
                        output.append(line).append("\n");

                        // 检查是否包含端口信息
                        if (line.contains("SERVER_PORT:")) {
                            String portStr = line.substring(line.indexOf("SERVER_PORT:") + 12).trim();
                            try {
                                serverPort = Integer.parseInt(portStr);
                                isRunning.set(true);
                                LOGGER.info("✅ 翻译服务器已启动，端口: {}", serverPort);

                                // 启动监控线程后立即返回
                                startOutputMonitor();
                                return serverPort;
                            } catch (NumberFormatException e) {
                                LOGGER.error("无法解析端口号: {}", portStr);
                            }
                        }

                        // 检查是否启动失败
                        if (line.contains("[失败]") || line.contains("Error") || line.contains("Exception")) {
                            LOGGER.error("[Python错误] {}", line);
                        }

                        // 超时检查
                        if (System.currentTimeMillis() - startTime > timeout) {
                            LOGGER.error("启动超时 ({} 毫秒)", timeout);
                            serverProcess.destroy();
                            break;
                        }
                    }
                }

                // 如果没找到端口信息，打印完整输出
                LOGGER.error("未收到服务器端口信息，完整输出:\n{}", output.toString());
                return -1;

            } catch (Exception e) {
                LOGGER.error("启动翻译服务器失败", e);
                return -1;
            }
        });
    }

    /**
     * 监控进程输出
     */
    private static void startOutputMonitor() {
        Thread monitor = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(serverProcess.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOGGER.info("[Python] {}", line);
                }
            } catch (Exception e) {
                LOGGER.error("监控线程错误", e);
            } finally {
                LOGGER.info("Python 进程输出流已关闭");
            }
        });
        monitor.setDaemon(true);
        monitor.start();

        // 也监控错误流（虽然已经合并，但保险起见）
        Thread errorMonitor = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(serverProcess.getErrorStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOGGER.error("[Python错误] {}", line);
                }
            } catch (Exception e) {
                // 忽略
            }
        });
        errorMonitor.setDaemon(true);
        errorMonitor.start();
    }

    /**
     * 停止服务器
     */
    public static void stopServer() {
        if (serverProcess != null) {
            serverProcess.destroy();
            isRunning.set(false);
            serverPort = -1;
            LOGGER.info("翻译服务器已停止");
        }
    }

    /**
     * 检查服务器是否运行
     */
    public static boolean isServerRunning() {
        return isRunning.get() && serverProcess != null && serverProcess.isAlive();
    }

    /**
     * 获取服务器端口
     */
    public static int getServerPort() {
        return serverPort;
    }
}