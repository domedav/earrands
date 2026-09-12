package com.domedav.ballanceometer.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "bank_accounts")
data class BankAccount(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val balance: Double = 0.0,
    val isMain: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
