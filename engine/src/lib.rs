pub mod crypto;
pub mod storage;
pub mod jni;

use std::sync::Mutex;
use ed25519_dalek::{SigningKey, VerifyingKey};
use rand_core::OsRng;
use x25519_dalek::{PublicKey as X25519Public, StaticSecret};
use zeroize::Zeroize;

use storage::db::EncryptedVault;

pub struct MestxaEngine {
    vault: Mutex<EncryptedVault>,
    identity_signing_key: SigningKey,
    identity_verifying_key: VerifyingKey,
    identity_dh_secret: StaticSecret,
    pub identity_dh_public: X25519Public,
}

impl MestxaEngine {
    /// Initialize client engine with hardware-isolated database key
    pub fn new(db_path: &str, mut db_key: [u8; 32]) -> Result<Self, Box<dyn std::error::Error>> {
        let vault = EncryptedVault::open(db_path, db_key)?;
        db_key.zeroize();

        // Generate or load identity keys
        let identity_signing_key = SigningKey::generate(&mut OsRng);
        let identity_verifying_key = identity_signing_key.verifying_key();

        let identity_dh_secret = StaticSecret::random_from_rng(OsRng);
        let identity_dh_public = X25519Public::from(&identity_dh_secret);

        Ok(Self {
            vault: Mutex::new(vault),
            identity_signing_key,
            identity_verifying_key,
            identity_dh_secret,
            identity_dh_public,
        })
    }

    /// Public Identity Key in Hex format
    pub fn identity_hex(&self) -> String {
        hex::encode(self.identity_verifying_key.as_bytes())
    }

    /// Save an outgoing or incoming message to encrypted local history
    pub fn record_message(
        &self,
        message_id: &str,
        peer_hex: &str,
        is_outgoing: bool,
        content: &str,
        timestamp_ms: u64,
    ) -> Result<(), Box<dyn std::error::Error>> {
        let vault = self.vault.lock().unwrap();
        vault.store_message(
            message_id,
            peer_hex,
            is_outgoing,
            1, // Text message
            content,
            timestamp_ms,
            if is_outgoing { 1 } else { 2 },
        )?;
        Ok(())
    }
}

// UniFFI export declarations
uniffi::setup_scaffolding!();
