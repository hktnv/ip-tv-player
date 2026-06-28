package com.evomrdm.iptvbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.evomrdm.iptvbox.core.designsystem.IptvTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val telemetry = AppPerformanceTelemetry(this)
        telemetry.mark("cold_start_on_create")
        setContent {
            IptvTheme {
                IptvBoxApp(telemetry)
            }
        }
    }
}
