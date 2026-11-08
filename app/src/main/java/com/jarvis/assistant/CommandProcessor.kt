package com.jarvis.assistant

import android.content.Context
import android.os.Handler
import android.os.Looper

/**
 * Turns a spoken sentence into actions. Supports chaining, e.g.
 * "answer the call and then play Believer on Spotify".
 */
class CommandProcessor(ctx: Context, private val speaker: Speaker) {

    private val handler = Handler(Looper.getMainLooper())
    val calls = CallController(ctx)
    private val music = MusicController(ctx)
    private val apps = AppLauncher(ctx)
    private val contacts = ContactsHelper(ctx)
    private val info = InfoTools(ctx)

    private val chainSplit = Regex(
        "\\s+(?:and then|then|after that|and)\\s+(?=(?:play|open|launch|call|cut|end|hang|reject|" +
            "decline|answer|pick|attend|accept|text|send|whatsapp|set|pause|resume|stop|next|skip|" +
            "turn|what|who|tell))"
    )

    fun run(raw: String) {
        Bus.log("Heard: $raw")
        val steps = normalize(raw).split(chainSplit).filter { it.isNotBlank() }
        steps.forEachIndexed { i, step ->
            handler.postDelayed({ execute(step.trim()) }, i * 2500L)
        }
    }

    private fun reply(text: String) = speaker.say(text)

    private fun normalize(raw: String): String {
        var t = raw.lowercase().replace(".", "").replace(",", "").trim()
        val words = mapOf(
            "one" to "1", "two" to "2", "three" to "3", "four" to "4", "five" to "5",
            "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9", "ten" to "10",
            "eleven" to "11", "twelve" to "12", "fifteen" to "15", "twenty" to "20",
            "thirty" to "30", "forty five" to "45", "sixty" to "60"
        )
        // Only convert number words in timer/alarm commands so song names stay intact.
        if (t.contains("timer") || t.contains("alarm")) {
            // "7 30 a m" -> "7 30am" (only here, so words like "a mellow song" stay intact)
            t = t.replace(Regex("(\\d)\\s*([ap])\\s*m\\b"), "\$1\$2m")
            words.entries.sortedByDescending { it.key.length }.forEach { (w, d) ->
                t = t.replace(Regex("\\b$w\\b"), d)
            }
        }
        return t
    }

    private fun execute(cmd: String) {
        // --- calls ---
        if (Regex("^(answer|pick up|attend|accept)\\b.*").matches(cmd)) {
            return reply(calls.answer())
        }
        if (Regex("^(cut|end|hang up|reject|decline|disconnect)\\b.*").matches(cmd)) {
            return reply(calls.end())
        }

        // --- music ---
        Regex("^play (.+?)(?: on spotify)?$").find(cmd)?.let {
            val query = it.groupValues[1]
            return reply(music.playOnSpotify(query))
        }
        if (cmd == "play" || cmd == "resume" || cmd == "resume music") {
            music.resume(); return
        }
        if (cmd.matches(Regex("^(pause|stop)( the)?( music| song| playing)?$"))) {
            music.pause(); return
        }
        if (cmd.matches(Regex("^(next|skip)( song| track)?$"))) {
            music.next(); return
        }
        if (cmd.matches(Regex("^(previous|go back)( song| track)?$"))) {
            music.previous(); return
        }
        if (cmd.contains("volume up") || cmd.contains("louder")) {
            music.volumeUp(); return
        }
        if (cmd.contains("volume down") || cmd.contains("quieter") || cmd.contains("lower the volume")) {
            music.volumeDown(); return
        }

        // --- messaging ---
        Regex("^(?:send (?:a )?(?:message|text|sms) to|text) (.+?) (?:saying|that says|message) (.+)$")
            .find(cmd)?.let { return reply(contacts.sms(it.groupValues[1], it.groupValues[2])) }
        Regex("^(?:send (?:a )?whatsapp (?:message )?to|whatsapp) (.+?) (?:saying|that says|message) (.+)$")
            .find(cmd)?.let { return reply(contacts.whatsapp(it.groupValues[1], it.groupValues[2])) }

        // --- calling a contact ---
        Regex("^(?:call|dial|phone) (.+)$").find(cmd)?.let {
            return reply(contacts.call(it.groupValues[1]))
        }

        // --- info and tools ---
        Regex("(?:set|start)(?: a)? timer for (\\d+) (second|minute|hour)s?").find(cmd)?.let {
            return reply(info.timer(it.groupValues[1].toInt(), it.groupValues[2]))
        }
        Regex("set(?: an)? alarm (?:for|at) (\\d{1,2})(?::(\\d{2}))? ?(am|pm)?").find(cmd)?.let {
            var hour = it.groupValues[1].toInt()
            val minute = it.groupValues[2].ifEmpty { "0" }.toInt()
            when (it.groupValues[3]) {
                "pm" -> if (hour < 12) hour += 12
                "am" -> if (hour == 12) hour = 0
            }
            return reply(info.alarm(hour, minute))
        }
        // --- open apps ---
        Regex("^(?:open|launch|start) (.+)$").find(cmd)?.let {
            return reply(apps.open(it.groupValues[1]))
        }

        if (Regex("(turn|switch) (on|off).*(flashlight|torch)").containsMatchIn(cmd)) {
            return reply(info.flashlight(cmd.contains(" on")))
        }
        if (Regex("\\btime\\b").containsMatchIn(cmd) && (cmd.startsWith("what") || cmd.startsWith("tell"))) {
            return reply(info.time())
        }
        if (Regex("\\bdate\\b").containsMatchIn(cmd) || cmd.contains("what day")) {
            return reply(info.date())
        }
        if (Regex("\\bweather\\b").containsMatchIn(cmd)) {
            return reply(info.weather())
        }
        Regex("^(?:who is|who was|what is|what are|tell me about) (.+)$").find(cmd)?.let {
            val topic = it.groupValues[1]
            info.wikipedia(topic) { answer ->
                handler.post { reply(answer ?: info.webSearch(cmd)) }
            }
            return
        }

        // --- fallback ---
        reply(info.webSearch(cmd))
    }
}
