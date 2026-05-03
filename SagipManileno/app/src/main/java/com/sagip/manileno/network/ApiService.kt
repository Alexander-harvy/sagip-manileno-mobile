package com.sagip.manileno.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

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

interface ApiService {

    @POST("users/login")
    fun loginCitizen(
        @Body request: CitizenLoginRequest
    ): Call<LoginResponse>

    @POST("responders/login")
    fun loginResponder(
        @Body request: ResponderLoginRequest
    ): Call<LoginResponse>
}