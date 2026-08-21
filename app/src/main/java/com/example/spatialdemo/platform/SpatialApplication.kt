package com.example.spatialdemo.platform

import android.app.Application
import com.pico.spatial.ui.foundation.dsl.launch
import com.example.spatialdemo.mainApp

class SpatialApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        launch(::mainApp)
    }
}
