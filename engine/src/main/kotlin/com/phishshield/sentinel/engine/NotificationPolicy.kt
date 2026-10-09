package com.phishshield.sentinel.engine

import java.security.MessageDigest

data class NotificationPolicy(val enabled: Boolean, val packages: Set<String>) {
    fun accepts(owner: String, self: String, groupSummary: Boolean, ongoing: Boolean): Boolean =
        enabled && owner != self && owner in packages && !groupSummary && !ongoing
}

/** Bounded, in-memory content digests. Same-key updates with changed content are not suppressed. */
class NotificationDeduplicator(private val capacity: Int = 128) {
    private val seen = LinkedHashMap<String, String>()
    init { require(capacity > 0) }
    fun duplicate(key: String, content: String): Boolean {
        val digest = MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        if (seen[key] == digest) return true
        seen.remove(key)
        seen[key] = digest
        while (seen.size > capacity) seen.remove(seen.keys.first())
        return false
    }
    fun clear() = seen.clear()
}
