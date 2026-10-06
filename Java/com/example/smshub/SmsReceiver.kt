package com.example.smshub

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in smsMessages) {
                val sender = sms.originatingAddress ?: ""
                val messageBody = sms.messageBody ?: ""

                // فحص إذا كانت الرسالة تخص فودافون كاش
                if (sender.contains("Vodafone") || sender.contains("VFCash") || messageBody.contains("تحويل")) {
                    Log.i("SMS_DEBUG", "تم رصد رسالة فودافون كاش: $messageBody")
                    sendToServer(sender, messageBody)
                }
            }
        }
    }

    private fun sendToServer(sender: String, message: String) {
        val client = OkHttpClient()
        val serverUrl = "https://your-server-domain.com/api/sms-webhook" // رابط سيرفرك لاحقاً
        val secretKey = "MY_SECURE_KEY_123"

        val jsonBody = JSONObject().apply {
            put("sender", sender)
            put("message", message)
            put("secretKey", secretKey)
        }

        val body = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(serverUrl)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SMS_SERVER", "فشل الإرسال للسيرفر: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.i("SMS_SERVER", "تم إرسال الرسالة للسيرفر بنجاح!")
                }
            }
        })
    }
}
