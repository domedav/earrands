package com.domedav.ballanceometer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AppConfig::class, TaskGroup::class, Subtask::class, Completion::class, Spending::class],
    version = 3,
    exportSchema = false
)
abstract class BallanceDatabase : RoomDatabase() {
    abstract fun appConfigDao(): AppConfigDao
    abstract fun taskGroupDao(): TaskGroupDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun completionDao(): CompletionDao
    abstract fun spendingDao(): SpendingDao

    companion object {
        @Volatile
        private var INSTANCE: BallanceDatabase? = null

        fun getInstance(context: Context): BallanceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BallanceDatabase::class.java,
                    "ballance_database"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
