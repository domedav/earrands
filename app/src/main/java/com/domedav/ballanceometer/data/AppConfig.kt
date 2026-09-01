package com.domedav.ballanceometer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_config")
data class AppConfig(
    @PrimaryKey val id: Int = 1,
    val totalBalance: Double = 0.0,
    val minimalSpend: Double = 0.0,
    val currency: String = "HUF"
)
