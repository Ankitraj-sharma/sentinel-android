package com.phishshield.sentinel.engine

import java.io.File
import java.security.MessageDigest

/** Independently authored synthetic holdout; no rule tuning against this file. */
object Stage7Evaluation {
    @JvmStatic fun main(args: Array<String>) {
        val bytes = javaClass.getResourceAsStream("/${args.getOrElse(2) { "stage7-holdout.tsv" }}")!!.use { it.readBytes() }
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        val cases = bytes.toString(Charsets.UTF_8).lineSequence().drop(1).filter { it.isNotBlank() }.map {
            val f = it.split('\t', limit = 3); require(f.size == 3 && f[1] in setOf("SCAM", "BENIGN"))
            PrototypeEvaluation.Case(f[0], f[1], f[2])
        }.toList()
        require(cases.map { it.id }.distinct().size == cases.size)
        val engine = ThreatEngine()
        val observations = cases.map { PrototypeEvaluation.Observation(it, engine.analyse(it.text)) }
        val report = buildString {
            appendLine("# Stage 7 synthetic holdout — ${args[1]}")
            appendLine("SHA256: `$hash`")
            appendLine("Independent author did not inspect rules/tests. Small synthetic English set, not representative real-world accuracy. No tuning against holdout results. Default rules engine only; bundled historical index not loaded.")
            if (args[1] == "final") appendLine("This original set was reused after its first results were exposed to review; the final rerun is a diagnostic, not an unseen final evaluation.")
            appendLine("Manual/shared text: full fixtures. Notification: same readable text projected through engine, not Android delivery/permission/extraction tests. QR: decoded URL destinations only; no camera/image decoding test.")
            for ((route, items) in listOf("Manual/shared" to observations, "Notification engine projection" to observations,
                "Decoded QR URL only" to cases.filter { it.text.startsWith("https://") && !it.text.contains(' ') }.map { PrototypeEvaluation.Observation(it, engine.analyse(it.text)) })) {
                val tp = items.count { it.case.label == "SCAM" && it.warned }
                val fp = items.count { it.case.label == "BENIGN" && it.warned }
                val scams = items.count { it.case.label == "SCAM" }; val benign = items.count { it.case.label == "BENIGN" }
                fun ratio(n: Int, d: Int) = if (d == 0) "N/A" else "$n/$d (${"%.1f".format(java.util.Locale.ROOT, n * 100.0 / d)}%)"
                appendLine("## $route")
                appendLine("Cases ${items.size}; TP $tp; FP $fp; missed scams including abstentions ${scams-tp}; abstentions ${items.count { it.abstained }}.")
                appendLine("Precision ${ratio(tp,tp+fp)}; recall ${ratio(tp,scams)}; false-positive rate ${ratio(fp,benign)}.")
            }
            appendLine("## All observations")
            appendLine("|ID|Label|Risk|Status|Rules|"); appendLine("|---|---|---|---|---|")
            observations.forEach { appendLine("|${it.case.id}|${it.case.label}|${it.result.level}|${it.result.status}|${it.result.signals.joinToString { s -> s.id }}|") }
        }
        File(args[0]).apply { parentFile.mkdirs(); writeText(report) }
        println("Wrote evaluation with frozen dataset hash $hash")
    }
}
