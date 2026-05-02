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
    val success: Boolean? = null,
    val message: String? = null,
    val token: String? = null,
    val role: String? = null
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