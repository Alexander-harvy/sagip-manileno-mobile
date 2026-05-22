package com.sagip.manileno.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Path

// LOGIN
data class CitizenLoginRequest(
    val contact_no: String,
    val password: String
)

data class ResponderLoginRequest(
    val employee_no: String,
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val data: LoginData?
)

data class LoginData(
    val token: String?,
    val user: UserData?,
    val responder: ResponderData?
)

data class UserData(
    val user_id: Int,
    val first_name: String,
    val last_name: String,
    val contact_no: String
)

data class ResponderData(
    val responder_id: Int,
    val dept_id: Int,
    val substation_id: Int,
    val employee_no: String,
    val username: String,
    val first_name: String,
    val last_name: String,
    val contact_no: String
)

// INCIDENTS
data class CreateIncidentRequest(
    val incident_type: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val source: String? = null
)

data class GenericResponse(
    val success: Boolean,
    val message: String
)

data class MyIncidentsResponse(
    val success: Boolean,
    val message: String? = null,
    val data: List<Incident>
)

data class Incident(
    val incident_id: Int,
    val incident_type: String,
    val description: String?,
    val latitude: Double?,
    val longitude: Double?,
    val status: String?,
    val latest_status: String?,
    val created_at: String?
)

data class StatusHistoryResponse(
    val success: Boolean,
    val data: List<StatusItem>
)

data class StatusItem(
    val status: String,
    val responder_name: String?,
    val created_at: String
)

//RESPONDER

data class ResponderIncidentsResponse(
    val success: Boolean,
    val message: String? = null,
    val data: List<ResponderIncident>
)

data class ResponderIncident(
    val incident_id: Int,
    val incident_type: String,
    val description: String?,
    val latitude: Double?,
    val longitude: Double?,
    val latest_status: String?,
    val status: String?,
    val reported_at: String?
)

data class UpdateStatusRequest(
    val incident_id: Int,
    val status: String
)
interface ApiService {
// LOGIN
    @POST("users/login")
    fun loginCitizen(
        @Body request: CitizenLoginRequest
    ): Call<LoginResponse>

    @POST("responders/login")
    fun loginResponder(
        @Body request: ResponderLoginRequest
    ): Call<LoginResponse>

//INCIDENTS
    @POST("incidents")
    fun createIncident(
        @Body request: CreateIncidentRequest
    ): Call<GenericResponse>

    @GET("users/me/incidents")
    fun getMyIncidents(): Call<MyIncidentsResponse>

    @GET("incidents/{id}/status-history")
    fun getStatusHistory(
        @Path("id") id: Int
    ): Call<StatusHistoryResponse>

// RESPONDERS

    @GET("responders/me/incidents")
    fun getResponderIncidents(): Call<ResponderIncidentsResponse>

    @POST("responders/me/status")
    fun updateResponderStatus(
        @Body request: UpdateStatusRequest
    ): Call<GenericResponse>
}