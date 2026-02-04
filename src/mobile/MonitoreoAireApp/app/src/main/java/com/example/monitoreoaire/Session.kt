package com.example.monitoreoaire

import android.content.Context

object Session {

    private const val PREF = "sesion"

    fun isLoggedIn(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return prefs.getBoolean("isLoggedIn", false)
    }

    fun getUserId(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return prefs.getInt("id_usuario", 0)
    }

    fun getRole(context: Context): String {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return prefs.getString("rol", "SUPERVISOR") ?: "SUPERVISOR"
    }

    fun getToken(context: Context): String {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return prefs.getString("api_token", "") ?: ""
    }

    fun saveToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        prefs.edit().putString("api_token", token).apply()
    }

    fun authHeaders(context: Context): MutableMap<String, String> {
        val token = getToken(context)
        val headers = HashMap<String, String>()
        if (token.isNotEmpty()) headers["Authorization"] = "Bearer $token"
        return headers
    }
}