package com.phishshield.sentinel.engine

import java.io.File
import java.util.Locale

/** Transparent synthetic development diagnostic, deliberately including misses and false alarms. */
object PrototypeEvaluation {
    data class Case(val id: String, val label: String, val text: String)
    data class Observation(val case: Case, val result: AnalysisResult) {
        val warned get() = result.level == RiskLevel.HIGH || result.level == RiskLevel.REVIEW
        val abstained get() = result.level == RiskLevel.UNKNOWN || result.status == AnalysisStatus.NOT_ANALYSED
    }
    fun cases(): List<Case> = javaClass.getResourceAsStream("/prototype-evaluation-v1.tsv")!!.bufferedReader().useLines { lines ->
        lines.drop(1).filter { it.isNotBlank() }.map { line ->
            val fields = line.split('\t', limit = 3); require(fields.size == 3)
            require(fields[1] in setOf("SCAM", "BENIGN", "OUT_OF_SCOPE"))
            Case(fields[0], fields[1], fields[2])
        }.toList()
    }
    fun observe(): List<Observation> = cases().map { Observation(it, ThreatEngine().analyse(it.text)) }
    private fun percentage(n: Int, d: Int) = if (d == 0) "N/A" else String.format(Locale.ROOT, "%.1f%% (%d/%d)", n * 100.0 / d, n, d)
    fun report(observations: List<Observation>): String {
        val scams = observations.filter { it.case.label == "SCAM" }
        val benign = observations.filter { it.case.label == "BENIGN" }
        val out = observations.filter { it.case.label == "OUT_OF_SCOPE" }
        val tp = scams.count { it.warned }; val fp = benign.count { it.warned }
        val fn = scams.count { !it.warned && !it.abstained }; val tn = benign.count { !it.warned && !it.abstained }
        val abstentions = (scams + benign).count { it.abstained }
        return buildString {
            appendLine("# Prototype evaluation v1")
            appendLine()
            appendLine("Synthetic development diagnostic for prototype rule set v1. All ${observations.size} examples are authored fixtures, not real user data or a representative independent test set. Labels describe each synthetic scenario; wording alone may be ambiguous. These fixtures are visible during development and overlap rule design. No production accuracy, generalisation, calibration or hackathon score is established.")
            appendLine()
            appendLine("Warning = HIGH or REVIEW. Negative = NO_OBVIOUS_THREAT. Abstention = UNKNOWN or NOT_ANALYSED. OUT_OF_SCOPE cases are reported separately and excluded from phishing precision/recall denominators. Recall includes abstained SCAM cases in its denominator.")
            appendLine()
            appendLine("| Diagnostic | Observed result |")
            appendLine("|---|---|")
            appendLine("| Authored cases | ${observations.size}: ${scams.size} SCAM, ${benign.size} BENIGN, ${out.size} OUT_OF_SCOPE |")
            appendLine("| True warnings / false warnings | $tp / $fp |")
            appendLine("| Misses with negative verdict / true negatives | $fn / $tn |")
            appendLine("| Labelled SCAM/BENIGN abstentions | $abstentions |")
            appendLine("| Warning precision on authored labels | ${percentage(tp, tp + fp)} |")
            appendLine("| Warning recall on authored SCAM labels | ${percentage(tp, scams.size)} |")
            appendLine("| Decision coverage on SCAM/BENIGN | ${percentage(scams.size + benign.size - abstentions, scams.size + benign.size)} |")
            appendLine("| OUT_OF_SCOPE abstentions | ${percentage(out.count { it.abstained }, out.size)} |")
            appendLine()
            appendLine("## Every observed case")
            appendLine()
            appendLine("| ID | Authored label | Observed level | Coverage status | Rules |")
            appendLine("|---|---|---|---|---|")
            observations.forEach { appendLine("| ${it.case.id} | ${it.case.label} | ${it.result.level} | ${it.result.status} | ${it.result.signals.joinToString { signal -> signal.id }.ifEmpty { "none" }} |") }
            appendLine()
            appendLine("Misses and false alarms are retained rather than removed or relabelled. Unsupported URL destinations, language coverage, context interpretation and brand-directory gaps need a broader independently labelled evaluation in Stages 7–8. No rule thresholds were tuned in Stage 6 to improve these numbers.")
        }
    }
    @JvmStatic fun main(args: Array<String>) {
        val output = File(args.single()); output.parentFile.mkdirs(); output.writeText(report(observe()))
        println("Wrote synthetic prototype evaluation: ${output.path}")
    }
}
