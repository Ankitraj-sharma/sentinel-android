# Stage 7 synthetic holdout — unseen
SHA256: `51ad7f35b61fbad3c033964f5b50f54d0ea7e9423647d3f8b2d94a9723a4e20c`
Independent author did not inspect rules/tests. Small synthetic English set, not representative real-world accuracy. No tuning against holdout results. Default rules engine only; bundled historical index not loaded.
Manual/shared text: full fixtures. Notification: same readable text projected through engine, not Android delivery/permission/extraction tests. QR: decoded URL destinations only; no camera/image decoding test.
## Manual/shared
Cases 24; TP 2; FP 0; missed scams including abstentions 10; abstentions 0.
Precision 2/2 (100.0%); recall 2/12 (16.7%); false-positive rate 0/12 (0.0%).
## Notification engine projection
Cases 24; TP 2; FP 0; missed scams including abstentions 10; abstentions 0.
Precision 2/2 (100.0%); recall 2/12 (16.7%); false-positive rate 0/12 (0.0%).
## Decoded QR URL only
Cases 0; TP 0; FP 0; missed scams including abstentions 0; abstentions 0.
Precision N/A; recall N/A; false-positive rate N/A.
## All observations
|ID|Label|Risk|Status|Rules|
|---|---|---|---|---|
|U001|SCAM|NO_OBVIOUS_THREAT|COMPLETE||
|U002|SCAM|NO_OBVIOUS_THREAT|COMPLETE|urgency|
|U003|SCAM|NO_OBVIOUS_THREAT|COMPLETE||
|U004|SCAM|NO_OBVIOUS_THREAT|COMPLETE||
|U005|SCAM|REVIEW|COMPLETE|credential-request|
|U006|SCAM|NO_OBVIOUS_THREAT|COMPLETE||
|U007|SCAM|NO_OBVIOUS_THREAT|COMPLETE||
|U008|SCAM|NO_OBVIOUS_THREAT|COMPLETE||
|U009|SCAM|NO_OBVIOUS_THREAT|COMPLETE||
|U010|SCAM|REVIEW|COMPLETE|credential-request|
|U011|SCAM|NO_OBVIOUS_THREAT|COMPLETE||
|U012|SCAM|NO_OBVIOUS_THREAT|COMPLETE||
|U013|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|U014|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|U015|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|U016|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|U017|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|U018|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|U019|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|U020|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|U021|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|U022|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|U023|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
|U024|BENIGN|NO_OBVIOUS_THREAT|COMPLETE||
