package com.phishshield.sentinel.engine

/** Combination rules: a topic or an isolated keyword is insufficient evidence. */
internal object EnglishScamRules {
    private data class Rule(val id: String, val title: String, val explanation: String,
        val topic: Regex, val action: Regex, val weight: Int)
    private fun pattern(value: String) = Regex(value, RegexOption.IGNORE_CASE)
    private val rules = listOf(
        Rule("task-earnings-deposit", "Payment demanded to release task earnings",
            "A work or task offer asks you to pay before withdrawing earnings. Verify independently before paying.",
            pattern("\\b(?:tasks?|earnings|commission|salary|job)\\b"),
            pattern("\\b(?:pay|deposit|transfer)\\b[^.!?\\n]{0,70}\\b(?:unlock|withdraw|release)\\b[^.!?\\n]{0,40}\\b(?:earnings|salary|commission|balance)\\b"), 40),
        Rule("investment-return", "Unrealistic investment return promise",
            "An investment or money request is paired with a rapid or guaranteed return promise. Verify the offer independently.",
            pattern("\\b(?:invest(?:ment)?|trading|crypto|money|returns?|profit)\\b"),
            pattern("\\b(?:double your (?:money|investment)|guaranteed (?:returns?|profit)|risk[ -]free (?:returns?|profit)|\\d{2,3}% (?:daily|per day)|double (?:it|money) (?:today|tomorrow))\\b"), 65),
        Rule("gift-card-request", "Gift card purchase and code request",
            "Buying gift cards and sending their codes is a common payment scam pattern. Verify the person through a known contact.",
            pattern("\\b(?:buy|purchase|get)\\b[^.!?\\n]{0,80}\\bgift[ -]?cards?\\b"),
            pattern("\\b(?:send|share|text|email|reply|give)\\b[^.!?\\n]{0,80}\\b(?:codes?|numbers?|pins?|pictures?|photos?)\\b"), 65),
        Rule("delivery-action", "Delivery problem with a linked action",
            "A parcel or delivery problem directs you to a link to confirm details or make a payment. Check your order in the carrier's official app.",
            pattern("\\b(?:parcel|package|delivery|shipment)\\b[^.!?\\n]{0,70}\\b(?:delayed|held|failed|undeliverable|pending|on hold)\\b"),
            pattern("\\b(?:confirm|verify|update|pay|reschedule)\\b"), 35),
        Rule("refund-action", "Refund claim with a linked action",
            "A tax or government refund claim asks you to follow a link. Verify eligibility directly through the official service.",
            pattern("\\b(?:tax|government|irs|income tax)\\s+refund\\b"),
            pattern("\\b(?:ready|claim|collect|eligible|approved|verify|confirm)\\b"), 35),
        Rule("remote-access", "Request to install remote access software",
            "A support or financial claim asks you to install remote control software. This can expose your device and accounts.",
            pattern("\\b(?:bank|refund|support|account|payment)\\b"),
            pattern("\\b(?:install|download)\\b[^.!?\\n]{0,70}\\b(?:anydesk|teamviewer|remote (?:access|control))\\b"), 65),
        Rule("job-deposit", "Payment requested to obtain a job",
            "An employment offer asks for an upfront deposit or recruitment fee. Verify the employer and payment purpose independently.",
            pattern("\\b(?:job|recruitment|employment|work from home)\\b"),
            pattern("\\b(?:pay|send|transfer|deposit)\\b[^.!?\\n]{0,70}\\b(?:registration fee|security deposit|recruitment fee|joining fee)\\b"), 40),
    )

    fun inspect(text: String): List<Signal> = rules.mapNotNull { rule ->
        // Bound context to a paragraph so unrelated messages cannot combine into one warning.
        text.split('\n').firstNotNullOfOrNull { paragraph ->
            val topic = rule.topic.find(paragraph) ?: return@firstNotNullOfOrNull null
            val action = rule.action.find(paragraph) ?: return@firstNotNullOfOrNull null
            if (rule.id in setOf("delivery-action", "refund-action") &&
                !Regex("(?i)(?:https?://|www\\.|\\b[a-z0-9-]+\\.[a-z]{2,}(?:/|\\b))").containsMatchIn(paragraph)) return@firstNotNullOfOrNull null
            val beforeAction = paragraph.substring(0, action.range.first).takeLast(60)
            val beforeTopic = paragraph.substring(0, topic.range.first).takeLast(60)
            val negative = pattern("\\b(?:never|do not|don't)\\s+(?:(?:ask(?:\\s+(?:you|anyone|customers))?\\s+to|ever|please)\\s+)?$")
            if (negative.containsMatchIn(beforeAction) || negative.containsMatchIn(beforeTopic)) return@firstNotNullOfOrNull null
            Signal(rule.id, rule.title, rule.explanation, "${topic.value} / ${action.value}", rule.weight)
        }
    }
}
