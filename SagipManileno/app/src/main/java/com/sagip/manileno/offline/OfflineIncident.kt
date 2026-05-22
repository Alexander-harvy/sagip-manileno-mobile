package com.sagip.manileno.offline

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_incidents")
data class OfflineIncident(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val incident_type: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val source: String = "offline_sync",
    val sms_sent: Boolean = false,
    val created_at: Long = System.currentTimeMillis()
)