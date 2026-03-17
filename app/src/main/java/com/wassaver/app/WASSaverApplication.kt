package com.wassaver.app

import android.app.Application

class WASSaverApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: WASSaverApplication
            private set
    }
}
