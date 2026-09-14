package com.autotrack.app

import android.app.Application

class AutoTrackApp : Application() {

    companion object {
        lateinit var instance: AutoTrackApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        DataSyncManager.init(this)
    }
}
