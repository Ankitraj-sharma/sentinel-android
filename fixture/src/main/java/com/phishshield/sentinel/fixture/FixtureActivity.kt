package com.phishshield.sentinel.fixture

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Separate debug-only publisher. All content is fixed synthetic data, never real messages. */
class FixtureActivity : Activity() {
    private val manager by lazy { getSystemService(NotificationManager::class.java) }
    private lateinit var status: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        manager.createNotificationChannel(NotificationChannel("demo", "Synthetic previews", NotificationManager.IMPORTANCE_DEFAULT))
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 48, 32, 32) }
        layout.addView(TextView(this).apply { text = "SENTINEL · SYNTHETIC DEMO\nSeparate publisher to verify real Android notification delivery. Enable Synthetic demo source in Sentinel's debug build. These samples do not measure detection accuracy."; textSize = 18f })
        status = TextView(this); layout.addView(status)
        layout.addView(Button(this).apply { text = "Allow demo notifications"; setOnClickListener {
            if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 77)
        } })
        for (kind in listOf("scam", "benign", "shortener", "updated", "hidden", "group", "ongoing")) {
            layout.addView(Button(this).apply { text = "Post $kind preview"; setOnClickListener { post(kind) } })
        }
        setContentView(layout)
        intent.getStringExtra("kind")?.let(::post)
    }
    private fun post(kind: String) {
        val body = when (kind) {
            "scam" -> "Send your OTP to our agent immediately https://paypal.com.evil.test"
            "updated" -> "PayPal account suspended. Act now https://paypal.com.evil.test"
            "shortener" -> "https://bit.ly/demo"
            "hidden" -> ""
            else -> "Your appointment is tomorrow at 10."
        }
        val builder = Notification.Builder(this, "demo").setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Synthetic preview").setContentText(body)
        if (kind == "group") builder.setGroup("demo_group").setGroupSummary(true)
        if (kind == "ongoing") builder.setOngoing(true)
        try { manager.notify(42, builder.build()); status.text = "Posted $kind · id 42 (repeated identical content tests deduplication)" }
        catch (_: SecurityException) { status.text = "Grant demo notification permission first." }
    }
}
