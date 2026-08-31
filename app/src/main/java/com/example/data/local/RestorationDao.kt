package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.RestorationRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface RestorationDao {
    @Query("SELECT * FROM restorations ORDER BY timestamp DESC")
    fun getAllRestorations(): Flow<List<RestorationRecord>>

    @Query("SELECT * FROM restorations WHERE id = :id")
    suspend fun getRestorationById(id: Long): RestorationRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRestoration(record: RestorationRecord): Long

    @Update
    suspend fun updateRestoration(record: RestorationRecord)

    @Delete
    suspend fun deleteRestoration(record: RestorationRecord)

    @Query("DELETE FROM restorations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM restorations")
    suspend fun clearAll()
}
