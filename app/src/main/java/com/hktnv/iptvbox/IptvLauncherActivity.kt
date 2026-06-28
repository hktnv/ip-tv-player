package com.hktnv.iptvbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hktnv.iptvbox.core.designsystem.IptvTheme
import com.hktnv.iptvbox.telemetry.AppPerformanceTelemetry
import com.hktnv.iptvbox.ui.host.IptvBoxApp

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
