package com.cyberdeck.android

import android.app.Application
import com.cyberdeck.android.core.EventLog

class CyberDeckApp : Application() {
    override fun onCreate() {
        super.onCreate()
        EventLog.init()
        EventLog.add("CyberDeck started")
    }
}
