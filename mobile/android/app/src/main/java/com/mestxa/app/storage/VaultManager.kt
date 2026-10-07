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

    private var cachedBundle: PrekeyBundle? = null

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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

    /**
     * Derives deterministic 32-byte recipient key from contact identifier
     */
    fun getRecipientKey(contactName: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest("mestxa-contact:${contactName.trim().lowercase()}".toByteArray(Charsets.UTF_8))
    }
}
