package com.coinspace.plugin.accessibilitydatasensitive;

import android.os.Build;
import android.util.Log;
import android.view.View;

import org.apache.cordova.CordovaPlugin;

/**
 * Marks the native Cordova WebView as accessibility data sensitive.
 *
 * <p>The plugin is declared with {@code onload="true"} in plugin.xml, so Cordova instantiates it
 * while the CordovaWebView is being initialized, before the start page is loaded. Every new
 * CordovaWebView (e.g. after the activity is recreated) gets a new plugin instance, so the flag
 * is applied to each WebView exactly once.</p>
 */
public class AccessibilityDataSensitive extends CordovaPlugin {

    private static final String TAG = "A11yDataSensitive";

    @Override
    protected void pluginInitialize() {
        // View methods must be called on the UI thread. Plugin initialization already runs
        // there, in which case runOnUiThread() executes the action immediately.
        cordova.getActivity().runOnUiThread(this::markWebViewSensitive);
    }

    private void markWebViewSensitive() {
        // View.setAccessibilityDataSensitive() and View.ACCESSIBILITY_DATA_SENSITIVE_YES were
        // added in API 34 (Android 14, UPSIDE_DOWN_CAKE). The plugin compiles against SDK 34+
        // (cordova-android >= 13), but must not call the API on older devices, where it does
        // not exist. Below API 34 there is no equivalent, so the plugin does nothing.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Log.d(TAG, "Requires API 34+, running on API " + Build.VERSION.SDK_INT + ", skipped");
            return;
        }

        View view = webView.getView();
        // Setting the same value again is a no-op, so repeated initialization is harmless.
        view.setAccessibilityDataSensitive(View.ACCESSIBILITY_DATA_SENSITIVE_YES);
        Log.d(TAG, "Marked " + view.getClass().getName() + " as accessibility data sensitive");
    }
}
