package com.jarvis.assistant

import android.os.Handler
import android.os.Looper

/** Tiny in-app log channel so the service can show activity on the main screen. */
object Bus {
    var onLog: ((String) -> Unit)? = null
    private val main = Handler(Looper.getMainLooper())

    fun log(message: String) {
        main.post { onLog?.invoke(message) }
    }
}
