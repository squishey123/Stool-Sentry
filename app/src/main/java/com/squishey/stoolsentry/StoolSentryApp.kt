package com.squishey.stoolsentry

import android.app.Application

class StoolSentryApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
