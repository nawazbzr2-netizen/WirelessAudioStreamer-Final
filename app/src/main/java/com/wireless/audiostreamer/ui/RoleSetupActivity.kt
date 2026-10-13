package com.wireless.audiostreamer.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import com.wireless.audiostreamer.R
import com.wireless.audiostreamer.util.AppPrefs

class RoleSetupActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_setup)

        val etPin = findViewById<EditText>(R.id.etPin)
        val rbSender = findViewById<RadioButton>(R.id.rbSender)
        val rbReceiver = findViewById<RadioButton>(R.id.rbReceiver)

        val existingRole = AppPrefs.role(this)
        if (existingRole == AppPrefs.ROLE_SENDER) rbSender.isChecked = true
        else if (existingRole == AppPrefs.ROLE_RECEIVER) rbReceiver.isChecked = true
        else rbSender.isChecked = true

        etPin.setText(AppPrefs.pin(this))

        findViewById<Button>(R.id.btnContinue).setOnClickListener {
            val pin = etPin.text?.toString()?.trim().orEmpty()
            if (pin.length < 4 || !pin.all { it.isDigit() }) {
                Toast.makeText(this, R.string.pin_invalid, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val role = if (rbSender.isChecked) AppPrefs.ROLE_SENDER else AppPrefs.ROLE_RECEIVER
            AppPrefs.setPin(this, pin)
            AppPrefs.setRole(this, role)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
