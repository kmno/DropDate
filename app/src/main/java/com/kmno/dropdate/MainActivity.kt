package com.kmno.dropdate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kmno.dropdate.core.analytics.AnalyticsHelper
import com.kmno.dropdate.core.analytics.LocalAnalyticsHelper
import com.kmno.dropdate.presentation.navigation.DropDateNavGraph
import com.kmno.dropdate.ui.theme.DropDateTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle =
                SystemBarStyle.dark(
                    android.graphics.Color.TRANSPARENT,
                ),
            navigationBarStyle =
                SystemBarStyle.dark(
                    android.graphics.Color.TRANSPARENT,
                ),
        )
        setContent {
            DropDateTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CompositionLocalProvider(LocalAnalyticsHelper provides analyticsHelper) {
                        DropDateNavGraph()
                    }
                }
            }
        }
    }
}
