package com.xiao.qqautothumbsup

import android.app.Application
import android.content.Context
import android.content.SharedPreferences

class SpUtil private constructor() {
    companion object {
        val instance by lazy { SpUtil() }
    }

    private lateinit var context: Context
    lateinit var preferences: SharedPreferences
        private set
    private lateinit var editor: SharedPreferences.Editor

    fun init(context: Context) {
        this.context = context.applicationContext
        preferences = context.getSharedPreferences(context.packageName, Application.MODE_PRIVATE)
        editor = preferences.edit()
    }

    fun put(key: String, value: Any) {
        when (value) {
            is String -> editor.putString(key, value)
            is Int -> editor.putInt(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is Float -> editor.putFloat(key, value)
            is Long -> editor.putLong(key, value)
            else -> throw IllegalArgumentException("SharedPreferences can not save this type.")
        }
        editor.apply()
    }

    inline fun <reified T : Any> get(key: String, defaultValue: T): T? = runCatching {
        when (defaultValue) {
            is String -> preferences.getString(key, defaultValue)
            is Int -> preferences.getInt(key, defaultValue)
            is Boolean -> preferences.getBoolean(key, defaultValue)
            is Float -> preferences.getFloat(key, defaultValue)
            is Long -> preferences.getLong(key, defaultValue)
            else -> throw IllegalArgumentException("SharedPreferences can not get this type.")
        } as? T
    }.getOrNull()
}