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
        if (intent?.action == "android.provider.Telephony.SMS_RECEIVED") {
            val bundle = intent.extras
            if (bundle != null) {
                val pdus = bundle.get("pdus") as Array<*>?
                if (pdus != null) {
                    for (pdu in pdus) {
                        val sms = SmsMessage.createFromPdu(pdu as ByteArray)
                        val sender = sms.originatingAddress ?: ""
                        val body = sms.messageBody ?: ""

                        // تصفية الرسائل لتلتقط Vodafone Cash فقط
                        if (sender.contains("Vodafone", ignoreCase = true) || body.contains("فودافون كاش") || body.contains("Vodafone Cash")) {
                            sendToServer(sender, body)
                        }
                    }
                }
            }
        }
    }

    private fun sendToServer(sender: String, message: String) {
        thread {
            try {
                // استبدل هذا الرابط برابط السيرفر أو الـ Webhook الخاص بك
                val url = URL("https://your-server-domain.com/api/sms")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; utf-8")
                conn.doOutput = true

                val jsonInputString = "{\"sender\": \"$sender\", \"message\": \"$message\"}"

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
