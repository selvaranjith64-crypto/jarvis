package com.jarvis.assistant

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.SmsManager
import androidx.core.content.ContextCompat

/** Contacts lookup, calling, SMS and WhatsApp. */
class ContactsHelper(private val ctx: Context) {

    // Added in front of 10-digit numbers for WhatsApp links. Change to your country code.
    private val defaultCountryCode = "91"

    private fun has(permission: String) =
        ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED

    fun findNumber(name: String): String? {
        if (!has(Manifest.permission.READ_CONTACTS)) return null
        val cursor = ctx.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$name%"),
            null
        ) ?: return null
        cursor.use { if (it.moveToFirst()) return it.getString(0) }
        return null
    }

    fun call(name: String): String {
        if (!has(Manifest.permission.CALL_PHONE)) return "I need the phone permission to place calls."
        val number = if (name.any { it.isLetter() }) findNumber(name) else name.filter { it.isDigit() || it == '+' }
        if (number.isNullOrBlank()) return "I couldn't find $name in your contacts."
        return try {
            ctx.startActivity(
                Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(number)}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            "Calling $name."
        } catch (e: Exception) {
            "I couldn't place the call."
        }
    }

    @Suppress("DEPRECATION")
    private fun smsManager(): SmsManager =
        if (Build.VERSION.SDK_INT >= 31) ctx.getSystemService(SmsManager::class.java)
        else SmsManager.getDefault()

    fun sms(name: String, message: String): String {
        if (!has(Manifest.permission.SEND_SMS)) return "I need the SMS permission to send messages."
        val number = findNumber(name) ?: return "I couldn't find $name in your contacts."
        return try {
            val manager = smsManager()
            manager.sendMultipartTextMessage(number, null, manager.divideMessage(message), null, null)
            "Message sent to $name."
        } catch (e: Exception) {
            "I couldn't send the message."
        }
    }

    /** Opens a WhatsApp chat with the text filled in. WhatsApp still needs one tap on send. */
    fun whatsapp(name: String, message: String): String {
        val raw = findNumber(name) ?: return "I couldn't find $name in your contacts."
        var digits = raw.filter { it.isDigit() }
        if (digits.length == 10) digits = defaultCountryCode + digits
        return try {
            val uri = Uri.parse("https://wa.me/$digits?text=${Uri.encode(message)}")
            ctx.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            "WhatsApp is ready with your message to $name. Tap send."
        } catch (e: Exception) {
            "I couldn't open WhatsApp."
        }
    }
}
