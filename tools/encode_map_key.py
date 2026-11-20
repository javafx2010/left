#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
开发期工具：把腾讯地图 Key 用 LSB 隐写编码进 PNG（不参与 App 构建）。

用法：
  TENCENT_MAP_KEY='你的Key' python tools/encode_map_key.py
  # 或：python tools/encode_map_key.py '你的Key'

输出：app/src/main/assets/mapkey.png

明文 Key 只从环境变量 / 命令行读取，绝不写入任何文件；脚本只打印字节数，不回显 Key。
旧方案（AES 密文）迁移：先用 openssl 解密现有密文得到明文，再通过上面的方式传入。

位格式（Kotlin KeyProvider.decodePixels 必须一致）：
  - 像素行优先；每像素取 R,G,B 的 LSB（跳过 alpha）
  - 每字节 MSB 先发
  - 头 32 bit = magic 'LFT1'，随后 16 bit 大端 = payload 字节数，接着为 Key 的 UTF-8 字节
"""
import os
import sys
from pathlib import Path
from PIL import Image

MAGIC = b"LFT1"
W = H = 128
MAX_PAYLOAD = 512


def key_from_input() -> bytes:
    key = os.environ.get("TENCENT_MAP_KEY") or (sys.argv[1] if len(sys.argv) > 1 else "")
    if not key:
        raise SystemExit("请通过环境变量 TENCENT_MAP_KEY 或命令行参数提供 Key")
    return key.encode("utf-8")


def bits_of(data: bytes):
    for byte in data:
        for i in range(7, -1, -1):
            yield (byte >> i) & 1


def main():
    payload = key_from_input()
    if not (1 <= len(payload) <= MAX_PAYLOAD):
        raise SystemExit(f"Key 长度必须在 1..{MAX_PAYLOAD} 字节之间")

    header = MAGIC + len(payload).to_bytes(2, "big")
    stream = list(bits_of(header + payload))

    capacity = W * H * 3
    if len(stream) > capacity:
        raise SystemExit(f"容量不足：需要 {len(stream)} bit，可用 {capacity} bit")

    # 生成看似无害的平滑渐变底图（RGB）。
    img = Image.new("RGB", (W, H), (0, 0, 0))
    px = img.load()
    for y in range(H):
        for x in range(W):
            px[x, y] = (
                (x * 255) // (W - 1),
                (y * 255) // (H - 1),
                ((x + y) * 255) // (W + H - 2),
            )

    # 把 bit 嵌入 R,G,B 的 LSB。
    idx = 0
    for y in range(H):
        for x in range(W):
            r, g, b = px[x, y]
            for ch in (0, 1, 2):
                if idx < len(stream):
                    bit = stream[idx]
                    if ch == 0:
                        r = (r & 0xFE) | bit
                    elif ch == 1:
                        g = (g & 0xFE) | bit
                    else:
                        b = (b & 0xFE) | bit
                    idx += 1
            px[x, y] = (r, g, b)

    out = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "assets" / "mapkey.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out, format="PNG", optimize=False)

    print(f"wrote {out}  ({W}x{H}, payload {len(payload)} bytes, {idx} bits)")


if __name__ == "__main__":
    main()
