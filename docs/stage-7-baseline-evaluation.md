# Stage 7 synthetic holdout — baseline
SHA256: `7a470e7f80b86550b6d6e552a698c0e4f174a9016e4bba52abf4eeda0528bec1`
Independent author did not inspect rules/tests. Small synthetic English set, not representative real-world accuracy. No tuning against holdout results. Default rules engine only; bundled historical index not loaded.
Manual/shared text: full fixtures. Notification: same readable text projected through engine, not Android delivery/permission/extraction tests. QR: decoded URL destinations only; no camera/image decoding test.
## Manual/shared
Cases 40; TP 5; FP 2; missed scams including abstentions 15; abstentions 0.
Precision 5/7 (71.4%); recall 5/20 (25.0%); false-positive rate 2/20 (10.0%).
## Notification engine projection
Cases 40; TP 5; FP 2; missed scams including abstentions 15; abstentions 0.
Precision 5/7 (71.4%); recall 5/20 (25.0%); false-positive rate 2/20 (10.0%).
## Decoded QR URL only
Cases 0; TP 0; FP 0; missed scams including abstentions 0; abstentions 0.
Precision N/A; recall N/A; false-positive rate N/A.
## All observations
|ID|Label|Risk|Status|Rules|
|---|---|---|---|---|
|H001|SCAM|NO_OBVIOUS_THREAT|COMPLETE||
|H002|SCAM|HIGH|COMPLETE|prize-payment, advance-fee|
|H003|SCAM|HIGH|COMPLETE|gift-card-request|
|H004|SCAM|NO_OBVIOUS_THREAT|COMPLETE||
|H005|SCAM|NO_OBVIOUS_THREAT|COMPLETE||
|H006|SCAM|NO_OBVIOUS_THREAT|COMPLETE||
|H007|SCAM|HIGH|COMPLETE|remote-access|
|H008|SCAM|NO_OBVIOUS_THREAT|COMPLETE||
|H009|SCAM|NO_OBVIOUS_THREAT|COMPLETE|urgency|
|H010|SCAM|NO_OBVIOUS_THREAT|COMPLETE||
|H011|SCAM|NO_OBVIOUS_THREAT|COMPLETE||
|H012|SCAM|NO_OBVIOUS_THREAT|COMPLETE||
|H013|SCAM|NO_OBVIOUS_THREAT|COMPLETE||
|H014|SCAM|REVIEW|COMPLETE|advance-fee|
|H015|SCAM|NO_OBVIOUS_THREAT|COMPLETE||
|H016|SCAM|REVIEW|COMPLETE|credential-request|
|H017|SCAM|NO_OBVIOUS_THREAT|COMPLETE||
|H018|SCAM|NO_OBVIOUS_THREAT|COMPLETE|urgency|
|H019|SCAM|NO_OBVIOUS_THREAT|COMPLETE||
|H020|SCAM|NO_OBVIOUS_THREAT|COMPLETE||
|H021|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|H022|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|H023|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|H024|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|H025|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|H026|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|H027|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|H028|BENIGN|HIGH|COMPLETE|remote-access|
|H029|BENIGN|REVIEW|COMPLETE|advance-fee|
|H030|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|H031|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|H032|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|H033|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|H034|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|H035|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|H036|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|H037|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|H038|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|H039|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|H040|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
