package com.jarvis.assistant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

class JarvisSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = JarvisSession(this)
}

/**
 * Runs when the user triggers the assistant (power-button or home-button long press, or
 * "Hey Google" style shortcuts). Wakes Jarvis immediately so the user can just speak a command.
 */
class JarvisSession(private val context: Context) : VoiceInteractionSession(context) {
    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        val intent = Intent(context, VoiceService::class.java).setAction(VoiceService.ACTION_WAKE)
        try {
            context.startForegroundService(intent)
        } catch (e: Exception) {
            Bus.log("Open the Jarvis app once and tap Start listening.")
        }
        hide()
    }
}
