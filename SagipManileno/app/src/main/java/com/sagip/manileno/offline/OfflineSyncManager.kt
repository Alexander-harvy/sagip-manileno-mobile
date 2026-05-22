package com.sagip.manileno.offline

import android.content.Context
import android.widget.Toast
import com.sagip.manileno.network.ApiClient
import com.sagip.manileno.network.CreateIncidentRequest
import com.sagip.manileno.network.GenericResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object OfflineSyncManager {

    fun syncOfflineIncidents(context: Context) {

        val db = OfflineIncidentDatabase.getDatabase(context)

        CoroutineScope(Dispatchers.IO).launch {

            val incidents = db.offlineIncidentDao().getAllIncidents()

            incidents.forEach { incident ->

                val request = CreateIncidentRequest(
                    incident_type = normalizeIncidentType(incident.incident_type),
                    description = incident.description,
                    latitude = incident.latitude,
                    longitude = incident.longitude,
                    source = "offline_sync"
                )

                ApiClient.getClient(context)
                    .createIncident(request)
                    .enqueue(object : Callback<GenericResponse> {

                        override fun onResponse(
                            call: Call<GenericResponse>,
                            response: Response<GenericResponse>
                        ) {

                            if (
                                response.isSuccessful &&
                                response.body()?.success == true
                            ) {

                                CoroutineScope(Dispatchers.IO).launch {
                                    db.offlineIncidentDao()
                                        .deleteIncident(incident.id)
                                }

                                Toast.makeText(
                                    context,
                                    "Offline incident synced",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        override fun onFailure(
                            call: Call<GenericResponse>,
                            t: Throwable
                        ) {
                            // keep incident in queue
                        }
                    })
            }
        }
    }

    private fun normalizeIncidentType(type: String): String {
        return when (type.trim().lowercase()) {
            "medical" -> "medic"
            "medic" -> "medic"
            "police" -> "police"
            "fire" -> "fire"
            else -> type.trim().lowercase()
        }
    }
}