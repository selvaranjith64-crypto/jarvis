package com.jarvis.assistant

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/** Finds an installed app by its name and opens it. */
class AppLauncher(private val ctx: Context) {

    fun open(spoken: String): String {
        val name = spoken.trim().lowercase()
        if (name.isEmpty()) return "Which app should I open?"
        val pm = ctx.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(launcher, PackageManager.MATCH_ALL)
            .map { it.loadLabel(pm).toString() to it.activityInfo.packageName }

        val match = apps.firstOrNull { it.first.lowercase() == name }
            ?: apps.firstOrNull { it.first.lowercase().contains(name) }
            ?: apps.firstOrNull { name.contains(it.first.lowercase()) && it.first.length > 3 }
            ?: return "I couldn't find an app called $spoken."

        val intent = pm.getLaunchIntentForPackage(match.second)
            ?: return "I couldn't open ${match.first}."
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            ctx.startActivity(intent)
            "Opening ${match.first}."
        } catch (e: Exception) {
            "I couldn't open ${match.first}."
        }
    }
}
