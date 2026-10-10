package com.phishshield.sentinel.engine

/** Official stable clients only; do not accept package prefixes or third-party clients. */
object TelegramSources {
    val packages: Set<String> = setOf("org.telegram.messenger", "org.telegram.messenger.web")
}
