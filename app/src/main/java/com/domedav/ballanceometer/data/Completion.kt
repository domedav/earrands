package com.domedav.ballanceometer.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "completions",
    foreignKeys = [
        ForeignKey(
            entity = Subtask::class,
            parentColumns = ["id"],
            childColumns = ["subtaskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subtaskId"]), Index(value = ["date"])]
)
data class Completion(
    @PrimaryKey val id: String,
    val subtaskId: String,
    val date: String,
    val completedAt: Long,
    val valueUnlocked: Double
)
