package com.ethanstudio.lunartasks.voice

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import com.ethanstudio.lunartasks.R
import java.util.Locale

/**
 * Mở ô nghe giọng nói có sẵn của máy (Google). App không xin quyền micro: chỉ nghe khi
 * người dùng bấm nút trong app, và máy tự đóng micro sau khi nghe xong một câu.
 * Trả về hàm để gọi khi bấm nút.
 */
@Composable
fun rememberVoiceInput(onResult: (String) -> Unit, onUnavailable: () -> Unit): () -> Unit {
    val prompt = stringResource(R.string.voice_prompt)
    val currentOnResult by rememberUpdatedState(onResult)
    val currentOnUnavailable by rememberUpdatedState(onUnavailable)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (result.resultCode == Activity.RESULT_OK && !spoken.isNullOrBlank()) currentOnResult(spoken)
    }
    return remember(launcher, prompt) {
        {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            try {
                launcher.launch(intent)
            } catch (e: ActivityNotFoundException) {
                currentOnUnavailable()
            }
        }
    }
}
