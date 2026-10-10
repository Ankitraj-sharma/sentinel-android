package com.phishshield.sentinel.engine

import java.net.IDN
import java.net.URI
import java.text.Normalizer
import java.util.Locale

/** Pure local rules: never resolves a host, fetches a URL, or calls a remote model. */
class ThreatEngine(private val brands: List<Brand> = BrandExamples.entries,
    private val threatLookup: UrlThreatLookup = UrlThreatLookup { null }) {
    companion object {
        const val MAX_INPUT = 20_000
        const val MAX_URLS = 16
        private val links = Regex("(?i)\\b(?:https?://|www\\.)[^\\s<>\"']+")
        private val bareHost = Regex("(?i)^(?:[\\p{L}\\p{N}-]+\\.)+(?:[\\p{L}]{2,63}|\\d{1,3})(?::\\d{1,5})?(?:[/?#][^\\s]*)?$")
        private val inlineDomains = Regex("(?i)(?<![\\p{L}\\p{N}@./-])(?:[\\p{L}\\p{N}](?:[\\p{L}\\p{N}-]{0,61}[\\p{L}\\p{N}])?\\.)+[\\p{L}]{2,63}(?![\\p{L}\\p{N}.-])(?::\\d{1,5})?(?:[/?#][^\\s<>\"']*)?")
        private val scheme = Regex("(?i)^[a-z][a-z0-9+.-]*:")
        private val request = Regex("(?i)\\b(?:send|share|give|tell|reply(?: with)?|provide)\\b(?:\\s+(?!(?:do|don't|never|not|then|and|but)\\b)[\\p{L}\\p{N}'-]+){0,6}\\s+\\b(?:otp|one[ -]time password|password|pin|cvv|verification code|login code)\\b")
        private val urgency = Regex("(?i)\\b(?:urgent|immediately|act now|last chance|within \\d+ (?:minutes?|hours?))\\b")
        private val accountThreat = Regex("(?i)\\b(?:account|banking|card|wallet)\\b[^.!?\\n]{0,40}\\b(?:blocked|suspended|disabled|closed)\\b")
        private val payment = Regex("(?i)\\b(?:pay|transfer|send money|deposit|processing fee|refund fee)\\b")
        private val prize = Regex("(?i)\\b(?:won|winner|lottery|prize)\\b")
        private val advanceFee = Regex("(?i)\\b(?:processing fee|refund fee|release fee|activation fee)\\b")
        private val negation = Regex("(?i)\\b(?:do not|don't|never|not)\\s+(?:(?:ask(?:\\s+(?:you|anyone|customers))?\\s+to|ever|please)\\s+)?$")
        private val shorteners = setOf("bit.ly", "tinyurl.com", "t.co", "is.gd", "ow.ly")
        private val offlineLimit = "Offline analysis cannot verify current website reputation, redirects, page content or sender identity. A low score does not prove safety."
    }

    fun analyse(input: String): AnalysisResult {
        if (input.isBlank()) return rejected("Enter a message or HTTP(S) URL to analyse.")
        if (input.length > MAX_INPUT) return rejected("Input exceeds $MAX_INPUT characters. Nothing was scanned; split the content into smaller messages.")
        val text = Normalizer.normalize(input, Normalizer.Form.NFKC)
            .replace(Regex("\\p{Cf}"), "")
        val signals = mutableListOf<Signal>()
        fun add(id: String, title: String, reason: String, match: MatchResult?, weight: Int) {
            if (match != null) signals += Signal(id, title, reason, match.value, weight)
        }
        val recoveryRequest = Regex("(?i)\\b(?:send|share|provide|give)\\b[^.!?\\n]{0,55}\\b(?:seed phrase|recovery phrase|private key)\\b")
        val credential = request.findAll(text).firstOrNull { !isNegated(text, it.range.first) }
            ?: recoveryRequest.findAll(text).firstOrNull { !isNegated(text, it.range.first) }
        add("credential-request", "Request to disclose a secret",
            "The message asks you to disclose an authentication secret. Verify the request through the official service.", credential, 55)
        val pressure = urgency.findAll(text).firstOrNull { !isNegated(text, it.range.first) }
        add("urgency", "Pressure to act quickly", "Time pressure can discourage verification. It is a weak signal on its own.", pressure, 15)
        val threat = accountThreat.findAll(text).firstOrNull { !isNegated(text, it.range.first) }
        add("account-threat", "Account restriction threat", "The message threatens account restrictions. Verify independently before responding.", threat, 15)
        val money = payment.findAll(text).firstOrNull { !isNegated(text, it.range.first) }
        val winnings = prize.find(text)
        if (money != null && winnings != null) {
            signals += Signal("prize-payment", "Payment linked to a prize", "A prize claim is combined with a payment request. Check independently before paying.", "${winnings.value} / ${money.value}", 40)
        }
        add("advance-fee", "Fee requested before a benefit", "An upfront fee is mentioned. Confirm the purpose and recipient independently.",
            advanceFee.findAll(text).firstOrNull { !isNegated(text, it.range.first) }, 25)

        EnglishScamRules.inspect(text).forEach { signals += it }

        val normalisationChanged = text != input
        val normalisedLinks = links.findAll(text).toList()
        val explicit = (links.findAll(input) + if (normalisationChanged) normalisedLinks.asSequence() else emptySequence()).toList()
        val inline = inlineDomains.findAll(text).filter { candidate ->
            normalisedLinks.none { it.range.first <= candidate.range.first && it.range.last >= candidate.range.last }
        }
        val extracted = (explicit.asSequence().map { it.value } + inline.map { it.value }).map(::trimLink).distinct().toList()
        val trimmed = input.trim()
        // A pasted URL is not sentence prose: preserve valid quotes/punctuation in its path/query.
        val standalone = !trimmed.any { it.isWhitespace() } &&
            (bareHost.matches(trimmed) || scheme.containsMatchIn(trimmed))
        val rawUrls = if (standalone) listOf(trimmed) else extracted
        if (rawUrls.size > MAX_URLS) return rejected("More than $MAX_URLS links found. Nothing was scanned; analyse smaller batches.")
        val context = credential != null || threat != null || pressure != null || money != null
        val urls = rawUrls.map { inspectUrl(it, text, context) }
        // Repeat URLs/signals must not inflate a score. Keep one strongest contribution per rule.
        val combined = (signals + urls.flatMap { it.signals }).groupBy { it.id }
            .map { (_, matches) -> matches.maxBy { it.weight } }
        val score = combined.sumOf { it.weight }.coerceAtMost(100)
        val limits = mutableListOf(offlineLimit, "English message patterns and a small example brand directory are supported. This is a rule-based prototype, not a trained ML model.")
        if (normalisationChanged) limits += "Text matching normalises Unicode and removes invisible formatting; some evidence snippets show normalised text."
        limits += "Bare hostname extraction can also match filename-like text. It does not verify ownership or website contents."
        urls.mapNotNullTo(limits) { it.limitation }
        val letters = text.filter { it.isLetter() }
        val languageLimited = letters.isNotEmpty() && letters.count { it in 'a'..'z' || it in 'A'..'Z' } * 2 < letters.length
        if (languageLimited) limits += "Message language is outside the English rule coverage. A lack of matches is inconclusive."
        val incomplete = urls.any { it.host == null || it.limitation != null } || languageLimited
        val level = when {
            score >= 65 -> RiskLevel.HIGH
            score >= 25 -> RiskLevel.REVIEW
            incomplete -> RiskLevel.UNKNOWN
            else -> RiskLevel.NO_OBVIOUS_THREAT
        }
        return AnalysisResult(level, if (incomplete) AnalysisStatus.LIMITED else AnalysisStatus.COMPLETE,
            score, combined, urls, limits.distinct(), when (level) {
                RiskLevel.HIGH -> "Avoid responding or opening the link until you verify through the organisation's official app or a contact you already trust."
                RiskLevel.REVIEW -> "Inspect the destination and verify the request independently before sharing information or paying."
                RiskLevel.UNKNOWN -> "Provide a supported HTTP(S) URL or more context. Treat unanalysed content cautiously."
                RiskLevel.NO_OBVIOUS_THREAT -> "No strong configured indicators matched. Verify unexpected requests independently; this result is not a safety guarantee."
            })
    }

    private fun inspectUrl(raw: String, message: String, context: Boolean): UrlFinding {
        val value = if (raw.startsWith("www.", true) || bareHost.matches(raw)) "https://$raw" else raw
        val uri = try { URI(value) } catch (_: Exception) { return invalidUrl(raw, "Malformed URL; destination could not be parsed.") }
        if (!uri.scheme.equals("https", true) && !uri.scheme.equals("http", true)) return invalidUrl(raw, "Unsupported URL scheme. Only HTTP(S) destinations are inspected.")
        val authority = uri.rawAuthority ?: return invalidUrl(raw, "URL is missing a destination host.")
        if (authority.any { it.isWhitespace() || Character.getType(it) == Character.FORMAT.toInt() } || authority.contains('%')) return invalidUrl(raw, "Encoded or hidden characters in the authority prevent reliable host interpretation.")
        val hostPort = authority.substringAfterLast('@')
        val hostText = if (hostPort.startsWith('[')) {
            if (uri.host == null) return invalidUrl(raw, "Invalid numeric address.")
            hostPort.substringBefore(']') + "]"
        } else hostPort.substringBefore(':')
        val portSuffix = if (hostPort.startsWith('[')) hostPort.substringAfter(']', "") else hostPort.removePrefix(hostText)
        if (portSuffix.isNotEmpty() && (!portSuffix.startsWith(':') || portSuffix.drop(1).toIntOrNull() !in 1..65535)) return invalidUrl(raw, "Invalid URL port.")
        val host = try { if (hostText.startsWith('[')) hostText.lowercase(Locale.ROOT) else IDN.toASCII(hostText.removeSuffix("."), IDN.USE_STD3_ASCII_RULES).lowercase(Locale.ROOT) }
            catch (_: IllegalArgumentException) { return invalidUrl(raw, "Invalid destination hostname.") }
        if (host.isBlank() || host.length > 253 || host.contains("..")) return invalidUrl(raw, "Invalid destination hostname.")
        if (Regex("\\d+(?:\\.\\d+){3}").matches(host) && !isIpv4(host)) return invalidUrl(raw, "Invalid IPv4 address.")
        val signals = mutableListOf<Signal>()
        if (authority.contains('@')) signals += Signal("url-userinfo", "Destination obscured by user information", "Text before @ is user information; the actual destination is $host.", authority, 50)
        if (uri.scheme.equals("http", true)) signals += Signal("unencrypted-url", "Unencrypted connection", "HTTP does not encrypt the connection. This alone does not establish phishing.", raw, 10)
        if (host.startsWith('[') || isIpv4(host)) signals += Signal("numeric-host", "Numeric destination", "The destination uses an IP address rather than an organisation domain. It may also be a legitimate local service.", host, 10)
        threatLookup.find(value)?.let { known ->
            signals += Signal("listed-url", "URL appears in a historical threat dataset",
                "This exact destination was labelled ${known.description} in the bundled Kaggle dataset. This is historical evidence; the site's current state is not verified.", raw, 70)
        }
        val candidates = brands.filter { brand ->
                val name = brand.name.lowercase(Locale.ROOT)
                Regex("(?i)\\b${Regex.escape(brand.name)}\\b").containsMatchIn(message) ||
                    host.split('.').any { it.contains(name) || name.length >= 4 && nearBrand(it, name) }
        }
        for (candidate in candidates) {
            val matches = candidate.domains.any { host == it || host.endsWith(".$it") }
            if (!matches && (context || authority.contains('@') || host.split('.').any { it.contains(candidate.name.lowercase(Locale.ROOT)) || nearBrand(it, candidate.name.lowercase(Locale.ROOT)) })) {
                signals += Signal("brand-domain-mismatch", "Possible ${candidate.name} impersonation",
                    "The destination $host is outside the configured ${candidate.name} example domains. The directory is incomplete; this is a signal, not proof.", host, 35)
            }
        }
        val limitation = when {
            host in shorteners -> "Shortened destination $host cannot be expanded offline. Its final destination is unverified."
            host.startsWith("xn--") || host.contains(".xn--") -> "Internationalised destination $host requires careful inspection; Unicode domains are not inherently malicious."
            else -> null
        }
        return UrlFinding(raw, host, signals, limitation)
    }

    private fun nearBrand(part: String, brand: String): Boolean {
        if (brand.length < 4 || part.length != brand.length || part == brand) return false
        return part.zip(brand).count { (a, b) -> a != b } == 1
    }
    private fun isNegated(text: String, start: Int): Boolean {
        val prefix = text.substring((start - 60).coerceAtLeast(0), start)
            .substringAfterLast('.').substringAfterLast('!').substringAfterLast('?').substringAfterLast(';').substringAfterLast(',')
        return negation.containsMatchIn(prefix)
    }
    private fun isIpv4(host: String): Boolean = host.split('.').let { parts -> parts.size == 4 && parts.all { it.toIntOrNull() in 0..255 } }
    private fun trimLink(value: String): String {
        var result = value.trimEnd('.', ',', '!', ';')
        for ((open, close) in listOf('(' to ')', '[' to ']', '{' to '}')) {
            while (result.endsWith(close) && result.count { it == close } > result.count { it == open }) result = result.dropLast(1)
        }
        return result
    }
    private fun invalidUrl(raw: String, limitation: String) = UrlFinding(raw, null, emptyList(), limitation)
    private fun rejected(reason: String) = AnalysisResult(RiskLevel.UNKNOWN, AnalysisStatus.NOT_ANALYSED, null,
        emptyList(), emptyList(), listOf(reason), "Correct the input and analyse again.")
}
