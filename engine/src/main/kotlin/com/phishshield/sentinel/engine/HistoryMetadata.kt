package com.phishshield.sentinel.engine

enum class HistorySource(val label: String) {
    MANUAL("Manual input"), SHARED_TEXT("Shared text"), QR_CAMERA("QR camera"), QR_IMAGE("QR image"),
    WHATSAPP("WhatsApp preview"), GMAIL("Gmail preview"), SMS("SMS preview"), SYNTHETIC_DEMO("Synthetic demo preview")
}
enum class HistoryRule(val code: String, val label: String) {
    SECRET("credential-request", "Request to disclose a secret"), URGENCY("urgency", "Pressure to act quickly"),
    ACCOUNT("account-threat", "Account restriction threat"), PRIZE("prize-payment", "Payment linked to a prize"),
    FEE("advance-fee", "Fee requested before a benefit"), USERINFO("url-userinfo", "Destination obscured by user information"),
    HTTP("unencrypted-url", "Unencrypted connection"), NUMERIC("numeric-host", "Numeric destination"),
    BRAND("brand-domain-mismatch", "Possible brand impersonation")
}
/** No free-form text or destination field: raw content cannot enter persisted metadata. */
data class HistoryRecord(val id: String, val timeMillis: Long, val source: HistorySource,
    val level: RiskLevel, val status: AnalysisStatus, val score: Int?, val rules: List<HistoryRule>)

object HistoryPolicy {
    const val MAX_ENTRIES = 100
    const val RETENTION_MILLIS = 7L * 24 * 60 * 60 * 1000
    fun metadata(id: String, time: Long, source: HistorySource, result: AnalysisResult): HistoryRecord? {
        if (result.status == AnalysisStatus.NOT_ANALYSED) return null
        return HistoryRecord(id, time, source, result.level, result.status, result.ruleScore,
            result.signals.mapNotNull { signal -> HistoryRule.entries.find { it.code == signal.id } }.distinct())
    }
    fun retain(records: List<HistoryRecord>, now: Long): List<HistoryRecord> = records
        .filter { it.timeMillis > now - RETENTION_MILLIS && it.timeMillis <= now }
        .sortedByDescending { it.timeMillis }.distinctBy { it.id }.take(MAX_ENTRIES)
}
