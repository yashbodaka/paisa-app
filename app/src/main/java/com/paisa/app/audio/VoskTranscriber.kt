package com.paisa.app.audio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileOutputStream
import org.json.JSONObject

/**
 * Robust, offline Speech-to-Text using Vosk.
 * Much faster and more accurate for Hindi/English than Whisper-tiny.
 */
class VoskTranscriber(private val context: Context) {

    private var model: Model? = null

    /**
     * Initializes the model by copying it from assets if needed.
     */
    private suspend fun initModel(): Model = withContext(Dispatchers.IO) {
        if (model != null) return@withContext model!!

        val modelPath = File(context.filesDir, "vosk-model")
        
        if (!modelPath.exists() || modelPath.list()?.isEmpty() == true) {
            copyAssetFolder("vosk-model", modelPath.absolutePath)
        }

        if (!modelPath.exists()) {
            error("Vosk model files not found. Run scripts/download-vosk-model.ps1")
        }

        model = Model(modelPath.absolutePath)
        model!!
    }

    private fun copyAssetFolder(srcName: String, dstName: String) {
        val assetManager = context.assets
        val dstFile = File(dstName)
        
        try {
            val assets = assetManager.list(srcName) ?: return
            if (assets.isEmpty()) {
                // It's a file
                assetManager.open(srcName).use { inputStream ->
                    FileOutputStream(dstFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } else {
                // It's a directory
                if (!dstFile.exists()) dstFile.mkdirs()
                for (asset in assets) {
                    copyAssetFolder("$srcName/$asset", "$dstName/$asset")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun transcribe(audio: FloatArray): String = withContext(Dispatchers.Default) {
        val voskModel = initModel()
        
        // Vosk works best with 16-bit PCM shorts
        val shortAudio = ShortArray(audio.size) { i ->
            (audio[i] * 32767).toInt().toShort()
        }

        val recognizer = Recognizer(voskModel, 16000.0f)
        
        // Feed the audio
        recognizer.acceptWaveForm(shortAudio, shortAudio.size)
        
        // Get the final result
        val jsonResult = recognizer.finalResult
        
        val text = try {
            JSONObject(jsonResult as String).getString("text")
        } catch (e: Exception) {
            ""
        }

        recognizer.close()
        text
    }

    fun close() {
        model?.close()
        model = null
    }
}
