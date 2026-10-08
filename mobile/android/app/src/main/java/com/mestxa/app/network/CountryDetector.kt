package com.mestxa.app.network

import android.content.Context
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object CountryDetector {
    private const val TAG = "CountryDetector"

    /**
     * Resolves the user's country code in priority order:
     * 1. SIM Country ISO
     * 2. Network Country ISO
     * 3. Server GeoIP lookup
     * 4. System default Locale
     */
    suspend fun detectCountry(context: Context): String = withContext(Dispatchers.IO) {
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (tm != null) {
                val simCountry = tm.simCountryIso?.trim()?.uppercase()
                if (!simCountry.isNullOrBlank() && simCountry.length == 2) {
                    Log.d(TAG, "Country resolved via SIM: $simCountry")
                    return@withContext simCountry
                }

                val networkCountry = tm.networkCountryIso?.trim()?.uppercase()
                if (!networkCountry.isNullOrBlank() && networkCountry.length == 2) {
                    Log.d(TAG, "Country resolved via Network: $networkCountry")
                    return@withContext networkCountry
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Telephony country check failed: ${e.message}")
        }

        // Try GeoIP from relay
        try {
            val geo = MestxaApiClient.getInstance().getGeoCountry()
            if (geo != null && geo.iso.length == 2) {
                Log.d(TAG, "Country resolved via Relay GeoIP: ${geo.iso}")
                return@withContext geo.iso.uppercase()
            }
        } catch (e: Exception) {
            Log.w(TAG, "GeoIP fallback failed: ${e.message}")
        }

        // Fallback to Locale
        val localeCountry = Locale.getDefault().country?.trim()?.uppercase()
        if (!localeCountry.isNullOrBlank() && localeCountry.length == 2) {
            Log.d(TAG, "Country resolved via Locale: $localeCountry")
            return@withContext localeCountry
        }

        "US" // Global fallback
    }
}
