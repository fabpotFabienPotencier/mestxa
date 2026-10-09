package com.mestxa.app.network

import android.content.Context
import android.telephony.TelephonyManager
import android.util.Log
import com.mestxa.app.engine.MestxaBridge
import com.mestxa.app.engine.PrekeyBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

data class CountryDto(
    val iso: String,
    val name: String,
    val callingCode: String,
    val digits: Int
) {
    val flagEmoji: String
        get() {
            if (iso.length != 2) return "🌐"
            val firstChar = Character.codePointAt(iso.uppercase(), 0) - 0x41 + 0x1F1E6
            val secondChar = Character.codePointAt(iso.uppercase(), 1) - 0x41 + 0x1F1E6
            return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
        }
}

data class NumberOfferDto(
    val number: String,
    val country: CountryDto,
    val offerToken: String,
    val expiresIn: Long
)

data class UsernameCheckDto(
    val available: Boolean,
    val username: String? = null,
    val reason: String? = null
)

data class DirectoryEntryDto(
    val userHex: String,
    val number: String,
    val username: String,
    val name: String
)

/**
 * Production REST client for Mestxa relay directory, number allocation, and identity management.
 */
class MestxaApiClient(
    private val baseUrl: String = "https://api.mestxa.com"
) {
    companion object {
        private const val TAG = "MestxaApiClient"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        @Volatile
        private var instance: MestxaApiClient? = null

        fun getInstance(): MestxaApiClient {
            return instance ?: synchronized(this) {
                instance ?: MestxaApiClient().also { instance = it }
            }
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Fetch the list of all supported countries with calling codes and national digit lengths.
     */
    suspend fun getCountries(): List<CountryDto> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/v1/numbers/countries")
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val array = JSONArray(bodyStr)
                val list = ArrayList<CountryDto>(array.length())
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        CountryDto(
                            iso = obj.getString("iso"),
                            name = obj.getString("name"),
                            callingCode = obj.getString("calling_code"),
                            digits = obj.getInt("digits")
                        )
                    )
                }
                list
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch countries: ${e.message}")
            emptyList()
        }
    }

    /**
     * Fetch country hint derived from IP connection.
     */
    suspend fun getGeoCountry(): CountryDto? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/v1/geo")
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyStr = response.body?.string() ?: return@withContext null
                val obj = JSONObject(bodyStr)
                if (obj.isNull("country")) return@withContext null
                val c = obj.getJSONObject("country")
                CountryDto(
                    iso = c.getString("iso"),
                    name = c.getString("name"),
                    callingCode = c.getString("calling_code"),
                    digits = c.getInt("digits")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get geo hint: ${e.message}")
            null
        }
    }

    /**
     * Request an offered candidate MX number for the given country.
     */
    suspend fun offerNumber(countryIso: String): NumberOfferDto? = withContext(Dispatchers.IO) {
        val jsonPayload = JSONObject().apply {
            put("country", countryIso)
        }.toString()

        val request = Request.Builder()
            .url("$baseUrl/v1/numbers/offer")
            .post(jsonPayload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Offer number failed with code ${response.code}")
                    return@withContext null
                }
                val bodyStr = response.body?.string() ?: return@withContext null
                val obj = JSONObject(bodyStr)
                val c = obj.getJSONObject("country")
                val country = CountryDto(
                    iso = c.getString("iso"),
                    name = c.getString("name"),
                    callingCode = c.getString("calling_code"),
                    digits = c.getInt("digits")
                )
                NumberOfferDto(
                    number = obj.getString("number"),
                    country = country,
                    offerToken = obj.getString("offer_token"),
                    expiresIn = obj.optLong("expires_in", 300)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to offer number: ${e.message}")
            null
        }
    }

    /**
     * Check if a chosen @username is available.
     */
    suspend fun checkUsername(username: String): UsernameCheckDto = withContext(Dispatchers.IO) {
        val clean = username.trim().trimStart('@')
        if (clean.length < 3) {
            return@withContext UsernameCheckDto(available = false, reason = "At least 3 characters")
        }

        val request = Request.Builder()
            .url("$baseUrl/v1/username/check/$clean")
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: return@withContext UsernameCheckDto(false, reason = "No response")
                val obj = JSONObject(bodyStr)
                UsernameCheckDto(
                    available = obj.optBoolean("available", false),
                    username = obj.optString("username", clean),
                    reason = obj.optString("reason", null)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check username: ${e.message}")
            UsernameCheckDto(available = false, reason = "Connection error")
        }
    }

    /**
     * Register account with Ed25519 signature proof over identity.
     */
    suspend fun registerAccount(
        userHex: String,
        number: String,
        offerToken: String,
        username: String,
        name: String,
        countryIso: String,
        privateKey: ByteArray
    ): Result<DirectoryEntryDto> = withContext(Dispatchers.IO) {
        val ts = System.currentTimeMillis() / 1000
        val cleanUname = username.trim().trimStart('@').lowercase()
        val challenge = "mestxa-register:$number:$cleanUname:${userHex.lowercase()}:$ts"

        val sigHex = try {
            val sigBytes = MestxaBridge.sign(privateKey, challenge.toByteArray(Charsets.UTF_8))
            MestxaBridge.bytesToHex(sigBytes)
        } catch (e: Exception) {
            return@withContext Result.failure(Exception("Failed to sign registration: ${e.message}"))
        }

        val jsonPayload = JSONObject().apply {
            put("user_hex", userHex.lowercase())
            put("number", number)
            put("offer_token", offerToken)
            put("username", cleanUname)
            put("name", name)
            put("country", countryIso.uppercase())
            put("ts", ts)
            put("sig", sigHex)
        }.toString()

        val request = Request.Builder()
            .url("$baseUrl/v1/account/register")
            .post(jsonPayload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errMsg = try {
                        JSONObject(bodyStr).optString("error", "Registration failed with code ${response.code}")
                    } catch (_: Exception) {
                        "Registration failed (${response.code})"
                    }
                    return@withContext Result.failure(Exception(errMsg))
                }
                val obj = JSONObject(bodyStr)
                Result.success(
                    DirectoryEntryDto(
                        userHex = userHex.lowercase(),
                        number = obj.getString("number"),
                        username = obj.getString("username"),
                        name = obj.optString("name", name)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Registration request failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Upload ML-KEM-768 + X25519 prekey bundle for post-quantum key exchange.
     */
    suspend fun uploadPrekeys(
        userHex: String,
        bundle: PrekeyBundle,
        username: String?
    ): Boolean = withContext(Dispatchers.IO) {
        val jsonPayload = JSONObject().apply {
            put("user_hex", userHex.lowercase())
            put("identity_key_hex", MestxaBridge.bytesToHex(bundle.identityKey))
            put("signed_prekey_hex", MestxaBridge.bytesToHex(bundle.signedPrekey))
            put("signed_prekey_sig_hex", MestxaBridge.bytesToHex(bundle.signature))
            put("kyber_prekey_hex", MestxaBridge.bytesToHex(bundle.kyberPublicKey))
            put("kyber_sig_hex", MestxaBridge.bytesToHex(bundle.kyberSignature))
            put("one_time_prekeys_hex", JSONArray())
            if (!username.isNullOrBlank()) {
                put("username", username.trim().trimStart('@').lowercase())
            }
        }.toString()

        val request = Request.Builder()
            .url("$baseUrl/v1/prekeys/upload")
            .post(jsonPayload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            httpClient.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload prekeys: ${e.message}")
            false
        }
    }

    /**
     * Resolve @username or MX-number to a directory entry containing public identity key.
     */
    suspend fun lookupDirectory(handle: String): DirectoryEntryDto? = withContext(Dispatchers.IO) {
        val clean = handle.trim()
        val request = Request.Builder()
            .url("$baseUrl/v1/directory/$clean")
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyStr = response.body?.string() ?: return@withContext null
                val obj = JSONObject(bodyStr)
                DirectoryEntryDto(
                    userHex = obj.getString("user_hex"),
                    number = obj.getString("number"),
                    username = obj.getString("username"),
                    name = obj.optString("name", obj.getString("username"))
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Directory lookup failed: ${e.message}")
            null
        }
    }

    /**
     * Fetch public prekey bundle for a user (@username or user_hex) from relay.
     */
    suspend fun fetchPrekeys(target: String): PrekeyBundle? = withContext(Dispatchers.IO) {
        val clean = target.trim()
        val request = Request.Builder()
            .url("$baseUrl/v1/prekeys/$clean")
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyStr = response.body?.string() ?: return@withContext null
                val obj = JSONObject(bodyStr)
                val otHex = obj.optString("one_time_prekey_hex", "")
                val otks = if (otHex.isNotEmpty()) listOf(MestxaBridge.hexToBytes(otHex)) else emptyList()
                PrekeyBundle(
                    identityKey = MestxaBridge.hexToBytes(obj.getString("identity_key_hex")),
                    signedPrekey = MestxaBridge.hexToBytes(obj.getString("signed_prekey_hex")),
                    signature = MestxaBridge.hexToBytes(obj.getString("signed_prekey_sig_hex")),
                    kyberPublicKey = MestxaBridge.hexToBytes(obj.getString("kyber_prekey_hex")),
                    kyberSignature = MestxaBridge.hexToBytes(obj.getString("kyber_sig_hex")),
                    oneTimePrekeys = otks
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch prekeys for $clean: ${e.message}")
            null
        }
    }
}
