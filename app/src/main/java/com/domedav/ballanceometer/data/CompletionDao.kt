package com.domedav.ballanceometer.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletionDao {
    @Query("SELECT * FROM completions")
    fun getAll(): Flow<List<Completion>>

    @Query("SELECT * FROM completions WHERE date = :date")
    fun getByDate(date: String): Flow<List<Completion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(completion: Completion)

    @Delete
    suspend fun delete(completion: Completion)
}
