package com.phishshield.sentinel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.phishshield.sentinel.data.HistoryRepository
import com.phishshield.sentinel.engine.*
import com.phishshield.sentinel.input.readSharedInput
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryProducerTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun waitFor(message: String, condition: () -> Boolean) {
        val until = android.os.SystemClock.elapsedRealtime() + 10_000
        while (!condition() && android.os.SystemClock.elapsedRealtime() < until) Thread.sleep(50)
        assertTrue(message, condition())
    }
    @Test fun actualImportedSourcesAndEditedContentReachMetadataThroughViewModel() = runBlocking {
        val app = instrumentation.targetContext.applicationContext as Application
        val history = HistoryRepository.get(app); history.awaitReady(); history.setEnabled(false); history.setEnabled(true)
        val store = ViewModelStore()
        lateinit var model: SentinelViewModel
        instrumentation.runOnMainSync { model = ViewModelProvider(store, ViewModelProvider.AndroidViewModelFactory.getInstance(app))[SentinelViewModel::class.java] }
        fun analyseAndVerify(source: HistorySource) {
            val previousCount = history.state.value.entries.size
            instrumentation.runOnMainSync { model.analyse() }
            waitFor("$source metadata persisted") { history.state.value.entries.size == previousCount + 1 && model.scan.value.historyNotice?.contains("Encrypted metadata saved") == true }
            assertEquals(source, history.state.value.entries.first().source)
        }
        try {
            val share = readSharedInput(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "Send OTP immediately https://paypal.com.evil.test"))!!
            instrumentation.runOnMainSync { model.importContent(share, InputSource.SHARED_TEXT) }
            assertTrue(history.state.value.entries.isEmpty())
            analyseAndVerify(HistorySource.SHARED_TEXT)
            instrumentation.runOnMainSync { model.importQr("https://paypal.com@evil.test/login") }
            analyseAndVerify(HistorySource.QR_CAMERA)
            instrumentation.runOnMainSync { model.importQrImage(Uri.parse("content://${instrumentation.context.packageName}.qr/valid")) }
            waitFor("image actually decoded") { !model.scan.value.busy && model.scan.value.source == InputSource.QR_IMAGE && model.scan.value.input == "https://paypal.com@evil.test/login" }
            analyseAndVerify(HistorySource.QR_IMAGE)
            instrumentation.runOnMainSync { model.importQr("https://paypal.com@evil.test/login"); model.updateInput("Your appointment is tomorrow.") }
            analyseAndVerify(HistorySource.MANUAL)
            assertFalse(history.state.value.entries.toString().contains("evil.test"))
        } finally { instrumentation.runOnMainSync { store.clear() }; history.setEnabled(false) }
    }
}
