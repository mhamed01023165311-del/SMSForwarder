package com.example.smshub

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "android.provider.Telephony.SMS_RECEIVED" && context != null) {

            val prefs = context.getSharedPreferences("SMS_SETTINGS", Context.MODE_PRIVATE)
            val savedSender = prefs.getString("TARGET_SENDER", "VF-Cash") ?: "VF-Cash"

            val bundle = intent.extras
            if (bundle != null) {
                val pdus = bundle.get("pdus") as Array<*>?
                if (pdus != null) {
                    for (pdu in pdus) {
                        val sms = SmsMessage.createFromPdu(pdu as ByteArray)
                        val sender = sms.originatingAddress ?: ""
                        val body = sms.messageBody ?: ""

                        Log.d("SMS_HUB", "رسالة واردة من: $sender | النص: $body")

                        // المطابقة مع الشات المحدد أو مرسل فودافون كاش
                        if (sender.contains(savedSender, ignoreCase = true) || 
                            sender.contains("VF-Cash", ignoreCase = true) || 
                            sender.contains("Vodafone", ignoreCase = true)) {
                            
                            sendToFirestoreDirect(sender, body)
                        }
                    }
                }
            }
        }
    }

    private fun sendToFirestoreDirect(sender: String, messageBody: String) {
        thread {
            try {
                // الرابط المباشر للمشروع وقاعدة البيانات ومجموعة sms_logs الموضحة بالصورة
                val firestoreUrl = "https://firestore.googleapis.com/v1/projects/ai-studio-applet-webapp-5d1dc/databases/ai-studio-c8576547-cab3-499a-9da6-dc69b8b88d2c/documents/sms_logs"
                
                val url = URL(firestoreUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.doOutput = true

                // الحقول بنفس أسماء الكي الإنجليزية المطابقة لقاعدة البيانات
                val jsonPayload = """
                    {
                      "fields": {
                        "sender_phone": { "stringValue": "$sender" },
                        "raw_message": { "stringValue": "${messageBody.replace("\n", " ").replace("\"", "\\\"")}" },
                        "is_used": { "booleanValue": false },
                        "timestamp": { "stringValue": "${System.currentTimeMillis()}" }
                      }
                    }
                """.trimIndent()

                OutputStreamWriter(conn.outputStream, "UTF-8").use { os ->
                    os.write(jsonPayload)
                    os.flush()
                }

                val responseCode = conn.responseCode
                Log.d("SMS_HUB", "كود الاستجابة من السيرفر: $responseCode")

            } catch (e: Exception) {
                Log.e("SMS_HUB", "خطأ في الاتصال بالسيرفر", e)
            }
        }
    }
}
