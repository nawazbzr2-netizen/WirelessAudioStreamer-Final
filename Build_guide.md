# Build Guide — Wireless Audio Streamer v2

## APK kaise banaye / download karein

### Tarika 1: GitHub Actions (mobile pe asaan)

1. Ye link open karo:
   https://github.com/nawazbzr2-netizen/WirelessAudioStreamer-Final/actions

2. Latest **Build APK** run dekho.
   - Green ✓ = successful
   - Red ✗ = fail (screenshot bhejo)

3. Green run pe tap karo → neeche **Artifacts** section:
   - `WirelessAudioStreamer-debug`  → testing ke liye
   - `WirelessAudioStreamer-release` → normal use ke liye

4. APK download karo → phone pe install karo  
   (Settings → Allow install from unknown sources)

---

### Tarika 2: Complete source ZIP se (agar Actions fail ho)

1. File download karo: `WirelessAudioStreamer-v2-COMPLETE-SOURCE.zip`
2. Extract karo
3. PC pe Android Studio se open karo → **Build → Build APK(s)**
4. Ya kisi dost / cyber se Android Studio se build karwa lo

---

### Tarika 3: Command line (PC)

```bash
# Project folder mein
./gradlew assembleDebug
./gradlew assembleRelease
```

APK yahan milenge:
- `app/build/outputs/apk/debug/`
- `app/build/outputs/apk/release/`

---

## App use karne ka tarika

1. **Dono phones** pe app install karo
2. Pehli baar open → **Role** choose karo:
   - Ek phone = **Sender** (awaaz bhejega)
   - Doosra = **Receiver** (awaaz sunega)
3. **Same PIN** dono pe rakho (4–8 digits)
4. **Wi-Fi / Hotspot:**
   - Receiver pe "Start listening" dabao
   - Sender pe list se peer select karo + screen-cast Allow
5. **Bluetooth:**
   - Pehle system settings mein pair karo
   - Phir app ke BT tab se connect

---

## Requirements

| Cheez        | Value              |
|-------------|--------------------|
| min Android | 11 (API 30)        |
| max tested  | Android 15 (API 35)|
| JDK         | 17                 |
| Network     | Same Wi-Fi / Hotspot / paired BT |

---

## Important notes

- Release APK debug key se signed hai — seedha install ho jati hai
- Play Store ke liye apna keystore lagana hoga (`app/build.gradle.kts`)
- Internet use nahi hota — sirf LAN / Hotspot / Bluetooth
- Latency setting: Settings → Delay / stability (40 / 80 / 160 ms)

---

## Links

- Repo (Final): https://github.com/nawazbzr2-netizen/WirelessAudioStreamer-Final
- Actions: https://github.com/nawazbzr2-netizen/WirelessAudioStreamer-Final/actions
- Old repo: https://github.com/nawazbzr2-netizen/WirelessAudioStreamer-v2

---

Agar Actions red ho to screenshot bhejo — main fix kar dunga.
