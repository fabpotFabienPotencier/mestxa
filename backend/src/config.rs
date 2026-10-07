use std::env;
use std::net::SocketAddr;
use std::path::Path;

#[derive(Clone, Debug)]
pub struct ServerConfig {
    pub bind_addr: SocketAddr,
    pub redis_url: String,
    pub envelope_ttl_seconds: usize,
    pub max_envelopes_per_user: usize,
    pub max_payload_bytes: usize,
}

impl Default for ServerConfig {
    fn default() -> Self {
        let port: u16 = env::var("PORT")
            .ok()
            .and_then(|p| p.parse().ok())
            .unwrap_or(8080);

        let bind_addr = SocketAddr::from(([127, 0, 0, 1], port));

        // Prefer native Unix domain socket for 2x speed and zero TCP/IP overhead
        let redis_url = if let Ok(custom) = env::var("REDIS_URL") {
            custom
        } else if Path::new("/var/run/redis/redis.sock").exists() {
            "unix:///var/run/redis/redis.sock".to_string()
        } else {
            "redis://127.0.0.1:6379".to_string()
        };

        let envelope_ttl_seconds = env::var("ENVELOPE_TTL_SECONDS")
            .ok()
            .and_then(|t| t.parse().ok())
            .unwrap_or(14 * 24 * 3600); // 14 Days default

        let max_envelopes_per_user = 5000;
        let max_payload_bytes = 64 * 1024; // 64KB for text/metadata envelopes (media is offloaded to blob storage)

        Self {
            bind_addr,
            redis_url,
            envelope_ttl_seconds,
            max_envelopes_per_user,
            max_payload_bytes,
        }
    }
}
