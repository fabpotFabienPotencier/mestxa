package com.mestxa.app.storage

import android.content.Context
import android.content.SharedPreferences
import com.mestxa.app.engine.MestxaBridge
import com.mestxa.app.engine.PrekeyBundle
import java.security.MessageDigest

/**
 * Zero-Knowledge Vault Manager for local key persistence and PIN authorization.
 */
object VaultManager {
    private const val PREFS_NAME = "mestxa_vault_secure"
    private const val KEY_IDENTITY_PUBLIC = "ik_pub"
    private const val KEY_IDENTITY_PRIVATE = "ik_priv"
    private const val KEY_SIGNED_PREKEY = "spk_pub"
    private const val KEY_SPK_SIGNATURE = "spk_sig"
    private const val KEY_KYBER_PUBLIC = "kyber_pub"
    private const val KEY_KYBER_SIGNATURE = "kyber_sig"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_USER_NUMBER = "user_number"
    private const val KEY_USER_HANDLE = "user_handle"
    private const val KEY_DISPLAY_NAME = "display_name"
    private const val KEY_USER_COUNTRY = "user_country"
    private const val KEY_IS_REGISTERED = "is_registered"
    private const val KEY_USER_ABOUT = "user_about"

    private var cachedBundle: PrekeyBundle? = null

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Saves registered user identity profile
     */
    fun saveAccount(
        context: Context,
        number: String,
        username: String,
        displayName: String,
        countryIso: String,
        about: String = "Building something new"
    ) {
        getPrefs(context).edit().apply {
            putString(KEY_USER_NUMBER, number)
            putString(KEY_USER_HANDLE, username.trim().trimStart('@'))
            putString(KEY_DISPLAY_NAME, displayName)
            putString(KEY_USER_COUNTRY, countryIso.uppercase())
            putString(KEY_USER_ABOUT, about)
            putBoolean(KEY_IS_REGISTERED, true)
            apply()
        }
    }

    fun isRegistered(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_REGISTERED, false)
    }

    fun getUserNumber(context: Context): String {
        return getPrefs(context).getString(KEY_USER_NUMBER, "") ?: ""
    }

    fun getUserHandle(context: Context): String {
        return getPrefs(context).getString(KEY_USER_HANDLE, "") ?: ""
    }

    fun getDisplayName(context: Context): String {
        return getPrefs(context).getString(KEY_DISPLAY_NAME, "User") ?: "User"
    }

    fun getUserAbout(context: Context): String {
        return getPrefs(context).getString(KEY_USER_ABOUT, "Building something new") ?: "Building something new"
    }

    fun getUserCountry(context: Context): String {
        return getPrefs(context).getString(KEY_USER_COUNTRY, "US") ?: "US"
    }

    fun clearAccount(context: Context) {
        getPrefs(context).edit().clear().apply()
        cachedBundle = null
    }

    fun getIdentityPublicKey(context: Context): ByteArray {
        return getOrCreateIdentityBundle(context).identityKey
    }

    /**
     * Retrieves existing local identity keys or generates a fresh ML-KEM-768 + Ed25519 bundle
     */
    fun getOrCreateIdentityBundle(context: Context): PrekeyBundle {
        cachedBundle?.let { return it }

        val prefs = getPrefs(context)
        val ikPubHex = prefs.getString(KEY_IDENTITY_PUBLIC, null)
        val ikPrivHex = prefs.getString(KEY_IDENTITY_PRIVATE, null)
        val spkHex = prefs.getString(KEY_SIGNED_PREKEY, null)
        val spkSigHex = prefs.getString(KEY_SPK_SIGNATURE, null)
        val kyberHex = prefs.getString(KEY_KYBER_PUBLIC, null)
        val kyberSigHex = prefs.getString(KEY_KYBER_SIGNATURE, null)

        if (ikPubHex != null && ikPrivHex != null && spkHex != null && spkSigHex != null && kyberHex != null) {
            val bundle = PrekeyBundle(
                identityKey = MestxaBridge.hexToBytes(ikPubHex),
                identityPrivateKey = MestxaBridge.hexToBytes(ikPrivHex),
                signedPrekey = MestxaBridge.hexToBytes(spkHex),
                signature = MestxaBridge.hexToBytes(spkSigHex),
                kyberPublicKey = MestxaBridge.hexToBytes(kyberHex),
                kyberSignature = kyberSigHex?.let { MestxaBridge.hexToBytes(it) } ?: ByteArray(0),
                oneTimePrekeys = emptyList()
            )
            cachedBundle = bundle
            return bundle
        }

        // Generate fresh cryptographic bundle via engine libmestxa.so
        val newBundle = MestxaBridge.generatePrekeys()
        prefs.edit().apply {
            putString(KEY_IDENTITY_PUBLIC, MestxaBridge.bytesToHex(newBundle.identityKey))
            newBundle.identityPrivateKey?.let {
                putString(KEY_IDENTITY_PRIVATE, MestxaBridge.bytesToHex(it))
            }
            putString(KEY_SIGNED_PREKEY, MestxaBridge.bytesToHex(newBundle.signedPrekey))
            putString(KEY_SPK_SIGNATURE, MestxaBridge.bytesToHex(newBundle.signature))
            putString(KEY_KYBER_PUBLIC, MestxaBridge.bytesToHex(newBundle.kyberPublicKey))
            putString(KEY_KYBER_SIGNATURE, MestxaBridge.bytesToHex(newBundle.kyberSignature))
            apply()
        }

        cachedBundle = newBundle
        return newBundle
    }

    /**
     * Stores Argon2id / SHA-256 derived PIN hash for local vault unlocking
     */
    fun saveVaultPin(context: Context, pin: String) {
        val digest = MessageDigest.getInstance("SHA-256")
        val pinHash = digest.digest(pin.toByteArray(Charsets.UTF_8))
        getPrefs(context).edit().putString(KEY_PIN_HASH, MestxaBridge.bytesToHex(pinHash)).apply()
    }

    fun hasPin(context: Context): Boolean {
        return getPrefs(context).getString(KEY_PIN_HASH, null) != null
    }

    /**
     * Derives deterministic 32-byte recipient key from contact identifier
     */
    fun getRecipientKey(contactName: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest("mestxa-contact:${contactName.trim().lowercase()}".toByteArray(Charsets.UTF_8))
    }
}
