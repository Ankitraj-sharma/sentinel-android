# Automatic alert recovery

The reported SMS contained a prize claim and `br-icloud.com.br`. The existing bundled-index engine regression and the additional exact-body assertion produce HIGH; this is historical dataset evidence, not verification of the site's current state.

Phone diagnostics showed the SMS source selected, Google Messages as default SMS owner, monitoring enabled, listener connected, and a previously analysed preview with an alert-disabled event. Alert permission was enabled when inspected. This establishes a delivery recovery gap, not proof of the complete sequence for both earlier attempts.

Failed alert posting now retains only the current in-memory warning token for retry. App resume and the alert-permission callback refresh system state and retry it when access, connection, monitoring and alerts permit delivery. Successful retry clears the pending token, so later resumes do not replay the same warning. A cleared/replaced warning or policy/lifecycle invalidation cannot resurrect its old token. Nothing is uploaded or persisted by this mechanism.

The user confirmed on the connected Vivo Android 13 phone that a fresh SMS from the second phone produced a Sentinel alert after the in-place update. Existing app settings and history were not reset.

Validation uses a separate temporary package via `-PisolatedValidation=true -PvalidationSuffix=alerttest`. Fixture routing resolves the installed package at runtime, avoiding stale Kotlin-inlined variant constants. Real sources remain disabled in synthetic tests. The pending-retry regression simulates a transient runtime connection gate and verifies a real OS repost plus no repeated retry; AppOps denial did not change notification-enabled state in this instrumented Vivo process, so a real permission-denial cycle is not claimed as automated evidence. Earlier routing and AppOps test failures are not counted as passes.

Process death loses the in-memory warning; already-active notifications are not replayed. Android controls source preview availability, listener delivery and heads-up/sound presentation. A connected listener alone cannot guarantee every notification will be received.

Final observed verification: 42 engine tests passed, including the supplied SMS body; isolated Vivo Android 13 instrumentation reported OK (23 tests) in 48.524 seconds. Final build succeeded. Temporary test/fixture/app packages were removed after verification. The final user app was updated in place, preserving data and grants.

