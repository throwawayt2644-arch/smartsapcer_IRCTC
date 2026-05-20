package com.meilluer.smartspacer_irctc.data

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("irctc_prefs", Context.MODE_PRIVATE)

    fun saveCredentials(email: String, password: String) {
        prefs.edit().apply {
            putString("email", email)
            putString("password", password)
            apply()
        }
    }

    fun getEmail(): String? = prefs.getString("email", null)
    fun getPassword(): String? = prefs.getString("password", null)
}
