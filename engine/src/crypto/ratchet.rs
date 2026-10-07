use chacha20poly1305::{
    aead::{Aead, KeyInit},
    ChaCha20Poly1305, Nonce,
};
use hkdf::Hkdf;
use hmac::{Hmac, Mac};
use rand_core::OsRng;
use sha2::Sha256;
use x25519_dalek::{PublicKey as X25519Public, StaticSecret};
use zeroize::{Zeroize, ZeroizeOnDrop};

use super::CryptoError;

type HmacSha256 = Hmac<Sha256>;

#[derive(Zeroize, ZeroizeOnDrop)]
pub struct DoubleRatchetSession {
    pub root_key: [u8; 32],
    pub sending_chain_key: Option<[u8; 32]>,
    pub receiving_chain_key: Option<[u8; 32]>,
    #[zeroize(skip)]
    pub local_dh_secret: StaticSecret,
    #[zeroize(skip)]
    pub local_dh_public: X25519Public,
    #[zeroize(skip)]
    pub remote_dh_public: Option<X25519Public>,
    pub sending_counter: u32,
    pub receiving_counter: u32,
}

pub struct EncryptedMessage {
    pub dh_public: [u8; 32],
    pub sequence_number: u32,
    pub nonce: [u8; 12],
    pub ciphertext: Vec<u8>,
}

impl DoubleRatchetSession {
    /// Initialize Alice's side of the Double Ratchet after PQXDH
    pub fn init_alice(master_key: [u8; 32], bob_dh_public: [u8; 32]) -> Self {
        let local_dh_secret = StaticSecret::random_from_rng(OsRng);
        let local_dh_public = X25519Public::from(&local_dh_secret);
        let remote_dh_public = X25519Public::from(bob_dh_public);

        // Perform initial DH ratchet step
        let dh = local_dh_secret.diffie_hellman(&remote_dh_public);
        let (root_key, sending_chain_key) = kdf_rk(&master_key, dh.as_bytes());

        Self {
            root_key,
            sending_chain_key: Some(sending_chain_key),
            receiving_chain_key: None,
            local_dh_secret,
            local_dh_public,
            remote_dh_public: Some(remote_dh_public),
            sending_counter: 0,
            receiving_counter: 0,
        }
    }

    /// Encrypt a plaintext message with Forward Secrecy
    pub fn encrypt(&mut self, plaintext: &[u8]) -> Result<EncryptedMessage, CryptoError> {
        let chain_key = self
            .sending_chain_key
            .as_mut()
            .ok_or(CryptoError::KeyExchange("No sending chain established".into()))?;

        // 1. Advance the symmetric chain
        let (next_chain_key, message_key) = kdf_ck(chain_key);
        *chain_key = next_chain_key;

        // 2. Encrypt plaintext using ChaCha20-Poly1305 with random nonce
        let cipher = ChaCha20Poly1305::new_from_slice(&message_key)
            .map_err(|_| CryptoError::InvalidKey)?;
        
        let mut nonce_bytes = [0u8; 12];
        rand::RngCore::fill_bytes(&mut OsRng, &mut nonce_bytes);
        let nonce = Nonce::from_slice(&nonce_bytes);

        let ciphertext = cipher
            .encrypt(nonce, plaintext)
            .map_err(|_| CryptoError::DecryptionFailed)?;

        let seq = self.sending_counter;
        self.sending_counter += 1;

        Ok(EncryptedMessage {
            dh_public: *self.local_dh_public.as_bytes(),
            sequence_number: seq,
            nonce: nonce_bytes,
            ciphertext,
        })
    }

    /// Decrypt an incoming ciphertext message
    pub fn decrypt(
        &mut self,
        remote_dh: &[u8; 32],
        sequence_number: u32,
        nonce_bytes: &[u8; 12],
        ciphertext: &[u8],
    ) -> Result<Vec<u8>, CryptoError> {
        let incoming_dh = X25519Public::from(*remote_dh);

        // If remote has stepped their DH ratchet, perform DH step
        if self.remote_dh_public.as_ref() != Some(&incoming_dh) {
            self.dh_ratchet_step(incoming_dh);
        }

        let chain_key = self
            .receiving_chain_key
            .as_mut()
            .ok_or(CryptoError::KeyExchange("No receiving chain".into()))?;

        // Advance chain to this message
        let (next_chain_key, message_key) = kdf_ck(chain_key);
        *chain_key = next_chain_key;
        self.receiving_counter = sequence_number + 1;

        let cipher = ChaCha20Poly1305::new_from_slice(&message_key)
            .map_err(|_| CryptoError::InvalidKey)?;
        let nonce = Nonce::from_slice(nonce_bytes);

        let plaintext = cipher
            .decrypt(nonce, ciphertext)
            .map_err(|_| CryptoError::DecryptionFailed)?;

        Ok(plaintext)
    }

    fn dh_ratchet_step(&mut self, new_remote_dh: X25519Public) {
        self.remote_dh_public = Some(new_remote_dh);

        // Receiving DH ratchet step
        let dh_recv = self.local_dh_secret.diffie_hellman(&new_remote_dh);
        let (next_root, recv_chain) = kdf_rk(&self.root_key, dh_recv.as_bytes());
        self.root_key = next_root;
        self.receiving_chain_key = Some(recv_chain);

        // Generate new local keypair and sending step
        self.local_dh_secret = StaticSecret::random_from_rng(OsRng);
        self.local_dh_public = X25519Public::from(&self.local_dh_secret);

        let dh_send = self.local_dh_secret.diffie_hellman(&new_remote_dh);
        let (next_root_2, send_chain) = kdf_rk(&self.root_key, dh_send.as_bytes());
        self.root_key = next_root_2;
        self.sending_chain_key = Some(send_chain);
        self.sending_counter = 0;
    }
}

/// Derive next root key and chain key from DH output
fn kdf_rk(rk: &[u8; 32], dh_out: &[u8; 32]) -> ([u8; 32], [u8; 32]) {
    let hk = Hkdf::<Sha256>::new(Some(rk), dh_out);
    let mut out = [0u8; 64];
    hk.expand(b"Mestxa-Ratchet-RK-v1", &mut out).expect("HKDF expand");
    let mut next_rk = [0u8; 32];
    let mut chain_key = [0u8; 32];
    next_rk.copy_from_slice(&out[..32]);
    chain_key.copy_from_slice(&out[32..]);
    (next_rk, chain_key)
}

/// Advance symmetric chain and derive message key
fn kdf_ck(ck: &[u8; 32]) -> ([u8; 32], [u8; 32]) {
    let mut mac1 = HmacSha256::new_from_slice(ck).expect("HMAC init");
    mac1.update(&[0x01]);
    let message_key: [u8; 32] = mac1.finalize().into_bytes().into();

    let mut mac2 = HmacSha256::new_from_slice(ck).expect("HMAC init");
    mac2.update(&[0x02]);
    let next_ck: [u8; 32] = mac2.finalize().into_bytes().into();

    (next_ck, message_key)
}
