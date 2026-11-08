package com.jarvis.assistant

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Speaks replies. Uses "transient may duck" audio focus so music (Spotify) just gets quieter
 * while Jarvis talks and returns to full volume afterwards instead of being stopped.
 * Anything said before the text-to-speech engine is ready is queued and spoken once it is.
 */
class Speaker(context: Context) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var ready = false
    private lateinit var tts: TextToSpeech
    private val pending = mutableListOf<String>()
    private var outstanding = 0

    @Volatile
    var isSpeaking = false
        private set

    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANT)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attrs)
        .build()

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.getDefault()
                tts.setAudioAttributes(attrs)
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeaking = true
                    }

                    override fun onDone(utteranceId: String?) {
                        finishOne()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        finishOne()
                    }
                })
                ready = true
                val queued = synchronized(pending) { pending.toList().also { pending.clear() } }
                queued.forEach { speakNow(it) }
            }
        }
    }

    /** Called when one spoken reply ends. Focus is released only when nothing is left to say. */
    private fun finishOne() {
        val idle = synchronized(this) {
            outstanding = (outstanding - 1).coerceAtLeast(0)
            outstanding == 0
        }
        if (idle) {
            isSpeaking = false
            audio.abandonAudioFocusRequest(focusRequest)
        }
    }

    private fun speakNow(text: String) {
        audio.requestAudioFocus(focusRequest)
        synchronized(this) { outstanding++ }
        isSpeaking = true
        val result = tts.speak(text, TextToSpeech.QUEUE_ADD, null, "jarvis-${System.nanoTime()}")
        if (result != TextToSpeech.SUCCESS) finishOne()
    }

    fun say(text: String) {
        Bus.log("Jarvis: $text")
        if (!ready) {
            synchronized(pending) { pending.add(text) }
            return
        }
        speakNow(text)
    }

    fun shutdown() {
        if (ready) {
            tts.stop()
            tts.shutdown()
        }
        audio.abandonAudioFocusRequest(focusRequest)
    }
}
