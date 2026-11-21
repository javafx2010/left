package com.unic.watchdog.tts

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 全局语音播报：微软 Edge TTS 在线合成 + MediaPlayer 播放。
 * 串行（一次一条，新请求打断上一条），默认音色晓伊（zh-CN-XiaoyiNeural）。
 */
class TtsSpeaker(context: Context) {

    private val appContext = context.applicationContext
    private val synthesizer = EdgeTtsSynthesizer()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val playFile = File(appContext.cacheDir, "tts_play.mp3")

    private var job: Job? = null
    private var player: MediaPlayer? = null

    companion object {
        private const val TAG = "TtsSpeaker"
        const val DEFAULT_VOICE = "zh-CN-XiaoyiNeural"
        const val DEFAULT_LANG = "zh-CN"
    }

    /** 立即打断上一条，合成并播放 [text]。空文本忽略。 */
    fun speak(text: String, voice: String = DEFAULT_VOICE, lang: String = DEFAULT_LANG) {
        val t = text.trim()
        if (t.isEmpty()) return
        job?.cancel()
        stopPlayer()
        job = scope.launch {
            try {
                val bytes = synthesizer.synthesize(t, voice, lang)
                withContext(Dispatchers.Main) { play(bytes) }
            } catch (e: Exception) {
                Log.w(TAG, "合成失败: ${e.message}")
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        stopPlayer()
    }

    private fun play(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        try {
            playFile.writeBytes(bytes)
            val mp = MediaPlayer().apply {
                setDataSource(playFile.absolutePath)
                setOnCompletionListener { p ->
                    p.release()
                    if (player === p) player = null
                }
                setOnErrorListener { p, _, _ ->
                    p.release()
                    if (player === p) player = null
                    true
                }
                prepare()
                start()
            }
            player = mp
        } catch (e: Exception) {
            Log.w(TAG, "播放失败: ${e.message}")
        }
    }

    private fun stopPlayer() {
        try {
            player?.stop()
            player?.release()
        } catch (_: Exception) {
        }
        player = null
    }
}
