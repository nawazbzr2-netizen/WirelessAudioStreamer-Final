package com.wireless.audiostreamer.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import com.wireless.audiostreamer.R
import com.wireless.audiostreamer.network.NetUtil
import com.wireless.audiostreamer.util.AppPrefs

class SettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        when (AppPrefs.latency(this)) {
            AppPrefs.LAT_LOW -> findViewById<RadioButton>(R.id.rbLow).isChecked = true
            AppPrefs.LAT_SAFE -> findViewById<RadioButton>(R.id.rbSafe).isChecked = true
            else -> findViewById<RadioButton>(R.id.rbNormal).isChecked = true
        }

        if (AppPrefs.channels(this) == 2) findViewById<RadioButton>(R.id.rbStereo).isChecked = true
        else findViewById<RadioButton>(R.id.rbMono).isChecked = true

        findViewById<EditText>(R.id.etManualIp).setText(AppPrefs.manualIp(this))

        when (AppPrefs.lang(this)) {
            "en" -> findViewById<RadioButton>(R.id.rbLangEn).isChecked = true
            "ur" -> findViewById<RadioButton>(R.id.rbLangUr).isChecked = true
            else -> findViewById<RadioButton>(R.id.rbLangSystem).isChecked = true
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            val rgLat = findViewById<RadioGroup>(R.id.rgLatency)
            val lat = when (rgLat.checkedRadioButtonId) {
                R.id.rbLow -> AppPrefs.LAT_LOW
                R.id.rbSafe -> AppPrefs.LAT_SAFE
                else -> AppPrefs.LAT_NORMAL
            }
            AppPrefs.setLatency(this, lat)

            val rgCh = findViewById<RadioGroup>(R.id.rgChannels)
            val ch = if (rgCh.checkedRadioButtonId == R.id.rbStereo) 2 else 1
            AppPrefs.setChannels(this, ch)

            val ip = findViewById<EditText>(R.id.etManualIp).text?.toString()?.trim().orEmpty()
            if (ip.isNotEmpty() && !NetUtil.isPrivateIp(ip)) {
                Toast.makeText(this, R.string.invalid_ip, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AppPrefs.setManualIp(this, ip)

            val rgLang = findViewById<RadioGroup>(R.id.rgLang)
            val lang = when (rgLang.checkedRadioButtonId) {
                R.id.rbLangEn -> "en"
                R.id.rbLangUr -> "ur"
                else -> "system"
            }
            val old = AppPrefs.lang(this)
            AppPrefs.setLang(this, lang)

            Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
            if (lang != old) {
                recreate()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
