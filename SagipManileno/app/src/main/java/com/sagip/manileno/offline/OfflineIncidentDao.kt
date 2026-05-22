package com.sagip.manileno.offline

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface OfflineIncidentDao {

    @Insert
    suspend fun insertIncident(incident: OfflineIncident)

    @Query("SELECT * FROM offline_incidents ORDER BY created_at ASC")
    suspend fun getAllIncidents(): List<OfflineIncident>

    @Query("DELETE FROM offline_incidents WHERE id = :id")
    suspend fun deleteIncident(id: Int)
}