package com.phishshield.sentinel

import android.content.Context
import com.phishshield.sentinel.engine.ThreatEngine
import com.phishshield.sentinel.engine.UrlThreatIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Load the shared index once, off the UI thread. A corrupt/missing asset must not imply a clean scan. */
object LocalThreatEngine {
    @Volatile private var cached: ThreatEngine? = null
    suspend fun get(context: Context): ThreatEngine = withContext(Dispatchers.IO) {
        cached ?: synchronized(this@LocalThreatEngine) {
            cached ?: ThreatEngine(threatLookup = UrlThreatIndex.read(context.assets.open("url-threats-v1.bin")))
                .also { cached = it }
        }
    }
}
