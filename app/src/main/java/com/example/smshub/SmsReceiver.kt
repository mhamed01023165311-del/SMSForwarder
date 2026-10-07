package com.example.smshub

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "android.provider.Telephony.SMS_RECEIVED" && context != null) {

            val prefs = context.getSharedPreferences("SMS_SETTINGS", Context.MODE_PRIVATE)
            val targetSender = prefs.getString("TARGET_SENDER", "VodafoneCash") ?: ""

            val bundle = intent.extras
            if (bundle != null) {
                val pdus = bundle.get("pdus") as Array<*>?
                if (pdus != null) {
                    for (pdu in pdus) {
                        val sms = SmsMessage.createFromPdu(pdu as ByteArray)
                        val sender = sms.originatingAddress ?: ""
                        val body = sms.messageBody ?: ""

                        // المطابقة مع المحادثة المختارة من الشاشة
                        if (targetSender.isNotEmpty() && sender.contains(targetSender, ignoreCase = true)) {
                            sendToFirebase(sender, body)
                        }
                    }
                }
            }
        }
    }

    private fun sendToFirebase(sender: String, message: String) {
        thread {
            try {
                val url = URL("https://firestore.googleapis.com/v1/projects/ai-studio-c8716547-cab3-499a-9da6-dc69b8b88d2c/databases/(default)/documents/sms_logs")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; utf-8")
                conn.doOutput = true

                val jsonInputString = """
                    {
                      "fields": {
                        "sender_phone": { "stringValue": "$sender" },
                        "message": { "stringValue": "$message" },
                        "is_used": { "booleanValue": false },
                        "timestamp": { "integerValue": "${System.currentTimeMillis()}" }
                      }
                    }
                """.trimIndent()

                OutputStreamWriter(conn.outputStream).use { os ->
                    os.write(jsonInputString)
                    os.flush()
                }
                conn.responseCode
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
