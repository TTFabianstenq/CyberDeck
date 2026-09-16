# CyberDeck

Local Android cybersecurity and diagnostics toolkit. CyberDeck inspects **this device** and networks you already have access to. It does not implement exploitation, jamming, credential theft, spam, unauthorized scanning of third-party systems, or remote attacks.

Package: `com.cyberdeck.android`

## Features

- Live dashboard: CPU (`/proc/stat`), RAM, storage, battery, network type, local IPv4, DNS from `LinkProperties`, model, Android version, uptime
- Interactive terminal with real command implementations
- Network diagnostics: ping, DNS, best-effort traceroute, interface listing
- Wi-Fi analyzer using public `WifiManager` scan results
- BLE explorer (passive scan only)
- DNS lab with system resolver + optional user-initiated DoH compare
- Hash lab (MD5, SHA-1, SHA-256, SHA-512) for text and picked files
- Encoding lab (Base64, URL, hex, binary, UTF-8 bytes)
- App inspector and permission auditor (limited by package visibility)
- Storage dashboard
- System / sensor / battery report
- In-app event log with search, clear, copy

## Screenshots

Add device screenshots here after you install a build.

## Requirements

- Android 8.0 (API 26) or newer
- Android Studio Ladybug+ or JDK 17 and Android SDK 35
- Gradle 8.9 / AGP 8.7.2 / Kotlin 2.0.21

## Build

```bash
git clone https://github.com/TTFabianstenq/CyberDeck.git
cd CyberDeck
chmod +x gradlew
./gradlew :app:assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

## Permissions

See table in repository README body.

## License

MIT
