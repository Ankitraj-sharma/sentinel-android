# Stage 7 synthetic holdout — qr
SHA256: `1328a5cc16d293e9d3ceeb2507e78ce60c93e9caca60db8cc417db55fa4b46b2`
Independent author did not inspect rules/tests. Small synthetic English set, not representative real-world accuracy. No tuning against holdout results. Default rules engine only; bundled historical index not loaded.
Manual/shared text: full fixtures. Notification: same readable text projected through engine, not Android delivery/permission/extraction tests. QR: decoded URL destinations only; no camera/image decoding test.
## Manual/shared
Cases 12; TP 3; FP 0; missed scams including abstentions 3; abstentions 0.
Precision 3/3 (100.0%); recall 3/6 (50.0%); false-positive rate 0/6 (0.0%).
## Notification engine projection
Cases 12; TP 3; FP 0; missed scams including abstentions 3; abstentions 0.
Precision 3/3 (100.0%); recall 3/6 (50.0%); false-positive rate 0/6 (0.0%).
## Decoded QR URL only
Cases 12; TP 3; FP 0; missed scams including abstentions 3; abstentions 0.
Precision 3/3 (100.0%); recall 3/6 (50.0%); false-positive rate 0/6 (0.0%).
## All observations
|ID|Label|Risk|Status|Rules|
|---|---|---|---|---|
|Q001|SCAM|REVIEW|COMPLETE|url-userinfo|
|Q002|SCAM|REVIEW|COMPLETE|url-userinfo|
|Q003|SCAM|REVIEW|COMPLETE|url-userinfo|
|Q004|SCAM|NO_OBVIOUS_THREAT|COMPLETE||
|Q005|SCAM|NO_OBVIOUS_THREAT|COMPLETE||
|Q006|SCAM|NO_OBVIOUS_THREAT|COMPLETE||
|Q007|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|Q008|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|Q009|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|Q010|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|Q011|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|Q012|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
