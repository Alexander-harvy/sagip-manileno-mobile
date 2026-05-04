package com.sagip.manileno.utils


import android.content.Context

class TokenManager(context: Context) {

    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit().putString("token", token).apply()
    }

    fun getToken(): String? {
        return prefs.getString("token", null)
    }

    fun saveRole(role: String) {
        prefs.edit().putString("role", role).apply()
    }

    fun getRole(): String? {
        return prefs.getString("role", null)
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun saveUser(name: String, contact: String) {
        prefs.edit()
            .putString("name", name)
            .putString("contact", contact)
            .apply()
    }

    fun getName(): String? {
        return prefs.getString("name", null)
    }

    fun getContact(): String? {
        return prefs.getString("contact", null)
    }

}