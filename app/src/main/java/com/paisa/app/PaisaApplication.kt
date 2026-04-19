package com.paisa.app

import android.app.Application
import com.paisa.app.notifications.NotificationScheduler

class PaisaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationScheduler.schedule(this)
    }
}

