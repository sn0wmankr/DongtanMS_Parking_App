package com.dongtanms.parking

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parking_table")
data class ParkingEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val plateNumber: String,
    val status: String = "pending",
    val createdAt: Long = System.currentTimeMillis()
)
