package com.domedav.ballanceometer.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SpendingDao {
    @Query("SELECT * FROM spendings")
    fun getAll(): Flow<List<Spending>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(spending: Spending)

    @Delete
    suspend fun delete(spending: Spending)

    @Query("SELECT * FROM spendings WHERE accountId = :accountId")
    suspend fun getByAccount(accountId: String): List<Spending>

    @Query("UPDATE spendings SET accountId = :newAccountId WHERE accountId = :oldAccountId")
    suspend fun reassignAccount(oldAccountId: String, newAccountId: String?)
}
