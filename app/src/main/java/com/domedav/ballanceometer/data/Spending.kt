package com.domedav.ballanceometer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spendings")
data class Spending(
    @PrimaryKey val id: String,
    val amount: Double,
    val note: String = "",
    val timestamp: Long,
    /** Bank account this spending was paid from. Null = legacy spending, falls back to main account. */
    val accountId: String? = null
)
