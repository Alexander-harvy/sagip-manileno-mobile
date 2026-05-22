package com.sagip.manileno.offline

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [OfflineIncident::class],
    version = 1,
    exportSchema = false
)
abstract class OfflineIncidentDatabase : RoomDatabase() {

    abstract fun offlineIncidentDao(): OfflineIncidentDao

    companion object {
        @Volatile
        private var INSTANCE: OfflineIncidentDatabase? = null

        fun getDatabase(context: Context): OfflineIncidentDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OfflineIncidentDatabase::class.java,
                    "offline_incident_database"
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}