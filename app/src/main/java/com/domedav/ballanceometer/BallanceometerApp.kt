package com.domedav.ballanceometer

import android.app.Application
import com.domedav.ballanceometer.data.BallanceDatabase
import com.domedav.ballanceometer.data.BallanceRepository
import com.domedav.ballanceometer.widget.BallanceWidgetUpdater

class BallanceometerApp : Application() {
    val database by lazy { BallanceDatabase.getInstance(this) }
    val repository by lazy { BallanceRepository(database) }

    override fun onCreate() {
        super.onCreate()
        BallanceWidgetUpdater.enqueuePeriodic(this)
    }
}
