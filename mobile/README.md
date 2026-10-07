# Mestxa Mobile Client Architecture

This directory houses the mobile client shells (Android & iOS) that bind directly to **`libmestxa`** (our native Rust cryptographic engine).

---

## 1. Architectural Philosophy

1. **Native Performance, Zero Web-Wrapper Bloat**:
   * Android is built with **Kotlin + Jetpack Compose**.
   * iOS is built with **Swift + SwiftUI**.
   * Both platform shells link against the single compiled `libmestxa` binary (`.so` on Android, `.a` on iOS) through **UniFFI** type-safe bindings.
2. **Hardware KeyStore & Secure Enclave Isolation**:
   * The SQLCipher master key is generated inside the hardware security silicon:
     * **Android**: `AndroidKeyStore` with `KeyGenParameterSpec.Builder(..., KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT).setIsStrongBoxBacked(true)`
     * **iOS**: `kSecAttrTokenIDSecureEnclave` via the Apple Keychain Services API.
3. **Strict OLED Dark Mode Design System (from Stitch)**:
   * Canvas: `#000000` (true black).
   * Elevated surfaces: `#111111` / `#1C1C1C`.
   * Structural borders: 1px hairline `#222222`.
   * High contrast white pill buttons (`#FFFFFF`) with black typography.
   * Full-color uncompressed photographic avatars.

---

## 2. Directory Layout

```
mobile/
├── android/                  # Native Android Shell (Kotlin / Compose)
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── jniLibs/      # Compiled libmestxa.so (arm64-v8a, armeabi-v7a, x86_64)
│   │   │   ├── kotlin/com/mestxa/
│   │   │   │   ├── engine/   # Generated UniFFI bindings
│   │   │   │   ├── ui/       # Jetpack Compose screens (Onboarding, Chats, Conversation)
│   │   │   │   └── calls/    # WebRTC 48kHz audio pipeline with SFrame insertable stream
│   │   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── ios/                      # Native iOS Shell (Swift / SwiftUI)
│   ├── Mestxa/
│   │   ├── Engine/           # libmestxa.a + Generated Swift bindings
│   │   ├── Views/            # SwiftUI screens (Onboarding, Chats, Conversation)
│   │   └── Calls/            # CallKit & WebRTC audio pipeline
│   └── Mestxa.xcodeproj
└── shared/                   # Common proto definitions & assets
```

---

## 3. Web & Desktop Preview

An interactive, pixel-perfect preview uniting the Stitch Onboarding flow and the core 4-tab messaging interface is located at [`index.html`](file:///c:/xampp/htdocs/mestxa/index.html). If running XAMPP Apache, you can preview it immediately at `http://localhost/mestxa/`.
