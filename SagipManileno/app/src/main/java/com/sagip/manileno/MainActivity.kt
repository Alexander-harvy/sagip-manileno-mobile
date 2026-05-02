package com.sagip.manileno

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.sagip.manileno.network.ApiClient
import com.sagip.manileno.network.CitizenLoginRequest
import com.sagip.manileno.network.LoginResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        testCitizenLogin()
    }

    private fun testCitizenLogin() {
        val request = CitizenLoginRequest(
            contact_no = "09394228611",
            password = "051705"
        )

        ApiClient.apiService.loginCitizen(request)
            .enqueue(object : Callback<LoginResponse> {

                override fun onResponse(
                    call: Call<LoginResponse>,
                    response: Response<LoginResponse>
                ) {
                    if (response.isSuccessful) {
                        Log.d("LOGIN_TEST", "Citizen login success: ${response.body()}")
                    } else {
                        Log.e(
                            "LOGIN_TEST",
                            "Citizen login failed: ${response.code()} ${response.errorBody()?.string()}"
                        )
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Log.e("LOGIN_TEST", "Citizen login error: ${t.message}")
                }
            })
    }
}