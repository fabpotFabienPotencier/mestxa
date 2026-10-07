package com.mestxa.app

import android.app.Application
import android.util.Log
import net.sqlcipher.database.SQLiteDatabase

class MestxaApplication : Application() {

    companion object {
        private const val TAG = "MestxaApp"
        lateinit var instance: MestxaApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize SQLCipher native cryptographic SQLite engine
        try {
            SQLiteDatabase.loadLibs(this)
            Log.i(TAG, "SQLCipher native libraries initialized successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SQLCipher native libraries: ${e.message}", e)
        }
    }
}
