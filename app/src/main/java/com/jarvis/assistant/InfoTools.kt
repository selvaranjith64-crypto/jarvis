package com.jarvis.assistant

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.provider.AlarmClock
import android.app.SearchManager
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Alexa/Siri-style helpers: time, date, timer, alarm, flashlight, search and Wikipedia answers. */
class InfoTools(private val ctx: Context) {

    fun time(): String =
        "It's " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

    fun date(): String =
        "Today is " + SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())

    fun timer(amount: Int, unit: String): String {
        val seconds = when (unit) {
            "hour" -> amount * 3600
            "minute" -> amount * 60
            else -> amount
        }
        return try {
            ctx.startActivity(
                Intent(AlarmClock.ACTION_SET_TIMER)
                    .putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                    .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            "Timer set for $amount $unit${if (amount == 1) "" else "s"}."
        } catch (e: Exception) {
            "I couldn't set the timer."
        }
    }

    fun alarm(hour: Int, minute: Int): String = try {
        ctx.startActivity(
            Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_HOUR, hour)
                .putExtra(AlarmClock.EXTRA_MINUTES, minute)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        "Alarm set for ${"%d:%02d".format(if (hour % 12 == 0) 12 else hour % 12, minute)} ${if (hour < 12) "AM" else "PM"}."
    } catch (e: Exception) {
        "I couldn't set the alarm."
    }

    fun flashlight(on: Boolean): String = try {
        val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = cm.cameraIdList.first()
        cm.setTorchMode(id, on)
        if (on) "Flashlight on." else "Flashlight off."
    } catch (e: Exception) {
        "I couldn't use the flashlight."
    }

    fun webSearch(query: String): String = try {
        ctx.startActivity(
            Intent(Intent.ACTION_WEB_SEARCH)
                .putExtra(SearchManager.QUERY, query)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        "Searching for $query."
    } catch (e: Exception) {
        try {
            ctx.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query)))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            "Searching for $query."
        } catch (e2: Exception) {
            "I couldn't open a browser."
        }
    }

    fun weather(): String = webSearch("weather today")

    /** Looks up a short answer on Wikipedia in the background, then hands it to [onResult]. */
    fun wikipedia(topic: String, onResult: (String?) -> Unit) {
        Thread {
            val answer = try {
                val title = topic.trim().split(" ")
                    .joinToString("_") { w -> w.replaceFirstChar { it.uppercase() } }
                val url = URL(
                    "https://en.wikipedia.org/api/rest_v1/page/summary/" +
                        URLEncoder.encode(title, "UTF-8")
                )
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                conn.setRequestProperty("User-Agent", "JarvisAssistant/1.0")
                if (conn.responseCode != 200) {
                    null
                } else {
                    val extract = JSONObject(conn.inputStream.bufferedReader().readText())
                        .optString("extract")
                    extract.split(". ").take(2).joinToString(". ").trim()
                        .let { if (it.endsWith(".")) it else "$it." }
                        .takeIf { extract.isNotBlank() }
                }
            } catch (e: Exception) {
                null
            }
            onResult(answer)
        }.start()
    }
}
