package com.phishshield.sentinel.engine

/** Deterministic presentation of observed findings; never invents new evidence or certainty. */
data class Explanation(val summary: String, val coverage: String, val scoreMeaning: String,
    val verificationSteps: List<String>)

fun AnalysisResult.explain(): Explanation = Explanation(
    summary = when (level) {
        RiskLevel.HIGH -> "Strong configured warning signals matched. Pause and verify the request independently."
        RiskLevel.REVIEW -> "Some configured warning signals matched. Inspect the evidence before acting."
        RiskLevel.NO_OBVIOUS_THREAT -> "No strong configured warning signals matched. This does not establish that the message or destination is safe."
        RiskLevel.UNKNOWN -> "The available content could not be fully assessed. Missing evidence is not evidence of safety."
    },
    coverage = when (status) {
        AnalysisStatus.COMPLETE -> "Supported rules checked · Website and sender unverified"
        AnalysisStatus.LIMITED -> "Partial coverage · Some content remains unverified"
        AnalysisStatus.NOT_ANALYSED -> "Not analysed · Correct the input and try again"
    },
    scoreMeaning = if (ruleScore == null) "No score was produced because the input was not analysed."
        else "Rule points combine the strongest match per rule and are capped at 100. They are not a probability, confidence estimate or measured accuracy.",
    verificationSteps = if (status == AnalysisStatus.NOT_ANALYSED) listOf(
        "Resolve the input limitation shown below, then analyse again."
    ) else listOf(
        "Open the organisation's official app yourself, or use a contact you already know. Avoid contact details supplied by this message.",
        "Confirm the request there before replying, paying or sharing information. Do not disclose an OTP, password, PIN or CVV to someone who asks for it.",
        "Compare the full destination host below with the organisation's known domain. A familiar name in the link does not verify its destination."
    ).let { steps -> if (urls.isEmpty()) steps.dropLast(1) else steps },
)

/** Prevent invisible control characters from disguising raw evidence in result views. */
fun visibleEvidence(value: String): String = buildString {
    value.codePoints().forEach { code ->
        val type = Character.getType(code)
        if (type == Character.FORMAT.toInt() || type == Character.CONTROL.toInt() && code != 10 && code != 9) {
            append("⟦U+").append(code.toString(16).uppercase().padStart(4, '0')).append("⟧")
        } else appendCodePoint(code)
    }
}
