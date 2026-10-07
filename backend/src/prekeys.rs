use redis::aio::ConnectionManager;
use redis::AsyncCommands;
use std::sync::Arc;
use tracing::info;

pub struct PrekeyRegistry {
    redis: ConnectionManager,
}

impl PrekeyRegistry {
    pub fn new(redis: ConnectionManager) -> Self {
        Self { redis }
    }

    /// Stores the client's public prekey material
    pub async fn upload_bundle(
        &self,
        user_hex: &str,
        identity_key: &[u8],
        signed_prekey: &[u8],
        signed_prekey_sig: &[u8],
        one_time_prekeys: &[Vec<u8>],
        kyber_prekey: &[u8],
        kyber_sig: &[u8],
        username: Option<&str>,
        phone_hash: Option<&str>,
    ) -> Result<(), redis::RedisError> {
        let mut conn = self.redis.clone();

        // 1. Store the persistent public prekeys
        let meta_key = format!("pk:meta:{}", user_hex);
        conn.hset_multiple(
            &meta_key,
            &[
                ("ik", identity_key),
                ("spk", signed_prekey),
                ("spk_sig", signed_prekey_sig),
                ("kyber", kyber_prekey),
                ("kyber_sig", kyber_sig),
            ],
        )
        .await?;

        // 2. Push one-time prekeys into a queue
        if !one_time_prekeys.is_empty() {
            let ot_key = format!("pk:ot:{}", user_hex);
            for ot in one_time_prekeys {
                conn.rpush(&ot_key, ot).await?;
            }
        }

        // 3. Optional lookup indices (username -> user_hex, phone_hash -> user_hex)
        if let Some(uname) = username {
            let uname_clean = uname.trim_start_matches('@').to_lowercase();
            conn.set(format!("idx:uname:{}", uname_clean), user_hex)
                .await?;
        }

        if let Some(phash) = phone_hash {
            conn.set(format!("idx:phone:{}", phash), user_hex).await?;
        }

        info!("Prekeys successfully registered for user {}", user_hex);
        Ok(())
    }

    /// Fetches a prekey bundle to initiate a PQXDH session with recipient
    pub async fn fetch_bundle(
        &self,
        user_hex: &str,
    ) -> Result<Option<FetchedBundle>, redis::RedisError> {
        let mut conn = self.redis.clone();
        let meta_key = format!("pk:meta:{}", user_hex);

        let exists: bool = conn.exists(&meta_key).await?;
        if !exists {
            return Ok(None);
        }

        let (identity_key, signed_prekey, signed_prekey_sig, kyber_prekey, kyber_sig): (
            Vec<u8>,
            Vec<u8>,
            Vec<u8>,
            Vec<u8>,
            Vec<u8>,
        ) = conn
            .hget(
                &meta_key,
                ("ik", "spk", "spk_sig", "kyber", "kyber_sig"),
            )
            .await?;

        // Pop a single one-time prekey from the pool
        let ot_key = format!("pk:ot:{}", user_hex);
        let one_time_prekey: Option<Vec<u8>> = conn.lpop(&ot_key, None).await?;

        Ok(Some(FetchedBundle {
            identity_key,
            signed_prekey,
            signed_prekey_signature: signed_prekey_sig,
            one_time_prekey,
            kyber_prekey,
            kyber_signature: kyber_sig,
        }))
    }

    /// Resolve an @username to a user hex identifier
    pub async fn resolve_username(&self, username: &str) -> Result<Option<String>, redis::RedisError> {
        let mut conn = self.redis.clone();
        let clean = username.trim_start_matches('@').to_lowercase();
        conn.get(format!("idx:uname:{}", clean)).await
    }
}

pub struct FetchedBundle {
    pub identity_key: Vec<u8>,
    pub signed_prekey: Vec<u8>,
    pub signed_prekey_signature: Vec<u8>,
    pub one_time_prekey: Option<Vec<u8>>,
    pub kyber_prekey: Vec<u8>,
    pub kyber_signature: Vec<u8>,
}

pub type SharedPrekeys = Arc<PrekeyRegistry>;
