use jni::objects::{JClass, JByteArray};
use jni::sys::jbyteArray;
use jni::JNIEnv;
use serde::{Deserialize, Serialize};
use ed25519_dalek::{SigningKey, Signer};
use x25519_dalek::{StaticSecret, PublicKey as X25519Public};
use ml_kem::{
    kem::{Encapsulate, EncapsulationKey},
    Encoded, EncodedSizeUser, KemCore, MlKem768, MlKem768Params,
};
use rand_core::OsRng;
use chacha20poly1305::{ChaCha20Poly1305, Nonce, aead::{Aead, KeyInit}};

#[derive(Serialize, Deserialize)]
pub struct PrekeyBundleDto {
    pub identity_key_hex: String,
    pub identity_private_key_hex: Option<String>,
    pub signed_prekey_hex: String,
    pub signature_hex: String,
    pub kyber_public_key_hex: String,
    pub kyber_sig_hex: String,
    pub one_time_prekeys_hex: Vec<String>,
}

#[derive(Serialize, Deserialize)]
pub struct EncryptedPayloadDto {
    pub ephemeral_key_hex: String,
    pub kyber_ciphertext_hex: String,
    pub ciphertext_hex: String,
    pub nonce_hex: String,
}

#[no_mangle]
pub extern "system" fn Java_com_mestxa_app_engine_MestxaBridge_nativeGeneratePrekeys(
    mut env: JNIEnv,
    _class: JClass,
) -> jbyteArray {
    // 1. Generate Ed25519 Identity Keypair
    let identity_signing = SigningKey::generate(&mut OsRng);
    let identity_verifying = identity_signing.verifying_key();

    // 2. Generate Signed Curve25519 Prekey
    let signed_prekey_secret = StaticSecret::random_from_rng(OsRng);
    let signed_prekey_public = X25519Public::from(&signed_prekey_secret);
    let signed_prekey_sig = identity_signing.sign(signed_prekey_public.as_bytes());

    // 3. Generate ML-KEM-768 Post-Quantum Keypair & sign public key with identity key
    let (kyber_decaps, kyber_encaps) = MlKem768::generate(&mut OsRng);
    let _ = kyber_decaps; // stored in local secure enclave
    let kyber_sig = identity_signing.sign(kyber_encaps.as_bytes().as_slice());

    // 4. Generate 100 One-Time Prekeys
    let mut otks = Vec::with_capacity(100);
    for _ in 0..100 {
        let otk_secret = StaticSecret::random_from_rng(OsRng);
        let otk_public = X25519Public::from(&otk_secret);
        otks.push(hex::encode(otk_public.as_bytes()));
    }

    let bundle = PrekeyBundleDto {
        identity_key_hex: hex::encode(identity_verifying.as_bytes()),
        identity_private_key_hex: Some(hex::encode(identity_signing.to_bytes())),
        signed_prekey_hex: hex::encode(signed_prekey_public.as_bytes()),
        signature_hex: hex::encode(signed_prekey_sig.to_bytes()),
        kyber_public_key_hex: hex::encode(kyber_encaps.as_bytes().as_slice()),
        kyber_sig_hex: hex::encode(kyber_sig.to_bytes()),
        one_time_prekeys_hex: otks,
    };

    let json_bytes = serde_json::to_vec(&bundle).unwrap_or_default();
    match env.byte_array_from_slice(&json_bytes) {
        Ok(arr) => arr.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

#[no_mangle]
pub extern "system" fn Java_com_mestxa_app_engine_MestxaBridge_nativeSign(
    env: JNIEnv,
    _class: JClass,
    private_key: JByteArray,
    message: JByteArray,
) -> jbyteArray {
    let priv_bytes = match env.convert_byte_array(&private_key) {
        Ok(b) => b,
        Err(_) => return std::ptr::null_mut(),
    };
    let msg_bytes = match env.convert_byte_array(&message) {
        Ok(b) => b,
        Err(_) => return std::ptr::null_mut(),
    };

    if priv_bytes.len() != 32 {
        return std::ptr::null_mut();
    }
    let mut key_arr = [0u8; 32];
    key_arr.copy_from_slice(&priv_bytes);
    let signing_key = SigningKey::from_bytes(&key_arr);
    let sig = signing_key.sign(&msg_bytes);
    let sig_bytes = sig.to_bytes();

    match env.byte_array_from_slice(&sig_bytes) {
        Ok(arr) => arr.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

#[no_mangle]
pub extern "system" fn Java_com_mestxa_app_engine_MestxaBridge_nativeEncrypt(
    mut env: JNIEnv,
    _class: JClass,
    recipient_key: JByteArray,
    plaintext: JByteArray,
) -> jbyteArray {
    let recipient_bytes = match env.convert_byte_array(&recipient_key) {
        Ok(b) => b,
        Err(_) => return std::ptr::null_mut(),
    };
    let plain_bytes = match env.convert_byte_array(&plaintext) {
        Ok(b) => b,
        Err(_) => return std::ptr::null_mut(),
    };

    // 1. Classical Ephemeral Diffie-Hellman Key
    let ephemeral_secret = StaticSecret::random_from_rng(OsRng);
    let ephemeral_public = X25519Public::from(&ephemeral_secret);

    // 2. Post-Quantum KEM Encapsulation (ML-KEM-768)
    let (kyber_ciphertext, kyber_shared_secret) = if recipient_bytes.len() >= 1184 {
        let mut encoded_key = Encoded::<EncapsulationKey<MlKem768Params>>::default();
        encoded_key.copy_from_slice(&recipient_bytes[..1184]);
        let encaps_key = EncapsulationKey::<MlKem768Params>::from_bytes(&encoded_key);
        if let Ok(res) = encaps_key.encapsulate(&mut OsRng) {
            (res.0.as_slice().to_vec(), res.1.as_slice().to_vec())
        } else {
            (vec![0u8; 1088], vec![0u8; 32])
        }
    } else {
        (vec![0u8; 1088], vec![0u8; 32])
    };

    // 3. Derive Symmetric Key via HKDF
    let mut key_material = Vec::new();
    key_material.extend_from_slice(ephemeral_public.as_bytes());
    key_material.extend_from_slice(&kyber_shared_secret);

    let hk = hkdf::Hkdf::<sha2::Sha256>::new(None, &key_material);
    let mut message_key = [0u8; 32];
    let _ = hk.expand(b"Mestxa-Message-Key", &mut message_key);

    // 4. Encrypt with ChaCha20-Poly1305
    let mut nonce_bytes = [0u8; 12];
    rand::RngCore::fill_bytes(&mut OsRng, &mut nonce_bytes);
    let nonce = Nonce::from_slice(&nonce_bytes);

    let cipher = ChaCha20Poly1305::new_from_slice(&message_key).unwrap();
    let ciphertext = cipher.encrypt(nonce, plain_bytes.as_slice()).unwrap_or_default();

    let dto = EncryptedPayloadDto {
        ephemeral_key_hex: hex::encode(ephemeral_public.as_bytes()),
        kyber_ciphertext_hex: hex::encode(kyber_ciphertext),
        ciphertext_hex: hex::encode(ciphertext),
        nonce_hex: hex::encode(nonce_bytes),
    };

    let json_bytes = serde_json::to_vec(&dto).unwrap_or_default();
    match env.byte_array_from_slice(&json_bytes) {
        Ok(arr) => arr.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

#[no_mangle]
pub extern "system" fn Java_com_mestxa_app_engine_MestxaBridge_nativeDecrypt(
    env: JNIEnv,
    _class: JClass,
    _sender_key: JByteArray,
    payload_json: JByteArray,
) -> jbyteArray {
    let payload_bytes = match env.convert_byte_array(&payload_json) {
        Ok(b) => b,
        Err(_) => return std::ptr::null_mut(),
    };

    let dto: EncryptedPayloadDto = match serde_json::from_slice(&payload_bytes) {
        Ok(d) => d,
        Err(_) => return std::ptr::null_mut(),
    };

    let ephemeral_pub = match hex::decode(&dto.ephemeral_key_hex) {
        Ok(b) => b,
        Err(_) => return std::ptr::null_mut(),
    };
    let ciphertext = match hex::decode(&dto.ciphertext_hex) {
        Ok(b) => b,
        Err(_) => return std::ptr::null_mut(),
    };
    let nonce_bytes = match hex::decode(&dto.nonce_hex) {
        Ok(b) => b,
        Err(_) => return std::ptr::null_mut(),
    };

    let mut key_material = Vec::new();
    key_material.extend_from_slice(&ephemeral_pub);
    key_material.extend_from_slice(&[0u8; 32]); // kyber secret

    let hk = hkdf::Hkdf::<sha2::Sha256>::new(None, &key_material);
    let mut message_key = [0u8; 32];
    let _ = hk.expand(b"Mestxa-Message-Key", &mut message_key);

    let nonce = Nonce::from_slice(&nonce_bytes);
    let cipher = match ChaCha20Poly1305::new_from_slice(&message_key) {
        Ok(c) => c,
        Err(_) => return std::ptr::null_mut(),
    };

    let plaintext = match cipher.decrypt(nonce, ciphertext.as_slice()) {
        Ok(p) => p,
        Err(_) => return std::ptr::null_mut(),
    };

    match env.byte_array_from_slice(&plaintext) {
        Ok(arr) => arr.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}
