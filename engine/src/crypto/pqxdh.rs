use ed25519_dalek::{Signature, VerifyingKey};
use hkdf::Hkdf;
use ml_kem::{
    kem::{Encapsulate, EncapsulationKey},
    Encoded, EncodedSizeUser, MlKem768Params,
};
use rand_core::OsRng;
use sha2::Sha256;
use x25519_dalek::{EphemeralSecret, PublicKey as X25519Public, StaticSecret};
use zeroize::{Zeroize, ZeroizeOnDrop};

use super::CryptoError;

#[derive(Zeroize, ZeroizeOnDrop)]
pub struct PqxdhSecretSession {
    pub master_key: [u8; 32],
}

pub struct PqxdhInitiatorResult {
    pub session: PqxdhSecretSession,
    pub ephemeral_public: [u8; 32],
    pub kyber_ciphertext: Vec<u8>,
}

/// Initiator (Alice) computes the PQXDH master key to talk to Bob
pub fn initiate_pqxdh(
    alice_identity_secret: &StaticSecret,
    bob_identity_public: &[u8; 32],
    bob_signed_prekey: &[u8; 32],
    bob_signed_prekey_sig: &[u8; 64],
    bob_one_time_prekey: Option<&[u8; 32]>,
    bob_kyber_prekey: &[u8],
) -> Result<PqxdhInitiatorResult, CryptoError> {
    // 1. Verify Bob's signed prekey using his Ed25519 Identity Key
    let bob_verifying_key = VerifyingKey::from_bytes(bob_identity_public)
        .map_err(|_| CryptoError::SignatureVerification)?;
    let sig = Signature::from_bytes(bob_signed_prekey_sig);
    bob_verifying_key
        .verify_strict(bob_signed_prekey, &sig)
        .map_err(|_| CryptoError::SignatureVerification)?;

    // 2. Generate Alice's ephemeral Curve25519 keypair
    let alice_ephemeral = EphemeralSecret::random_from_rng(OsRng);
    let alice_ephemeral_public = X25519Public::from(&alice_ephemeral);

    // 3. Compute classical Diffie-Hellman combinations
    let bob_spk = X25519Public::from(*bob_signed_prekey);
    let bob_ik = X25519Public::from(*bob_identity_public);

    let dh1 = alice_identity_secret.diffie_hellman(&bob_spk);
    let dh2 = alice_ephemeral.diffie_hellman(&bob_ik);

    // Regenerate ephemeral for subsequent calculations or derive using static
    let alice_ephemeral_static = StaticSecret::random_from_rng(OsRng);
    let alice_ephemeral_static_pub = X25519Public::from(&alice_ephemeral_static);
    let dh3 = alice_ephemeral_static.diffie_hellman(&bob_spk);

    let mut dh_combined = Vec::with_capacity(160);
    dh_combined.extend_from_slice(dh1.as_bytes());
    dh_combined.extend_from_slice(dh2.as_bytes());
    dh_combined.extend_from_slice(dh3.as_bytes());

    if let Some(otk) = bob_one_time_prekey {
        let bob_otk = X25519Public::from(*otk);
        let dh4 = alice_ephemeral_static.diffie_hellman(&bob_otk);
        dh_combined.extend_from_slice(dh4.as_bytes());
    }

    // 4. Post-Quantum KEM Encapsulation (ML-KEM-768)
    if bob_kyber_prekey.len() < 1184 {
        return Err(CryptoError::KeyExchange("Invalid Kyber prekey length".into()));
    }
    let mut encoded_key = Encoded::<EncapsulationKey<MlKem768Params>>::default();
    encoded_key.copy_from_slice(&bob_kyber_prekey[..1184]);
    let encapsulation_key = EncapsulationKey::<MlKem768Params>::from_bytes(&encoded_key);
    let (kyber_ciphertext, kyber_shared_secret) = encapsulation_key
        .encapsulate(&mut OsRng)
        .map_err(|_| CryptoError::KeyExchange("Kyber encapsulation failed".into()))?;

    dh_combined.extend_from_slice(kyber_shared_secret.as_slice());

    // 5. Derive Master Shared Key using HKDF-SHA256
    let salt = [0u8; 32];
    let hk = Hkdf::<Sha256>::new(Some(&salt), &dh_combined);
    let mut master_key = [0u8; 32];
    hk.expand(b"Mestxa-PQXDH-v1-MasterKey", &mut master_key)
        .map_err(|_| CryptoError::KeyExchange("HKDF expansion failed".into()))?;

    // Zeroize sensitive temporary combined buffers
    dh_combined.zeroize();

    Ok(PqxdhInitiatorResult {
        session: PqxdhSecretSession { master_key },
        ephemeral_public: *alice_ephemeral_static_pub.as_bytes(),
        kyber_ciphertext: kyber_ciphertext.as_slice().to_vec(),
    })
}
