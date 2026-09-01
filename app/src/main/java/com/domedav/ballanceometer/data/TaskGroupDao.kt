package com.domedav.ballanceometer.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskGroupDao {
    @Query("SELECT * FROM task_groups")
    fun getAll(): Flow<List<TaskGroup>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: TaskGroup)

    @Update
    suspend fun update(group: TaskGroup)

    @Delete
    suspend fun delete(group: TaskGroup)
}
