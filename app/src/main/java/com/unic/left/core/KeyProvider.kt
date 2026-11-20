package com.unic.left.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.nio.charset.StandardCharsets

/**
 * 运行时从 assets/mapkey.png 的 RGB 最低有效位解码出腾讯地图 Key。
 * 明文 Key 不存在于源码/构建脚本中；PNG 为 LSB 隐写产物（混淆而非强加密）。
 *
 * 位格式（与 tools/encode_map_key.py 一致）：
 *   - 像素行优先；每像素 R→G→B 取 LSB
 *   - 每字节 MSB 先发
 *   - 头 32 bit magic 'LFT1'，随后 16 bit 大端 = payload 长度，接着 Key 的 UTF-8 字节
 */
object KeyProvider {

    private val MAGIC = byteArrayOf(0x4C, 0x46, 0x54, 0x31) // LFT1
    private const val ASSET_NAME = "mapkey.png"
    private const val MAX_PAYLOAD = 512

    @Volatile
    private var cached: String? = null

    fun get(context: Context): String {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: decodeFromAsset(context).also { cached = it }
        }
    }

    private fun decodeFromAsset(context: Context): String {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inPremultiplied = false
        }
        val bitmap = context.assets.open(ASSET_NAME).use { input ->
            BitmapFactory.decodeStream(input, null, options)
                ?: throw IllegalStateException("mapkey.png: 无法解码图片")
        }
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        bitmap.recycle()
        return decodePixels(pixels, width)
    }

    /** 纯函数：从 ARGB 像素数组解码 Key，便于单元测试。信息中绝不含 Key 文本。 */
    fun decodePixels(pixels: IntArray, width: Int): String {
        val bits = mutableListOf<Int>()
        val needed = 48 + MAX_PAYLOAD * 8
        outer@ for (y in 0 until pixels.size / width) {
            for (x in 0 until width) {
                val c = pixels[y * width + x]
                bits.add((c shr 16) and 1) // R
                bits.add((c shr 8) and 1)  // G
                bits.add(c and 1)          // B
                if (bits.size >= needed) break@outer
            }
        }
        if (bits.size < 48) throw IllegalStateException("mapkey.png: 像素不足")

        fun readByte(fromBit: Int): Int {
            var v = 0
            for (i in 0 until 8) v = (v shl 1) or bits[fromBit + i]
            return v
        }

        val magic = ByteArray(4) { readByte(it * 8).toByte() }
        if (!magic.contentEquals(MAGIC)) throw IllegalStateException("mapkey.png: magic 不匹配")

        val length = (readByte(32) shl 8) or readByte(40)
        if (length < 1 || length > MAX_PAYLOAD) {
            throw IllegalStateException("mapkey.png: 非法长度 $length")
        }

        val bytes = ByteArray(length) { readByte(48 + it * 8).toByte() }
        return String(bytes, StandardCharsets.UTF_8)
    }
}
