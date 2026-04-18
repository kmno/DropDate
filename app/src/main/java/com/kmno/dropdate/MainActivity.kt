package com.kmno.dropdate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kmno.dropdate.ui.theme.DropDateTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DropDateTheme {
                DropDateNavGraph()
            }
        }
    }
}

// Temporary stub — will be replaced in Task 12
@Composable
fun DropDateNavGraph() {
    androidx.compose.material3.Text("DropDate loading...")
}
