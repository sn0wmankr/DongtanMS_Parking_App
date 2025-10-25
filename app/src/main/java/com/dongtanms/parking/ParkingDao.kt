package com.dongtanms.parking

import androidx.room.*

@Dao
interface ParkingDao {

    @Query("SELECT * FROM parking_table ORDER BY createdAt DESC")
    suspend fun getAll(): List<ParkingEntry>

    @Insert
    suspend fun insert(entry: ParkingEntry)

    @Update
    suspend fun update(entry: ParkingEntry)

    @Delete
    suspend fun delete(entry: ParkingEntry)

    @Query("DELETE FROM parking_table")
    suspend fun deleteAll()
}
