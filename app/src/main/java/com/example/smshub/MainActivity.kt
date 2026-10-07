package com.example.smshub

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast

class MainActivity : Activity() {

    private val PERMISSION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // طلب الصلاحيات إن لم تكن ممنوحة
        if (checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS),
                PERMISSION_REQUEST_CODE
            )
        } else {
            loadSmsChats()
        }

        val btnSave = findViewById<Button>(R.id.btnSave)
        val spSenders = findViewById<Spinner>(R.id.spSenders)

        btnSave.setOnClickListener {
            val selectedSender = spSenders.selectedItem?.toString()
            if (!selectedSender.isNull_orEmpty()) {
                val prefs = getSharedPreferences("SMS_SETTINGS", Context.MODE_PRIVATE)
                prefs.edit().putString("TARGET_SENDER", selectedSender).apply()
                Toast.makeText(this, "تم تحديد الشات: $selectedSender", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadSmsChats()
        }
    }

    // جلب قائمة مرسلي الرسائل الحالية من الموبايل بدون تكرار
    private fun loadSmsChats() {
        val sendersList = mutableListOf<String>()
        val uri = Uri.parse("content://sms/inbox")
        val cursor = contentResolver.query(uri, arrayOf("address"), null, null, "date DESC")

        cursor?.use {
            val addressIndex = it.getColumnIndex("address")
            while (it.moveToNext()) {
                val address = it.getString(addressIndex)
                if (!address.isNull_orEmpty() && !sendersList.contains(address)) {
                    sendersList.add(address)
                }
            }
        }

        // إضافة خيار افتراضي إذا كانت القائمة فارغة
        if (sendersList.isEmpty()) {
            sendersList.add("VodafoneCash")
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sendersList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        
        val spSenders = findViewById<Spinner>(R.id.spSenders)
        spSenders.adapter = adapter
    }
}
