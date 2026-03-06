#!/usr/bin/env python3
"""
LiveTranslator 风格的翻译服务器
基于 m2m100 模型，提供本地翻译服务
"""
import os
import sys
import json
import socket
import argparse
import subprocess
import urllib.request
import urllib.parse
from http.server import HTTPServer, BaseHTTPRequestHandler
from pathlib import Path
import threading
import time

if sys.platform == 'win32':
    sys.stdout.reconfigure(encoding='utf-8')
    sys.stderr.reconfigure(encoding='utf-8')

# 添加 site-packages 到系统路径
script_dir = Path(__file__).parent
site_packages = script_dir / "Lib" / "site-packages"
if site_packages.exists():
    sys.path.insert(0, str(site_packages))
    print(f"添加路径: {site_packages}")

# 修改点1：先检查依赖，需要时才安装
def ensure_dependencies():
    """确保所有依赖都已安装，如果缺失则自动安装"""
    required_packages = [
        ("transformers", "transformers"),
        ("torch", "torch"),
        ("sentencepiece", "sentencepiece")
    ]

    missing_packages = []
    for package_name, install_name in required_packages:
        try:
            __import__(package_name)
            print(f"[OK] 依赖已存在: {package_name}")
        except ImportError:
            print(f"[失败] 缺少依赖: {package_name}")
            missing_packages.append(install_name)

    if missing_packages:
        print(f"正在安装缺失的依赖: {', '.join(missing_packages)}...")
        try:
            # 使用 --no-cache-dir 避免缓存问题
            cmd = [sys.executable, "-m", "pip", "install", "--no-cache-dir"] + missing_packages
            subprocess.check_call(cmd)
            print("[OK] 依赖安装成功！")
        except subprocess.CalledProcessError as e:
            print(f"[失败] 依赖安装失败: {e}")
            return False

    return True

# 执行依赖检查
if not ensure_dependencies():
    print("无法安装必要依赖，服务器退出")
    sys.exit(1)

# 现在可以安全导入
from transformers import M2M100ForConditionalGeneration, M2M100Tokenizer
import torch

class TranslationHandler(BaseHTTPRequestHandler):
    """处理翻译请求"""

    def do_POST(self):
        content_length = int(self.headers['Content-Length'])
        post_data = self.rfile.read(content_length)
        request = json.loads(post_data.decode('utf-8'))

        text = request.get('text', '')
        source_lang = request.get('source', 'auto')
        target_lang = request.get('target', 'zh')

        try:
            result = self.server.translator.translate(text, source_lang, target_lang)
            response = {'success': True, 'result': result}
        except Exception as e:
            response = {'success': False, 'error': str(e)}

        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.end_headers()
        self.wfile.write(json.dumps(response).encode('utf-8'))

    def log_message(self, format, *args):
        pass  # 禁用日志

class Translator:
    """翻译器，负责加载模型和执行翻译"""

    def __init__(self, model_path=None):
        self.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
        print(f"使用设备: {self.device}")

        # 确定模型路径
        if model_path is None:
            # 修改点2：使用游戏目录下的 models 文件夹
            # 获取当前脚本所在目录的上一级（python目录），再上一级就是 translator 目录
            current_dir = Path(__file__).parent
            self.model_path = current_dir.parent / "models"
        else:
            self.model_path = Path(model_path)

        self.model_path.mkdir(parents=True, exist_ok=True)
        print(f"模型存储路径: {self.model_path}")

        # 加载或下载模型
        self._load_model()

    def _load_model(self):
        """加载模型，如果不存在则下载"""
        import os

        # 将 Path 对象转换为字符串，并确保使用原始字符串
        model_path_str = str(self.model_path.absolute())
        print(f"检查模型文件: {model_path_str}")

        # 检查模型文件是否存在
        model_file = self.model_path / 'pytorch_model.bin'
        tokenizer_file = self.model_path / 'tokenizer_config.json'
        sp_model_file = self.model_path / 'sentencepiece.bpe.model'

        if not model_file.exists():
            print(f"模型文件不存在: {model_file}")
        if not tokenizer_file.exists():
            print(f"分词器配置文件不存在: {tokenizer_file}")
        if not sp_model_file.exists():
            print(f"SentencePiece模型不存在: {sp_model_file}")

        if not model_file.exists() or not tokenizer_file.exists() or not sp_model_file.exists():
            print("模型文件不完整，开始下载...")
            self._download_model()
        else:
            print("模型文件已存在，跳过下载")

        print("正在加载模型到内存...")
        try:
            # 使用原始字符串格式加载
            self.tokenizer = M2M100Tokenizer.from_pretrained(model_path_str)
            self.model = M2M100ForConditionalGeneration.from_pretrained(
                model_path_str,
                torch_dtype=torch.float16 if self.device.type == 'cuda' else torch.float32
            ).to(self.device)
            self.model.eval()
            print("[OK] 模型加载完成！")
        except Exception as e:
            print(f"模型加载失败: {e}")
            import traceback
            traceback.print_exc()
            raise

    def _download_model(self):
        """从 Hugging Face 镜像下载模型"""
        import urllib.request
        from pathlib import Path
        import time

        # Hugging Face 镜像站
        base_url = "https://hf-mirror.com/facebook/m2m100_418M/resolve/main"

        # 正确的文件名列表
        files = [
            ("pytorch_model.bin", "模型权重文件 (约1.94GB)"),
            ("config.json", "配置文件"),
            ("tokenizer_config.json", "分词器配置文件"),  # 注意这里是 tokenizer_config.json 不是 tokenizer.json
            ("vocab.json", "词汇表"),
            ("sentencepiece.bpe.model", "SentencePiece模型"),
            ("special_tokens_map.json", "特殊标记映射"),  # 可选，但有时需要
            ("generation_config.json", "生成配置")        # 可选
        ]

        total_files = len(files)
        for index, (file, description) in enumerate(files, 1):
            url = f"{base_url}/{file}"
            dest = self.model_path / file

            if dest.exists():
                print(f"[{index}/{total_files}] 文件已存在，跳过: {file}")
                continue

            print(f"[{index}/{total_files}] 正在下载 {description}: {file}...")
            max_retries = 3
            for attempt in range(max_retries):
                try:
                    req = urllib.request.Request(url, headers={
                        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
                        'Referer': 'https://hf-mirror.com/'
                    })
                    with urllib.request.urlopen(req, timeout=30) as response:
                        with open(dest, 'wb') as out_file:
                            file_size = int(response.headers.get('Content-Length', 0))
                            downloaded = 0
                            chunk_size = 8192

                            while True:
                                chunk = response.read(chunk_size)
                                if not chunk:
                                    break
                                out_file.write(chunk)
                                downloaded += len(chunk)
                                if file_size > 0:
                                    percent = int(downloaded * 100 / file_size)
                                    if percent % 10 == 0:
                                        print(f"   进度: {percent}%")

                    print(f"[OK] 下载完成: {file}")
                    break

                except urllib.error.HTTPError as e:
                    if e.code == 404:
                        print(f"[警告] 文件 {file} 不存在，跳过")
                        break
                    elif attempt < max_retries - 1:
                        print(f"[警告] 下载失败，{attempt + 1}秒后重试... ({attempt + 2}/{max_retries})")
                        time.sleep(attempt + 1)
                    else:
                        print(f"[失败] 下载失败 {file}: {e}")
                        raise
                except Exception as e:
                    if attempt < max_retries - 1:
                        print(f"[警告] 下载失败，{attempt + 1}秒后重试... ({attempt + 2}/{max_retries})")
                        time.sleep(attempt + 1)
                    else:
                        print(f"[失败] 下载失败 {file}: {e}")
                        raise

        print("🎉 所有模型文件下载完成！")

    def translate(self, text, source_lang='auto', target_lang='zh'):
        """执行翻译"""
        if not text:
            return ""

        # 语言代码映射
        lang_map = {
            'en': 'en', 'zh': 'zh', 'ja': 'ja', 'ko': 'ko',
            'fr': 'fr', 'de': 'de', 'es': 'es', 'ru': 'ru',
            'ar': 'ar', 'it': 'it', 'pt': 'pt', 'nl': 'nl',
            'pl': 'pl', 'tr': 'tr', 'vi': 'vi', 'th': 'th'
        }

        src = lang_map.get(source_lang, source_lang)
        tgt = lang_map.get(target_lang, target_lang)

        try:
            # 编码输入
            self.tokenizer.src_lang = src if src != 'auto' else 'en'
            inputs = self.tokenizer(text, return_tensors="pt", truncation=True, max_length=512).to(self.device)

            # 生成翻译
            with torch.no_grad():
                generated_tokens = self.model.generate(
                    **inputs,
                    forced_bos_token_id=self.tokenizer.get_lang_id(tgt),
                    max_length=512,
                    num_beams=4,
                    early_stopping=True
                )

            # 解码结果
            result = self.tokenizer.decode(generated_tokens[0], skip_special_tokens=True)
            return result

        except Exception as e:
            print(f"翻译出错: {e}")
            return f"[翻译错误: {e}]"

class TranslationServer:
    """翻译服务器，管理 HTTP 服务器和模型"""

    def __init__(self, port=9876, model_path=None):
        self.port = port
        self.translator = Translator(model_path)
        self.server = None
        self.thread = None

    def start(self):
        """启动服务器（非阻塞）"""
        try:
            print(f"正在启动 HTTP 服务器，端口: {self.port}")
            self.server = HTTPServer(('localhost', self.port), TranslationHandler)
            print("HTTPServer 创建成功")

            self.server.translator = self.translator
            print("translator 已绑定到 server")

            self.thread = threading.Thread(target=self.server.serve_forever)
            self.thread.daemon = True
            self.thread.start()
            print(f"[成功] 翻译服务器已启动，端口: {self.port}")
            return True
        except Exception as e:
            print(f"[失败] 服务器启动失败: {e}")
            import traceback
            traceback.print_exc()
            return False

    def stop(self):
        """停止服务器"""
        if self.server:
            self.server.shutdown()
            self.server.server_close()
            print("翻译服务器已停止")

def find_free_port():
    """查找空闲端口"""
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(('', 0))
        return s.getsockname()[1]

def main():
    parser = argparse.ArgumentParser(description='LiveTranslator 服务器')
    parser.add_argument('--port', type=int, default=0, help='服务器端口')
    parser.add_argument('--model-path', type=str, help='模型路径')
    args = parser.parse_args()

    print(f"命令行参数: port={args.port}, model-path={args.model_path}")

    port = args.port if args.port != 0 else find_free_port()
    print(f"使用端口: {port}")

    print("正在创建 TranslationServer 实例...")
    server = TranslationServer(port, args.model_path)
    print("TranslationServer 实例创建成功")

    print("正在调用 server.start()...")
    if server.start():
        print(f"SERVER_PORT:{port}")
        print("服务器启动成功，进入主循环")

        try:
            # 保持运行
            while True:
                time.sleep(1)
        except KeyboardInterrupt:
            print("收到中断信号，停止服务器")
            server.stop()
    else:
        print("server.start() 返回 False，启动失败")

if __name__ == '__main__':
    main()