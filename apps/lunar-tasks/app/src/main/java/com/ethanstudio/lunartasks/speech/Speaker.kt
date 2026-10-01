package com.ethanstudio.lunartasks.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID

/**
 * Đọc to bằng bộ đọc văn bản có sẵn trong máy (TextToSpeech), không cần mạng hay quyền gì.
 * Ưu tiên giọng tiếng Việt; máy không có thì dùng giọng theo ngôn ngữ máy.
 */
class Speaker(context: Context) {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private val ready = CompletableDeferred<Boolean>()

    private fun engine(): TextToSpeech =
        tts ?: TextToSpeech(appContext) { status ->
            val ok = status == TextToSpeech.SUCCESS
            if (ok) chooseVoice()
            ready.complete(ok)
        }.also { tts = it }

    private fun chooseVoice() {
        val engine = tts ?: return
        val vietnamese = Locale.forLanguageTag("vi-VN")
        val preferred = if (Locale.getDefault().language == "vi") vietnamese else Locale.getDefault()
        val result = engine.setLanguage(preferred)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale.getDefault())
        }
    }

    /** Đọc [text], ngắt câu đang đọc (nếu có). Trả về false nếu máy không có bộ đọc. */
    suspend fun speak(text: String): Boolean {
        val engine = engine()
        val ok = withTimeoutOrNull(INIT_TIMEOUT_MS) { ready.await() } ?: false
        if (!ok || text.isBlank()) return false
        return engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString()) == TextToSpeech.SUCCESS
    }

    /** Đọc xong rồi mới trả về (dùng trong BroadcastReceiver, có giới hạn thời gian). */
    suspend fun speakAndWait(text: String, timeoutMs: Long): Boolean {
        val engine = engine()
        val ok = withTimeoutOrNull(INIT_TIMEOUT_MS) { ready.await() } ?: false
        if (!ok || text.isBlank()) return false
        val done = CompletableDeferred<Unit>()
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) {
                done.complete(Unit)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                done.complete(Unit)
            }
        })
        val queued = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString()) == TextToSpeech.SUCCESS
        if (queued) withTimeoutOrNull(timeoutMs) { done.await() }
        return queued
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }

    private companion object {
        const val INIT_TIMEOUT_MS = 3_000L
    }
}
