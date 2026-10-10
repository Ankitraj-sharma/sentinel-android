# Sentinel — project overview

Sentinel ek Android app hai jo user ko suspicious messages, phishing links aur web QR
destinations samajhne mein help karti hai. Hackathon mein hamara main demonstration hai:
incoming readable notification ko locally analyse karna, risk ke reasons dikhana aur
suspicious source notification ko Sentinel warning se replace karna.

## User ke liye kya bana hai

1. **Protection:** user notification access deta hai, monitoring ON karta hai aur WhatsApp,
   Gmail ya default SMS sources select karta hai. App newly delivered readable notification
   previews analyse karti hai. Background protection ka ongoing notification status dikhata hai.
2. **Warnings:** suspicious patterns ya bundled historical malicious-URL evidence par risk
   assessment milta hai. Reasons aur matched evidence visible hain. Warning successfully
   post hone ke baad applicable source notification remove hota hai; original message
   WhatsApp/SMS/Gmail se delete nahi hota.
3. **Scan:** text/link paste karo ya kisi app se text Share to Sentinel karo.
4. **QR:** camera ya selected image se QR decode karke HTTP/HTTPS destination analyse karo.
   App destination automatically open nahi karti.
5. **History:** optional encrypted assessment metadata history, delete controls aur local
   retention limits. Full messages, OTPs, URLs aur images history mein save nahi hote.
6. **Settings and privacy:** sources choose karo, monitoring pause karo, permissions/status
   check karo aur history manage karo. App interface English mein hai.

## Detection kaise hoti hai

Readable input → text/URL extraction → local scam rules and historical exact-URL index →
risk assessment and reasons → UI/warning → optional encrypted metadata history.

Examples of rule signals include prize/payment urgency, credential requests, advance job
fees, remote access, gift cards and suspicious combinations. Bundled historical URL data
additional evidence deti hai. Ye trained AI model ya live website reputation service nahi hai.
Missing match ka matlab safe hona nahi hai; false positives aur missed scams possible hain.

## Privacy ko sahi tarah explain karo

"Notification access dene par Sentinel readable notification previews access aur analyse
karti hai. Analysis device par hoti hai; current APK Internet permission request nahi karti."

"Hum koi data access nahi karte" claim galat hoga: analysis ke liye app selected previews
read karti hai. Complete inbox, private chat history aur hidden previews available nahi hain.
Android naye versions mein OTP content redact kar sakta hai.

## Current readiness and testing

Stages 1–6 completed hain: team's estimated 75% pre-hackathon milestone. Iske baad SMS alert
recovery, background protection, notification replacement, branding aur release signing
improvements hue. Remaining stages ko hackathon objectives aur demo needs ke hisaab se
continue karna hai; prize guarantee nahi hai.

Version 0.7.3: Android 8+, universal 32/64-bit signed release. Engine ke 42 tests passed;
release lint mein 0 errors, 5 warnings. iQOO Android 16 par ADB install, launch aur same-version
reinstall verified. 0.7.2 par user ne Files installation confirm ki thi. Real SMS warning and
Recents swipe ke baad background replacement user ne earlier build par confirm kiya tha;
ye separate checks hain, har phone/version par full behavior verified nahi hai.

## Hackathon demo

1. Explain consent and enable protection on a connected, configured test phone.
2. Send a controlled suspicious SMS from another phone while Sentinel UI is closed.
3. Show the Sentinel warning and source-notification replacement.
4. Open assessment to explain the reasons and uncertainty.
5. Show an ordinary message staying untouched, then manual/QR scan and history deletion.

Use synthetic messages and test links. Notification preview must be delivered; an open chat
may suppress it. Android Force stop, reboot and vendor background restrictions can interrupt
protection; inspect status and reopen as needed.

## Distribution for this phase

Current focus hackathon and USB-connected test phones hai; Google appeal/public launch is
paused by the owner. Play Protect can block browser/WhatsApp/Files installation because the
app has notification-listener access. ADB testing is not proof that public sideloading works.
Connect each new phone, identify it and install the appropriate build without automatically
deleting an existing installation or its data. Standard release and older debug builds may
have different signing keys; `.validation` test builds have a separate package and data.
