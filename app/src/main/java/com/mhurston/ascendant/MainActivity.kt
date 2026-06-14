package com.mhurston.ascendant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mhurston.ascendant.ui.AppViewModel
import com.mhurston.ascendant.ui.AscendantApp
import com.mhurston.ascendant.ui.theme.AscendantTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Swap from the native launch-splash theme to the normal app theme once we draw.
        setTheme(R.style.Theme_Ascendant)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AscendantTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AscendantApp(viewModel)
                }
            }
        }
    }
}
