package com.phishshield.sentinel.engine

object PreviewCompleteness {
    /** Visible cut-off marker only; absence cannot establish that a preview is complete. */
    fun hasCutOffMarker(text: String): Boolean = text.lineSequence().any {
        val line = it.trimEnd()
        line.endsWith("…") || line.endsWith("...")
    }
}
