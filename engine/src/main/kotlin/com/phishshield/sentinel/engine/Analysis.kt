package com.phishshield.sentinel.engine

enum class RiskLevel(val label: String) {
    HIGH("High risk"), REVIEW("Review recommended"),
    NO_OBVIOUS_THREAT("No obvious threat detected"), UNKNOWN("Unable to assess fully")
}
enum class AnalysisStatus { COMPLETE, LIMITED, NOT_ANALYSED }

data class Signal(val id: String, val title: String, val explanation: String,
    val evidence: String, val weight: Int)
data class UrlFinding(val original: String, val host: String?, val signals: List<Signal>,
    val limitation: String? = null)
data class AnalysisResult(val level: RiskLevel, val status: AnalysisStatus,
    val ruleScore: Int?, val signals: List<Signal>, val urls: List<UrlFinding>,
    val limitations: List<String>, val nextAction: String)

/** Deliberately small, injectable examples; not an exhaustive brand directory or reputation list. */
data class Brand(val name: String, val domains: Set<String>)
object BrandExamples {
    val entries = listOf(
        Brand("PayPal", setOf("paypal.com", "paypalobjects.com")),
        Brand("SBI", setOf("sbi.bank.in", "sbi.co.in", "onlinesbi.sbi", "onlinesbi.com")),
        Brand("Google", setOf("google.com", "accounts.google.com", "googleapis.com", "googleusercontent.com", "gstatic.com")),
        Brand("Microsoft", setOf("microsoft.com", "live.com", "microsoftonline.com", "office.com")),
    )
}
