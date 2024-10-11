package com.github.xingray.kotlinandroidbase.cache

import android.content.Context
import android.content.SharedPreferences
import com.github.xingray.kotlinbase.cache.Cache

class SharedPreferenceFileCache(context: Context, name: String) : Cache {

    val sharedPreferences: SharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    override fun getString(key: String, defaultValue: String?): String? {
        return sharedPreferences.getString(key, defaultValue)
    }

    override fun putString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }
}