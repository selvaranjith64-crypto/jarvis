package com.jarvis.assistant

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.view.KeyEvent
import android.media.AudioManager

/** Spotify playback plus generic media keys that work with whatever player is active. */
class MusicController(private val ctx: Context) {
    private val audio = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private fun spotifyInstalled(): Boolean = try {
        ctx.packageManager.getPackageInfo("com.spotify.music", 0)
        true
    } catch (e: Exception) {
        false
    }

    /** Starts playing [query] (song, artist or playlist) in Spotify. */
    fun playOnSpotify(query: String): String {
        if (!spotifyInstalled()) return "Spotify is not installed."
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                setPackage("com.spotify.music")
                putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
            "Playing $query on Spotify."
        } catch (e: Exception) {
            // Fallback: open the Spotify search screen for the query.
            try {
                val uri = Uri.parse("spotify:search:" + Uri.encode(query))
                ctx.startActivity(
                    Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                "I opened Spotify search for $query."
            } catch (e2: Exception) {
                "I couldn't open Spotify."
            }
        }
    }

    private fun key(code: Int) {
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }

    fun pause() = key(KeyEvent.KEYCODE_MEDIA_PAUSE)
    fun resume() = key(KeyEvent.KEYCODE_MEDIA_PLAY)
    fun next() = key(KeyEvent.KEYCODE_MEDIA_NEXT)
    fun previous() = key(KeyEvent.KEYCODE_MEDIA_PREVIOUS)

    fun volumeUp() = audio.adjustStreamVolume(
        AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI
    )

    fun volumeDown() = audio.adjustStreamVolume(
        AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI
    )
}
