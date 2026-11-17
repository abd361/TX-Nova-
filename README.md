# TX Nova — Full Android Browser Source

This project contains:
- A Vite + Capacitor home screen
- A native Android WebView browser activity
- Address/search navigation (DuckDuckGo)
- Back, forward, home, reload via page navigation
- Multiple tabs and tab switching/closing
- Bookmarks and recent history
- Downloads to Android's Downloads folder
- Desktop-site mode
- Private browsing mode
- Clear browsing data
- Share page
- Dark/light home screen

## Requirements
- Windows 10/11
- Node.js LTS
- VS Code
- Android Studio with Android SDK installed
- A real Android phone or emulator for testing

## 1. Open and install
Extract this ZIP. Open the `TX-Nova-Full` folder in VS Code.
Open the VS Code terminal in the project folder and run:

```powershell
npm install
npm run build
npx cap add android
```

If `npx cap add android` says the Android platform already exists, do not run it again.

## 2. Add the native browser files
After `npx cap add android`, copy these two Java files from:
`android-native/app/src/main/java/com/metrixstudio/txnova/`

- `NovaBrowserPlugin.java`
- `BrowserActivity.java`

Into the generated Android folder:
`android/app/src/main/java/com/metrixstudio/txnova/`

The generated MainActivity package path may differ if you changed the App ID. If so, use the matching package folder and update the first `package` line in both Java files.

## 3. Register the plugin
Open the generated `MainActivity.java`. Add the import:

```java
import com.metrixstudio.txnova.NovaBrowserPlugin;
```

Inside its `onCreate`, before `super.onCreate(savedInstanceState);`, add:

```java
registerPlugin(NovaBrowserPlugin.class);
```

If you prefer, compare with `MainActivity.java.example` in the `android-native` folder. Keep the generated MainActivity class and package name appropriate for your project.

## 4. Add Android manifest entries
Open:
`android/app/src/main/AndroidManifest.xml`

Add these permissions directly inside `<manifest>` and outside `<application>`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

Inside `<application>`, add:

```xml
<activity
    android:name=".BrowserActivity"
    android:exported="false"
    android:configChanges="keyboard|keyboardHidden|orientation|screenSize" />
```

A complete example is in `android-native/app/src/main/AndroidManifest-additions.xml`.

## 5. Build and open Android Studio

From the project root:

```powershell
npm run build
npx cap sync android
npx cap open android
```

In Android Studio, wait for Gradle sync, connect your phone with USB debugging enabled, then press Run.

To create an APK: Android Studio → Build → Build Bundle(s) / APK(s) → Build APK(s).

## 6. After changing web files
Run:

```powershell
npm run build
npx cap sync android
```

Then run the app again from Android Studio.

## Important notes and limitations
- This is a source project, not a prebuilt APK.
- The native browser uses Android System WebView. It depends on Android's installed WebView/Chrome component.
- Private mode disables TX Nova's history saving and website storage/cache for tabs created/used in that mode, and clears cookies when enabled. Android/WebView and websites may still have their own network-side records; do not treat it as anonymity.
- Download support depends on the website and Android Download Manager.
- This is a substantial first implementation, not a security-audited browser. Do not enter sensitive information until thoroughly tested.
- File upload pickers, microphone/camera permissions, password syncing, extensions, ad blocking, sync across devices, and advanced Safe Browsing integration are not implemented yet.
