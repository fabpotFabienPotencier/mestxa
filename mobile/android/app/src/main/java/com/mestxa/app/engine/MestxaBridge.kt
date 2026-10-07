package com.mestxa.app.engine

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

data class IdentityKeyPair(
    val publicKey: ByteArray,
    val privateKey: ByteArray
)

data class PrekeyBundle(
    val identityKey: ByteArray,
    val identityPrivateKey: ByteArray? = null,
    val signedPrekey: ByteArray,
    val signature: ByteArray,
    val kyberPublicKey: ByteArray,
    val kyberSignature: ByteArray = ByteArray(0),
    val oneTimePrekeys: List<ByteArray>
)

data class EncryptedPayload(
    val ephemeralKey: ByteArray,
    val kyberCiphertext: ByteArray,
    val ciphertext: ByteArray,
    val nonce: ByteArray
)

object MestxaBridge {
    private const val TAG = "MestxaBridge"
    private var isNativeLoaded = false

    init {
        try {
            System.loadLibrary("mestxa")
            isNativeLoaded = true
            Log.i(TAG, "Native libmestxa successfully initialized.")
        } catch (e: UnsatisfiedLinkError) {
            isNativeLoaded = false
            Log.e(TAG, "CRITICAL: Native libmestxa.so failed to load: ${e.message}")
        }
    }

    fun isReady(): Boolean = isNativeLoaded

    /**
     * Post-Quantum Extended Triple Diffie-Hellman + Kyber-768 Prekey generation
     */
    fun generatePrekeys(): PrekeyBundle {
        check(isNativeLoaded) {
            "Cryptographic engine libmestxa.so is not initialized. Unencrypted operation strictly forbidden."
        }
        val jsonBytes = nativeGeneratePrekeys()
        val jsonStr = String(jsonBytes, Charsets.UTF_8)
        val obj = JSONObject(jsonStr)

        val otksJson = obj.getJSONArray("one_time_prekeys_hex")
        val otks = ArrayList<ByteArray>(otksJson.length())
        for (i in 0 until otksJson.length()) {
            otks.add(hexToBytes(otksJson.getString(i)))
        }

        val privHex = if (obj.has("identity_private_key_hex")) obj.getString("identity_private_key_hex") else null
        val kyberSigHex = if (obj.has("kyber_sig_hex")) obj.getString("kyber_sig_hex") else ""

        return PrekeyBundle(
            identityKey = hexToBytes(obj.getString("identity_key_hex")),
            identityPrivateKey = privHex?.let { hexToBytes(it) },
            signedPrekey = hexToBytes(obj.getString("signed_prekey_hex")),
            signature = hexToBytes(obj.getString("signature_hex")),
            kyberPublicKey = hexToBytes(obj.getString("kyber_public_key_hex")),
            kyberSignature = if (kyberSigHex.isNotEmpty()) hexToBytes(kyberSigHex) else ByteArray(0),
            oneTimePrekeys = otks
        )
    }

    /**
     * Double Ratchet ChaCha20-Poly1305 + PQXDH Message Encryption
     */
    fun encrypt(recipientIdentityKey: ByteArray, plaintext: ByteArray): EncryptedPayload {
        check(isNativeLoaded) {
            "Cryptographic engine libmestxa.so is not initialized. Plaintext transmission strictly forbidden."
        }
        val payloadJsonBytes = nativeEncrypt(recipientIdentityKey, plaintext)
        val jsonStr = String(payloadJsonBytes, Charsets.UTF_8)
        val obj = JSONObject(jsonStr)

        return EncryptedPayload(
            ephemeralKey = hexToBytes(obj.getString("ephemeral_key_hex")),
            kyberCiphertext = hexToBytes(obj.getString("kyber_ciphertext_hex")),
            ciphertext = hexToBytes(obj.getString("ciphertext_hex")),
            nonce = hexToBytes(obj.getString("nonce_hex"))
        )
    }

    /**
     * Decrypt incoming ciphertext envelope
     */
    fun decrypt(senderIdentityKey: ByteArray, payload: EncryptedPayload): ByteArray {
        check(isNativeLoaded) {
            "Cryptographic engine libmestxa.so is not initialized. Decryption failed."
        }
        val obj = JSONObject().apply {
            put("ephemeral_key_hex", bytesToHex(payload.ephemeralKey))
            put("kyber_ciphertext_hex", bytesToHex(payload.kyberCiphertext))
            put("ciphertext_hex", bytesToHex(payload.ciphertext))
            put("nonce_hex", bytesToHex(payload.nonce))
        }
        val payloadJsonBytes = obj.toString().toByteArray(Charsets.UTF_8)
        return nativeDecrypt(senderIdentityKey, payloadJsonBytes)
    }

    /**
     * Sign message with Ed25519 identity private key
     */
    fun sign(privateKey: ByteArray, message: ByteArray): ByteArray {
        check(isNativeLoaded) {
            "Cryptographic engine libmestxa.so is not initialized."
        }
        return nativeSign(privateKey, message)
    }

    fun hexToBytes(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789abcdef"
        val result = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val i = b.toInt() and 0xff
            result.append(hexChars[i shr 4])
            result.append(hexChars[i and 0x0f])
        }
        return result.toString()
    }

    // Native JNI Declarations implemented in libmestxa (engine/src/jni.rs)
    @JvmStatic
    private external fun nativeGeneratePrekeys(): ByteArray

    @JvmStatic
    private external fun nativeEncrypt(recipientKey: ByteArray, plaintext: ByteArray): ByteArray

    @JvmStatic
    private external fun nativeDecrypt(senderKey: ByteArray, payloadJson: ByteArray): ByteArray

    @JvmStatic
    private external fun nativeSign(privateKey: ByteArray, message: ByteArray): ByteArray
}
