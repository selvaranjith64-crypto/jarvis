package com.jarvis.assistant

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var log: TextView
    private lateinit var toggle: Button
    private lateinit var wakeWord: EditText

    private val permissions = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.ANSWER_PHONE_CALLS)
        add(Manifest.permission.CALL_PHONE)
        add(Manifest.permission.READ_PHONE_STATE)
        add(Manifest.permission.READ_CONTACTS)
        add(Manifest.permission.SEND_SMS)
        add(Manifest.permission.SET_ALARM)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result[Manifest.permission.RECORD_AUDIO] == true) {
                askOverlayThenStart()
            } else {
                append("Microphone permission is required.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.status)
        log = findViewById(R.id.log)
        toggle = findViewById(R.id.toggle)
        wakeWord = findViewById(R.id.wakeWord)

        val prefs = getSharedPreferences(VoiceService.PREFS, Context.MODE_PRIVATE)
        wakeWord.setText(prefs.getString(VoiceService.KEY_WAKE, "jarvis"))

        findViewById<Button>(R.id.setDefault).setOnClickListener {
            append("Choose Jarvis under \"Digital assistant app\" and confirm.")
            try {
                startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
            }
        }

        toggle.setOnClickListener {
            if (VoiceService.running) {
                startService(Intent(this, VoiceService::class.java).setAction(VoiceService.ACTION_STOP))
            } else {
                prefs.edit()
                    .putString(VoiceService.KEY_WAKE, wakeWord.text.toString().trim().lowercase().ifBlank { "jarvis" })
                    .apply()
                permissionLauncher.launch(permissions)
            }
            toggle.postDelayed({ refresh() }, 500)
        }
    }

    /**
     * Android blocks apps from opening other apps from the background. The "display over other
     * apps" permission lets Jarvis open Spotify and other apps while you are in another app.
     */
    private fun askOverlayThenStart() {
        if (!Settings.canDrawOverlays(this)) {
            append("Please allow \"Display over other apps\" for Jarvis, then press Start again.")
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
            return
        }
        startForegroundService(Intent(this, VoiceService::class.java))
        toggle.postDelayed({ refresh() }, 500)
    }

    private fun refresh() {
        val on = VoiceService.running
        status.text = if (on) "Listening" else "Stopped"
        toggle.text = if (on) "Stop listening" else "Start listening"
    }

    private fun append(line: String) {
        log.text = "$line\n${log.text}"
    }

    override fun onResume() {
        super.onResume()
        Bus.onLog = { append(it) }
        refresh()
    }

    override fun onPause() {
        Bus.onLog = null
        super.onPause()
    }
}
