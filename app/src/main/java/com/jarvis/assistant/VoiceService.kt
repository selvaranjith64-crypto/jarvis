package com.jarvis.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.os.Bundle
import java.util.Locale

/**
 * Always-on listener. Waits for the wake word, then runs the command that follows it.
 * While a call is ringing or active, "answer" and "cut the call" work without the wake word.
 */
class VoiceService : Service(), RecognitionListener {

    companion object {
        const val PREFS = "jarvis"
        const val KEY_WAKE = "wake_word"
        const val ACTION_STOP = "com.jarvis.assistant.STOP"
        const val ACTION_WAKE = "com.jarvis.assistant.WAKE"
        @Volatile var running = false
    }

    private var recognizer: SpeechRecognizer? = null
    private lateinit var speaker: Speaker
    private lateinit var processor: CommandProcessor
    private val handler = Handler(Looper.getMainLooper())
    private var awakeUntil = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        speaker = Speaker(this)
        processor = CommandProcessor(this, speaker)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startInForeground()
        val wasRunning = running
        running = true
        if (!wasRunning) {
            createRecognizer()
            listen()
            Bus.log("Listening for \"${wakeWord()}\"")
        }
        if (intent?.action == ACTION_WAKE) {
            // Triggered as the default assistant: skip the wake word and listen for a command.
            awakeUntil = System.currentTimeMillis() + 8000
            speaker.say("Yes?")
        }
        return START_STICKY
    }

    private fun wakeWord(): String =
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_WAKE, "jarvis")!!
            .lowercase().ifBlank { "jarvis" }

    private fun startInForeground() {
        val channelId = "jarvis_listening"
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(channelId, "Jarvis listening", NotificationManager.IMPORTANCE_LOW)
        )
        val stop = android.app.PendingIntent.getService(
            this, 0,
            Intent(this, VoiceService::class.java).setAction(ACTION_STOP),
            android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Jarvis is listening")
            .setContentText("Say \"${wakeWord()}\" followed by a command")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .addAction(Notification.Action.Builder(null, "Stop", stop).build())
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(1, notification)
        }
    }

    private fun createRecognizer() {
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).also {
            it.setRecognitionListener(this)
        }
    }

    private fun listen() {
        if (!running) return
        if (speaker.isSpeaking) {
            handler.postDelayed({ listen() }, 500)
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
        }
        try {
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            createRecognizer()
            handler.postDelayed({ listen() }, 1000)
        }
    }

    private fun handle(text: String) {
        val wake = wakeWord()
        val now = System.currentTimeMillis()
        var command = text
        val idx = text.indexOf(wake)
        when {
            idx >= 0 -> {
                command = text.substring(idx + wake.length).trim()
                if (command.isEmpty()) {
                    awakeUntil = now + 8000
                    speaker.say("Yes?")
                    return
                }
            }
            now < awakeUntil -> Unit
            processor.calls.isCallActive() && isCallCommand(text) -> Unit
            else -> return
        }
        awakeUntil = 0
        processor.run(command)
    }

    private fun isCallCommand(text: String) =
        Regex("^(answer|pick up|attend|accept|cut|end|hang up|reject|decline|disconnect)\\b").containsMatchIn(text)

    // --- RecognitionListener ---
    override fun onResults(results: Bundle?) {
        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()?.lowercase()?.trim()
        if (!text.isNullOrEmpty()) handle(text)
        handler.postDelayed({ listen() }, 300)
    }

    override fun onError(error: Int) {
        when (error) {
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                Bus.log("Microphone permission is missing.")
                stopSelf()
                return
            }
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY, SpeechRecognizer.ERROR_CLIENT -> createRecognizer()
        }
        handler.postDelayed({ listen() }, 400)
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    override fun onDestroy() {
        running = false
        handler.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        speaker.shutdown()
        Bus.log("Stopped")
        super.onDestroy()
    }
}
