package com.phishshield.sentinel.engine

import java.io.DataInputStream
import java.io.InputStream
import java.net.IDN
import java.net.URI
import java.security.MessageDigest
import java.util.Locale

enum class ListedThreat(val description: String) {
    PHISHING("phishing"), MALWARE("malware"), DEFACEMENT("website defacement")
}

/** Historical exact-URL evidence, never a live reputation service or a domain-wide blocklist. */
fun interface UrlThreatLookup {
    fun find(url: String): ListedThreat?
}

class UrlThreatIndex private constructor(private val records: ByteArray) : UrlThreatLookup {
    override fun find(url: String): ListedThreat? {
        val canonical = canonicalUrl(url) ?: return null
        val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
        var low = 0
        var high = records.size / 33 - 1
        while (low <= high) {
            val middle = (low + high).ushr(1)
            val offset = middle * 33
            var comparison = 0
            for (i in digest.indices) {
                comparison = (records[offset + i].toInt() and 255) - (digest[i].toInt() and 255)
                if (comparison != 0) break
            }
            when {
                comparison < 0 -> low = middle + 1
                comparison > 0 -> high = middle - 1
                else -> return ListedThreat.entries[records[offset + 32].toInt()]
            }
        }
        return null
    }

    companion object {
        fun read(input: InputStream): UrlThreatIndex = DataInputStream(input).use { stream ->
            require(stream.readInt() == 0x53554931) { "Invalid URL index" }
            val count = stream.readInt()
            require(count in 1..1_000_000)
            val bytes = ByteArray(count * 33)
            stream.readFully(bytes)
            require(stream.read() == -1)
            for (i in 0 until count) {
                require(bytes[i * 33 + 32].toInt() in ListedThreat.entries.indices)
                if (i > 0) {
                    var order = 0
                    for (j in 0..31) {
                        order = (bytes[(i - 1) * 33 + j].toInt() and 255) - (bytes[i * 33 + j].toInt() and 255)
                        if (order != 0) break
                    }
                    require(order < 0) { "Unsorted URL index" }
                }
            }
            UrlThreatIndex(bytes)
        }

        /** Preserve path/query case and escaping. Only scheme, host, default port and fragment normalise. */
        fun canonicalUrl(raw: String): String? { return try {
            val value = raw.trim()
            val uri = URI(if (value.contains("://")) value else "https://$value")
            if (uri.scheme.lowercase(Locale.ROOT) !in setOf("http", "https") || uri.rawUserInfo != null) null
            else {
                val host = uri.host ?: return null
                val ascii = if (host.startsWith('[')) host.lowercase(Locale.ROOT)
                    else IDN.toASCII(host.removeSuffix(".")).lowercase(Locale.ROOT)
                val port = if (uri.port == -1 || uri.scheme.equals("https", true) && uri.port == 443 ||
                    uri.scheme.equals("http", true) && uri.port == 80) "" else ":${uri.port}"
                ascii + port + uri.rawPath.ifEmpty { "/" } + (uri.rawQuery?.takeIf { it.isNotEmpty() }?.let { "?$it" } ?: "")
            }
        } catch (_: Exception) { null } }
    }
}
