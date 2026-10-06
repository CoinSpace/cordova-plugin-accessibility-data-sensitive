# cordova-plugin-accessibility-data-sensitive

A Cordova Android plugin that marks the native Cordova WebView as **accessibility data sensitive**, using
[`View.setAccessibilityDataSensitive()`](https://developer.android.com/reference/android/view/View#setAccessibilityDataSensitive(int))
(Android 14 / API 34+).

When the plugin starts, it effectively runs:

```java
webView.getView().setAccessibilityDataSensitive(View.ACCESSIBILITY_DATA_SENSITIVE_YES);
```

After that, Android only lets accessibility services that declare
[`isAccessibilityTool="true"`](https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo#isAccessibilityTool())
read the WebView's content, receive its accessibility events, or act on it. Other accessibility services can't see it.
This is aimed at malware that abuses the accessibility permission to scrape screens, for example to steal recovery
phrases, passwords or OTPs.

No JavaScript is needed. The plugin applies the flag automatically while the WebView is being initialized.

> [!WARNING]
> **This plugin marks the native Cordova WebView as accessibility-sensitive. It does not selectively mark individual
> HTML inputs. If only specific HTML fields need protection, a different/native WebView accessibility solution is
> required.**

## Requirements

| | |
|---|---|
| Platform | Android only |
| cordova-android | **13.0.0 or newer** (the first release that compiles against SDK 34) |
| Compile SDK | 34 or newer. Keep the cordova-android default, or keep `android-compileSdkVersion` at 34+ if you set it |
| Minimum device API | Whatever your app supports. On devices below API 34 the plugin does nothing (see below) |
| Protection active on | Android 14 (API 34) and newer |

## Installation

From npm:

```bash
cordova plugin add cordova-plugin-accessibility-data-sensitive
```

From a local checkout or from git:

```bash
cordova plugin add /path/to/cordova-plugin-accessibility-data-sensitive
cordova plugin add https://github.com/<owner>/cordova-plugin-accessibility-data-sensitive.git
```

Full example with a new app:

```bash
cordova create myapp com.example.myapp MyApp
cd myapp
cordova platform add android@latest
cordova plugin add cordova-plugin-accessibility-data-sensitive
cordova build android
cordova run android
```

The plugin never edits `platforms/android` by hand. Cordova copies the Java source and registers the plugin in
`res/xml/config.xml` through `plugin.xml`, so `cordova platform rm android && cordova platform add android` gives
you the same result.

## Configuration

There is nothing to configure. Once installed, the plugin always marks the WebView on Android 14+.

### JavaScript API

There isn't one, on purpose. A runtime `disable()` call would let any script running in the WebView (including
injected or compromised third-party code) turn the protection off. If you need it off, remove the plugin.

## How it works

```text
Cordova HTML / JS
    ↓
Chromium (renders the page, builds a virtual accessibility tree)
    ↓
android.webkit.WebView  ← CordovaWebView.getView()
    ↓
android.view.View
    ↓
setAccessibilityDataSensitive(ACCESSIBILITY_DATA_SENSITIVE_YES)
```

* `plugin.xml` registers `AccessibilityDataSensitive` as a Cordova feature with `<param name="onload" value="true" />`.
  Cordova's `PluginManager` then creates the plugin while the `CordovaWebView` is initialized, before the start page
  loads, and calls `CordovaPlugin.initialize()` → `pluginInitialize()`.
* On API 34+, `pluginInitialize()` calls
  `setAccessibilityDataSensitive(View.ACCESSIBILITY_DATA_SENSITIVE_YES)` on `CordovaWebView.getView()`, on the UI
  thread.
* There is no reflection and there are no extra dependencies. The API is called directly behind a
  `Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE` check.
* **Idempotent:** setting the same flag again does nothing. Page navigation and reloads keep the same WebView, so the
  flag stays in place. If the activity is recreated (process death, or a configuration change your app doesn't
  handle), Cordova builds a new WebView and a new plugin instance, and the flag is applied to that new WebView.

### It covers the whole WebView, not individual HTML elements

* The flag is set on **one native Android `View`: the Cordova WebView**. Android treats that view and everything
  under it as sensitive. That includes every node in Chromium's virtual accessibility tree, so every HTML element on
  every page shown in that WebView is covered.
* HTML elements are **not** native Android views. `<input>`, `<input type="password">`, `<textarea>` and so on do not
  become Android `EditText` controls, and they don't get their own sensitivity flag. You can't mark only the
  recovery-phrase field as sensitive with this plugin.
* For every accessibility service that is not an accessibility tool, the **whole app UI rendered in the WebView**
  disappears, including buttons, labels and non-sensitive text. That's intended, but think about the impact before
  you ship (see the limitations below).
* Native views outside the WebView are not affected: native dialogs, other plugins' native overlays, the system UI.

## Behavior on Android versions below 14

`setAccessibilityDataSensitive()` doesn't exist below API 34. On those devices the plugin skips the call and logs a
debug message (`A11yDataSensitive: Requires API 34+ ... skipped`). It doesn't crash, it doesn't use reflection, and
the WebView behaves exactly as it would without the plugin. **Android 13 and older get no protection from this
plugin.**

## Security and privacy limitations

* **It's not a screen-reader blocker.** TalkBack, Switch Access, Voice Access and other services that declare
  `isAccessibilityTool="true"` still read and control the WebView normally. That's by design, so users with
  disabilities aren't locked out. Google Play policy only lets apps that are actually built for people with
  disabilities declare that flag, but the flag itself is self-declared. A sideloaded malicious app can still claim it.
* **Android 14+ only.** Older devices get no protection.
* **Accessibility only.** It doesn't stop screenshots, screen recording, casting or the recents thumbnail. For that,
  use `FLAG_SECURE` (for example with a privacy-screen plugin). It also doesn't affect the clipboard, keyboards/IMEs,
  autofill services, WebView remote debugging, or JavaScript running inside your page.
* **WebView only.** Native UI outside the WebView isn't covered.
* **All-or-nothing for the WebView.** Non-tool accessibility services such as password managers, automation apps
  and some enterprise tools can't see any of your WebView UI.
* It reduces risk. It isn't a guarantee. Keep secrets out of the UI where you can, and use defense in depth.

## Verifying the behavior

Plugin log, visible on any API level:

```bash
adb logcat -s A11yDataSensitive
# API 34+:   Marked android.webkit.WebView as accessibility data sensitive
# API < 34:  Requires API 34+, running on API 33, skipped
```

## Project layout

```text
├── plugin.xml                                  Cordova plugin manifest
├── package.json
├── src/android/AccessibilityDataSensitive.java The plugin
└── example/                                    Sample config.xml and www/index.html with test fields
```

## License

MIT
