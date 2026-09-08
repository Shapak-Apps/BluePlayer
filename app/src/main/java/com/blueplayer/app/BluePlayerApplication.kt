package com.blueplayer.app

import android.app.Application
import com.blueplayer.app.di.AppContainer

class BluePlayerApplication : Application() {

    val container: AppContainer by lazy {
        AppContainer(this)
    }
}