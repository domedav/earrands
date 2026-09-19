package com.domedav.ballanceometer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE subtasks ADD COLUMN weight REAL NOT NULL DEFAULT 50.0")
    }
}

@Database(
    entities = [AppConfig::class, TaskGroup::class, Subtask::class, Completion::class, Spending::class, BankAccount::class],
    version = 5,
    exportSchema = false
)
abstract class BallanceDatabase : RoomDatabase() {
    abstract fun appConfigDao(): AppConfigDao
    abstract fun taskGroupDao(): TaskGroupDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun completionDao(): CompletionDao
    abstract fun spendingDao(): SpendingDao
    abstract fun bankAccountDao(): BankAccountDao

    companion object {
        @Volatile
        private var INSTANCE: BallanceDatabase? = null

        fun getInstance(context: Context): BallanceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BallanceDatabase::class.java,
                    "ballance_database"
                ).fallbackToDestructiveMigration(true).addMigrations(MIGRATION_4_5).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
