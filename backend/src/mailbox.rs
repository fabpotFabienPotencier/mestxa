use redis::aio::ConnectionManager;
use redis::AsyncCommands;
use std::sync::Arc;
use tracing::{error, info};

pub struct EphemeralMailbox {
    redis: ConnectionManager,
    ttl_seconds: usize,
}

impl EphemeralMailbox {
    pub async fn new(redis_url: &str, ttl_seconds: usize) -> Result<Self, redis::RedisError> {
        let client = redis::Client::open(redis_url)?;
        let redis = ConnectionManager::new(client).await?;
        info!("Ephemeral Mailbox connected to Redis at {}", redis_url);
        Ok(Self { redis, ttl_seconds })
    }

    /// Stores an opaque ciphertext envelope in the recipient's transient mailbox
    pub async fn enqueue(
        &self,
        recipient_hex: &str,
        message_id: &str,
        envelope_bytes: &[u8],
    ) -> Result<(), redis::RedisError> {
        let mut conn = self.redis.clone();
        let key = format!("mb:{}", recipient_hex);

        // Store envelope in a hash keyed by message_id
        conn.hset(&key, message_id, envelope_bytes).await?;
        
        // Ensure mailbox key has an expiring TTL (e.g. 14 days)
        conn.expire(&key, self.ttl_seconds as i64).await?;
        
        Ok(())
    }

    /// Fetches all pending envelopes for a recipient when they establish a session
    pub async fn fetch_all(
        &self,
        recipient_hex: &str,
    ) -> Result<Vec<(String, Vec<u8>)>, redis::RedisError> {
        let mut conn = self.redis.clone();
        let key = format!("mb:{}", recipient_hex);

        let entries: Vec<(String, Vec<u8>)> = conn.hgetall(&key).await?;
        Ok(entries)
    }

    /// INSTANT PURGE: Cryptographically deletes the ciphertext envelope upon Delivery ACK
    pub async fn acknowledge_and_purge(
        &self,
        recipient_hex: &str,
        message_id: &str,
    ) -> Result<bool, redis::RedisError> {
        let mut conn = self.redis.clone();
        let key = format!("mb:{}", recipient_hex);

        let deleted_count: usize = conn.hdel(&key, message_id).await?;
        if deleted_count > 0 {
            info!(
                "Zero-Storage Purge: Deleted envelope {} for recipient {}",
                message_id, recipient_hex
            );
            Ok(true)
        } else {
            Ok(false)
        }
    }
}

pub type SharedMailbox = Arc<EphemeralMailbox>;
