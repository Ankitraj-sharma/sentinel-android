# Prototype evaluation v1

Synthetic development diagnostic for prototype rule set v1. All 40 examples are authored fixtures, not real user data or a representative independent test set. Labels describe each synthetic scenario; wording alone may be ambiguous. These fixtures are visible during development and overlap rule design. No production accuracy, generalisation, calibration or hackathon score is established.

Warning = HIGH or REVIEW. Negative = NO_OBVIOUS_THREAT. Abstention = UNKNOWN or NOT_ANALYSED. OUT_OF_SCOPE cases are reported separately and excluded from phishing precision/recall denominators. Recall includes abstained SCAM cases in its denominator.

| Diagnostic | Observed result |
|---|---|
| Authored cases | 40: 16 SCAM, 16 BENIGN, 8 OUT_OF_SCOPE |
| True warnings / false warnings | 12 / 3 |
| Misses with negative verdict / true negatives | 4 / 13 |
| Labelled SCAM/BENIGN abstentions | 0 |
| Warning precision on authored labels | 80.0% (12/15) |
| Warning recall on authored SCAM labels | 75.0% (12/16) |
| Decision coverage on SCAM/BENIGN | 100.0% (32/32) |
| OUT_OF_SCOPE abstentions | 100.0% (8/8) |

## Every observed case

| ID | Authored label | Observed level | Coverage status | Rules |
|---|---|---|---|---|
| s01 | SCAM | HIGH | COMPLETE | credential-request, urgency, brand-domain-mismatch |
| s02 | SCAM | HIGH | COMPLETE | urgency, account-threat, brand-domain-mismatch |
| s03 | SCAM | HIGH | COMPLETE | credential-request, brand-domain-mismatch |
| s04 | SCAM | HIGH | COMPLETE | credential-request, urgency |
| s05 | SCAM | HIGH | COMPLETE | prize-payment, advance-fee |
| s06 | SCAM | HIGH | COMPLETE | prize-payment, advance-fee |
| s07 | SCAM | HIGH | COMPLETE | url-userinfo, brand-domain-mismatch |
| s08 | SCAM | HIGH | COMPLETE | url-userinfo, brand-domain-mismatch |
| s09 | SCAM | REVIEW | COMPLETE | credential-request |
| s10 | SCAM | HIGH | COMPLETE | urgency, account-threat, brand-domain-mismatch |
| s11 | SCAM | REVIEW | COMPLETE | account-threat, brand-domain-mismatch |
| s12 | SCAM | HIGH | COMPLETE | credential-request, urgency |
| s13 | SCAM | NO_OBVIOUS_THREAT | COMPLETE | none |
| s14 | SCAM | NO_OBVIOUS_THREAT | COMPLETE | none |
| s15 | SCAM | NO_OBVIOUS_THREAT | COMPLETE | none |
| s16 | SCAM | NO_OBVIOUS_THREAT | COMPLETE | none |
| b01 | BENIGN | NO_OBVIOUS_THREAT | COMPLETE | none |
| b02 | BENIGN | NO_OBVIOUS_THREAT | COMPLETE | none |
| b03 | BENIGN | NO_OBVIOUS_THREAT | COMPLETE | none |
| b04 | BENIGN | NO_OBVIOUS_THREAT | COMPLETE | none |
| b05 | BENIGN | NO_OBVIOUS_THREAT | COMPLETE | none |
| b06 | BENIGN | NO_OBVIOUS_THREAT | COMPLETE | none |
| b07 | BENIGN | NO_OBVIOUS_THREAT | COMPLETE | none |
| b08 | BENIGN | NO_OBVIOUS_THREAT | COMPLETE | none |
| b09 | BENIGN | NO_OBVIOUS_THREAT | COMPLETE | urgency |
| b10 | BENIGN | NO_OBVIOUS_THREAT | COMPLETE | none |
| b11 | BENIGN | REVIEW | COMPLETE | unencrypted-url, numeric-host |
| b12 | BENIGN | HIGH | COMPLETE | credential-request, urgency |
| b13 | BENIGN | REVIEW | COMPLETE | brand-domain-mismatch |
| b14 | BENIGN | NO_OBVIOUS_THREAT | COMPLETE | account-threat |
| b15 | BENIGN | NO_OBVIOUS_THREAT | COMPLETE | none |
| b16 | BENIGN | NO_OBVIOUS_THREAT | COMPLETE | none |
| u01 | OUT_OF_SCOPE | UNKNOWN | LIMITED | none |
| u02 | OUT_OF_SCOPE | UNKNOWN | LIMITED | none |
| u03 | OUT_OF_SCOPE | UNKNOWN | LIMITED | none |
| u04 | OUT_OF_SCOPE | UNKNOWN | LIMITED | none |
| u05 | OUT_OF_SCOPE | UNKNOWN | LIMITED | none |
| u06 | OUT_OF_SCOPE | UNKNOWN | LIMITED | none |
| u07 | OUT_OF_SCOPE | UNKNOWN | LIMITED | none |
| u08 | OUT_OF_SCOPE | UNKNOWN | LIMITED | none |

Misses and false alarms are retained rather than removed or relabelled. Unsupported URL destinations, language coverage, context interpretation and brand-directory gaps need a broader independently labelled evaluation in Stages 7–8. No rule thresholds were tuned in Stage 6 to improve these numbers.
