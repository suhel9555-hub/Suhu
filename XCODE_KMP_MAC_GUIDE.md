# Find Correct Premium: Xcode & iOS Integration Guide (Kotlin Multiplatform)

This guide provides step-by-step instructions for exporting this codebase, setting up Kotlin Multiplatform (KMP), and configuring **Xcode on a Mac** to compile and run both the Android and iOS apps natively. It also covers building the **Android App Bundle (`.aab`)** for publication.

---

## 🛠️ Step 1: Exporting Your Project

Before running on your Mac, download the latest version of your code from AI Studio:
1. In the top-right corner of the **Google AI Studio** workspace, click the **Settings / Actions Menu**.
2. Select **Export project as ZIP** (or select **Push to GitHub** to link directly to a private or public repository).
3. Save and extract the ZIP file onto your Mac computer.

---

## 💻 Step 2: Preparing Your Mac Environment

To compile Kotlin Multiplatform (KMP) code to native iOS targets via Xcode, your Mac must have the necessary developer tools installed.

1. **Install Xcode:**
   Download and install Xcode from the Mac App Store. Ensure you open Xcode at least once to agree to the license agreement and install the required command-line tools.
2. **Install Kotlin Multiplatform Mobile (KMM) Plugin:**
   In **Android Studio** on your Mac, go to `Preferences` -> `Plugins` -> `Marketplace` and search for **Kotlin Multiplatform Mobile**. Install and restart.
3. **Verify via KDoctor:**
   Run the environment verifier in your Terminal to ensure all multiplatform prerequisites are met:
   ```bash
   brew install kdoctor
   kdoctor
   ```

---

## 📂 Step 3: Understanding the KMP Project Architecture

In a Kotlin Multiplatform (KMP) structure, code is organized to maximize reuse while keeping access to platform-specific APIs (like Biometrics, GPS, and Camera):

```text
├── shared/                   # Shared Business Logic & Compose Multiplatform UI
│   ├── src/
│   │   ├── commonMain/       # 100% Shared UI (Compose) and State (ViewModel/Room)
│   │   ├── androidMain/      # Android-specific integrations
│   │   └── iosMain/          # iOS-specific integrations (Swift/UIKit Bridge)
├── app/                      # Native Android Client wrapper
└── iosApp/                   # Native Xcode Project / iOS Client wrapper
```

---

## 🔗 Step 4: Bridging Kotlin & Xcode (iOS Setup)

To display your Compose UI inside the iOS Application in Xcode, we use a bridge via `UIViewControllerRepresentable`.

### 1. The Kotlin Entry Point (Shared SDK)
Your shared Kotlin multiplatform codebase defines an iOS view controller in `shared/src/iosMain/kotlin/main.ios.kt`:

```kotlin
package com.example.ui

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    // Renders the main Find Correct Premium Jetpack Compose App UI
    MainAppTheme {
        MainHub()
    }
}
```

### 2. The SwiftUI View Wrapper (Swift side)
In Xcode, create a Swift file named `ComposeView.swift` to convert the Kotlin view controller into a SwiftUI view:

```swift
import SwiftUI
import shared // Compiles from the shared KMP module

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return Main_iosKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
```

### 3. The iOS App Entry Point
Update your primary SwiftUI `App` structure in Xcode to render the Compose view full-screen:

```swift
import SwiftUI

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea(.all) // Fluid Edge-to-Edge display
        }
    }
}
```

---

## ⚙️ Step 5: Xcode Project Configuration

To link the shared Kotlin library with your Xcode build pipeline, configure Xcode to call the Gradle compiler automatically on build:

1. Open Xcode on your Mac and open the `iosApp` workspace folder.
2. Select the **iosApp** project file in the left sidebar navigator.
3. Go to the **Build Phases** tab.
4. Click the **`+` (Add)** icon in the top-left and select **New Run Script Phase**.
5. Move the new script phase so it executes **before** the "Compile Sources" phase.
6. Paste the following shell script into the run script box:
   ```bash
   cd "$SRCROOT/.."
   ./gradlew :shared:embedAndSignAppleFrameworkForXcode
   ```
7. Press `Cmd + B` to build. Xcode will invoke Kotlin's native compiler and bundle the shared library.

---

## 📱 Step 6: Running the iOS App

1. Connect your iPhone via USB or select an **iOS Simulator** from the top device dropdown in Xcode.
2. Click the **Play button (Run)** or press `Cmd + R`.
3. The iOS application will compile, build, and deploy the high-fidelity matchmaking social network interface directly onto your device.

---

## 📦 Step 7: Building the Android App Bundle (`.aab`)

To distribute your Android app on the Google Play Store, build the signed binary archive file format (`.aab`):

### 1. Build via Terminal/Command Line:
From the root of your project, run:
```bash
./gradlew :app:bundleRelease
```

### 2. Locate the Generated `.aab`:
Once the Gradle execution finishes, your production-ready bundle will be located at:
```text
app/build/outputs/bundle/release/app-release.aab
```

### 3. Build via Android Studio UI:
1. Open the project in Android Studio on your Mac.
2. Select **Build** -> **Generate Signed Bundle / APK...** from the top menu bar.
3. Choose **Android App Bundle** and click **Next**.
4. Point to your secure production Keystore, enter your credentials, and build.
