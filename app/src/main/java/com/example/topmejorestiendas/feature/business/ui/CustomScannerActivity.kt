package com.example.topmejorestiendas.feature.business.ui

import android.content.res.Configuration
import android.os.Bundle
import com.journeyapps.barcodescanner.CaptureActivity

class CustomScannerActivity : CaptureActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }
}
