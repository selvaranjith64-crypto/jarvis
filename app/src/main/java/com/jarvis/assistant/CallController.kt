package com.jarvis.assistant

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

/** Answer / end phone calls by voice. Needs the ANSWER_PHONE_CALLS permission. */
class CallController(private val ctx: Context) {
    private val telecom = ctx.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

    private fun allowed() = ContextCompat.checkSelfPermission(
        ctx, Manifest.permission.ANSWER_PHONE_CALLS
    ) == PackageManager.PERMISSION_GRANTED

    /** True while a call is ringing or in progress. */
    @Suppress("DEPRECATION")
    fun isCallActive(): Boolean {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) return false
        val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return tm.callState != TelephonyManager.CALL_STATE_IDLE
    }

    fun answer(): String {
        if (!allowed()) return "I need the phone permission to answer calls."
        return try {
            telecom.acceptRingingCall()
            "Call answered."
        } catch (e: SecurityException) {
            "I couldn't answer the call."
        }
    }

    fun end(): String {
        if (!allowed()) return "I need the phone permission to end calls."
        if (Build.VERSION.SDK_INT < 28) return "Ending calls needs Android 9 or newer."
        return try {
            @Suppress("DEPRECATION")
            val ended = telecom.endCall()
            if (ended) "Call ended." else "There is no call to end."
        } catch (e: SecurityException) {
            "I couldn't end the call."
        }
    }
}
