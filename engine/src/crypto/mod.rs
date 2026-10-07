pub mod pqxdh;
pub mod ratchet;

use thiserror::Error;

#[derive(Error, Debug)]
pub enum CryptoError {
    #[error("Key exchange failed: {0}")]
    KeyExchange(String),
    #[error("Authentication or signature verification failed")]
    SignatureVerification,
    #[error("Decryption failed / invalid MAC authentication tag")]
    DecryptionFailed,
    #[error("Invalid key length or encoding")]
    InvalidKey,
}
