package com.domedav.ballanceometer.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BankAccountDao {
    @Query("SELECT * FROM bank_accounts ORDER BY createdAt ASC")
    fun getAll(): Flow<List<BankAccount>>

    @Query("SELECT * FROM bank_accounts WHERE id = :id")
    suspend fun getById(id: String): BankAccount?

    @Query("SELECT * FROM bank_accounts WHERE isMain = 1 LIMIT 1")
    suspend fun getMain(): BankAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: BankAccount)

    @Update
    suspend fun update(account: BankAccount)

    @Delete
    suspend fun delete(account: BankAccount)

    @Query("UPDATE bank_accounts SET isMain = 0")
    suspend fun clearMain()

    @Query("UPDATE bank_accounts SET balance = balance + :delta WHERE id = :id")
    suspend fun adjustBalance(id: String, delta: Double)
}
