package com.melihberat.mobiluyg

import android.content.Context

object UserPrefs {
    private const val PREFS = "app_prefs"
    private const val KEY_USERNAME = "PREF_USERNAME"  // prefs için tek anahtar

    fun getName(context: Context): String? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_USERNAME, null)
    }

    fun setName(context: Context, name: String) {
        // commit: anında diske yaz
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USERNAME, name)
            .commit()
    }
}
