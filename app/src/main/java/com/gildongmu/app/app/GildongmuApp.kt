package com.gildongmu.app.app

import android.app.Application

class GildongmuApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
