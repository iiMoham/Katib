package com.katib.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.LayoutDirection
import com.katib.app.data.WritingMode
import com.katib.app.data.WritingStats
import com.katib.app.net.CorrectResponse
import com.katib.app.net.KatibApiClient
import com.katib.app.ui.DashboardScreen
import com.katib.app.ui.OnboardingScreen
import com.katib.app.ui.PaywallScreen
import com.katib.app.ui.SettingsScreen
import com.katib.app.ui.theme.KatibTeal
import com.katib.app.ui.theme.KatibTheme
import kotlinx.coroutines.launch

private enum class Screen { Dashboard, Settings, Paywall }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as KatibApplication

        setContent {
            KatibTheme {
                // The whole app is RTL-first regardless of device locale.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        KatibRoot(app, activity = this)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KatibRoot(app: KatibApplication, activity: android.app.Activity) {
    val scope = rememberCoroutineScope()
    val prefs = app.prefs

    val onboarded by prefs.onboarded.collectAsState(initial = true)
    val mode by prefs.mode.collectAsState(initial = WritingMode.MSA)
    val isPremium by prefs.isPremium.collectAsState(initial = false)
    val stats by prefs.stats.collectAsState(initial = WritingStats())

    var screen by remember { mutableStateOf(Screen.Dashboard) }

    if (!onboarded) {
        OnboardingScreen(onDone = { scope.launch { prefs.setOnboarded(true) } })
        return
    }

    val runCorrection: suspend (String, String) -> CorrectResponse? = { text, m ->
        when (val r = app.api.correct(text, m, "correct")) {
            is KatibApiClient.Result.Ok -> r.response
            is KatibApiClient.Result.Error -> null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleFor(screen)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KatibTeal,
                    titleContentColor = Color.White,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = screen == Screen.Dashboard,
                    onClick = { screen = Screen.Dashboard },
                    icon = { Icon(Icons.Filled.Home, null) },
                    label = { Text("الرئيسية") },
                )
                NavigationBarItem(
                    selected = screen == Screen.Paywall,
                    onClick = { screen = Screen.Paywall },
                    icon = { Icon(Icons.Filled.Star, null) },
                    label = { Text("بريميوم") },
                )
                NavigationBarItem(
                    selected = screen == Screen.Settings,
                    onClick = { screen = Screen.Settings },
                    icon = { Icon(Icons.Filled.Settings, null) },
                    label = { Text("الإعدادات") },
                )
            }
        },
    ) { padding ->
        val content = Modifier.padding(padding)
        when (screen) {
            Screen.Dashboard -> androidx.compose.foundation.layout.Box(content) {
                DashboardScreen(stats = stats, mode = mode, runCorrection = runCorrection)
            }
            Screen.Settings -> androidx.compose.foundation.layout.Box(content) {
                SettingsScreen(
                    mode = mode,
                    isPremium = isPremium,
                    onModeChange = { scope.launch { prefs.setMode(it) } },
                    onOpenPaywall = { screen = Screen.Paywall },
                )
            }
            Screen.Paywall -> androidx.compose.foundation.layout.Box(content) {
                PaywallScreen(
                    isPremium = isPremium,
                    onSubscribe = { productId ->
                        app.subscriptions.launchPurchase(activity, productId)
                    },
                    onRestore = { app.subscriptions.restore() },
                    onDebugUnlock = { app.subscriptions.debugUnlock(true) },
                )
            }
        }
    }
}

private fun titleFor(screen: Screen): String = when (screen) {
    Screen.Dashboard -> "كاتب"
    Screen.Settings -> "الإعدادات"
    Screen.Paywall -> "بريميوم"
}
