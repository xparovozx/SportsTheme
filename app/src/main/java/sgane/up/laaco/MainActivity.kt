package sgane.up.laaco

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.BuildConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var remoteConfig: FirebaseRemoteConfig
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseApp.initializeApp(this)

        webView = findViewById(R.id.webView)
        webView.webViewClient = WebViewClient()

        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        val savedUrl = sharedPreferences.getString("webViewUrl", "")
        if (savedUrl!!.isNotEmpty()) {
            webView.loadUrl(savedUrl)
        } else {
            remoteConfig = FirebaseRemoteConfig.getInstance()
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build()
            remoteConfig.setConfigSettingsAsync(configSettings)
            remoteConfig.fetchAndActivate()
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val webViewUrl = remoteConfig.getString("webview_url")
                        if (webViewUrl.isNotEmpty()) {
                            if (isNetworkAvailable()) {
                                webView.loadUrl(webViewUrl)
                                // Сохраняем URL в SharedPreferences
                                sharedPreferences.edit().putString("webViewUrl", webViewUrl).apply()
                            } else {
                                val localNoConnectionUrl = "file:///android_asset/no_connection.html"
                                val localNoConnectionExists = try {
                                    assets.open("no_connection.html")
                                    true
                                } catch (e: IOException) {
                                    false
                                }
                                if (localNoConnectionExists) {
                                    webView.loadUrl(localNoConnectionUrl)
                                } else {
                                    if (shouldShowTicTacToe()) {
                                        startActivity(TicTacToeActivity.newIntent(this))
                                        finish()
                                    } else {
                                        webView.loadUrl("file:///android_asset/sport_placeholder.html")
                                    }
                                }
                            }
                        } else {
                            //webView.loadUrl("file:///android_asset/sport_placeholder.html")
                            startActivity(TicTacToeActivity.newIntent(this))
                            finish()
                        }
                    } else {
                        webView.loadUrl("file:///android_asset/error.html")
                    }
                }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
            }
        }
    }

    private fun shouldShowTicTacToe(): Boolean {
        val showTicTacToeFlag = remoteConfig.getBoolean("show_tic_tac_toe")
        return showTicTacToeFlag
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo?.isConnected == true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webView.restoreState(savedInstanceState)
    }

    private fun checkIsEmu(): Boolean {
        if (BuildConfig.DEBUG) return false
        val phoneModel = Build.MODEL
        val buildProduct = Build.PRODUCT
        val buildHardware = Build.HARDWARE
        var result = (Build.FINGERPRINT.startsWith("generic")
                || phoneModel.contains("google_sdk")
                || phoneModel.lowercase(Locale.getDefault()).contains("droid4x")
                || phoneModel.contains("Emulator")
                || phoneModel.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || buildHardware == "goldfish"
                || Build.BRAND.contains("google")
                || buildHardware == "vbox86"
                || buildProduct == "sdk"
                || buildProduct == "google_sdk"
                || buildProduct == "sdk_x86"
                || buildProduct == "vbox86p"
                || Build.BOARD.lowercase(Locale.getDefault()).contains("nox")
                || Build.BOOTLOADER.lowercase(Locale.getDefault()).contains("nox")
                || buildHardware.lowercase(Locale.getDefault()).contains("nox")
                || buildProduct.lowercase(Locale.getDefault()).contains("nox"))
        if (result) return true
        result = result or (Build.BRAND.startsWith("generic") &&
                Build.DEVICE.startsWith("generic"))
        if (result) return true
        result = result or ("google_sdk" == buildProduct)
        return result
    }
}
