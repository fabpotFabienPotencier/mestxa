use rusqlite::{params, Connection};
use std::path::Path;
use zeroize::Zeroize;

use super::StorageError;

pub struct EncryptedVault {
    conn: Connection,
}

impl EncryptedVault {
    /// Opens or creates an on-device SQLCipher encrypted database
    pub fn open<P: AsRef<Path>>(
        db_path: P,
        mut encryption_key: [u8; 32],
    ) -> Result<Self, StorageError> {
        let conn = Connection::open(db_path)?;

        // Apply SQLCipher raw encryption key
        let key_hex = hex::encode(encryption_key);
        conn.execute_batch(&format!("PRAGMA key = \"x'{}'\";", key_hex))?;
        
        // Zeroize key string in memory immediately
        encryption_key.zeroize();

        // Performance & integrity pragmas
        conn.execute_batch(
            "PRAGMA journal_mode = WAL;
             PRAGMA synchronous = NORMAL;
             PRAGMA foreign_keys = ON;",
        )?;

        let vault = Self { conn };
        vault.run_migrations()?;
        Ok(vault)
    }

    fn run_migrations(&self) -> Result<(), StorageError> {
        self.conn.execute_batch(
            "CREATE TABLE IF NOT EXISTS identity (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                identity_key BLOB NOT NULL,
                identity_secret BLOB NOT NULL,
                signed_prekey BLOB NOT NULL,
                signed_prekey_secret BLOB NOT NULL,
                signed_prekey_sig BLOB NOT NULL,
                kyber_prekey BLOB NOT NULL,
                kyber_secret BLOB NOT NULL,
                created_at INTEGER NOT NULL
            );

            CREATE TABLE IF NOT EXISTS sessions (
                peer_identity_hex TEXT PRIMARY KEY,
                root_key BLOB NOT NULL,
                sending_chain_key BLOB,
                receiving_chain_key BLOB,
                local_dh_secret BLOB NOT NULL,
                local_dh_public BLOB NOT NULL,
                remote_dh_public BLOB,
                sending_counter INTEGER NOT NULL,
                receiving_counter INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            );

            CREATE TABLE IF NOT EXISTS conversations (
                peer_identity_hex TEXT PRIMARY KEY,
                display_name TEXT NOT NULL,
                username TEXT,
                last_message_text TEXT,
                last_message_timestamp INTEGER NOT NULL,
                unread_count INTEGER DEFAULT 0
            );

            CREATE TABLE IF NOT EXISTS messages (
                message_id TEXT PRIMARY KEY,
                peer_identity_hex TEXT NOT NULL,
                is_outgoing BOOLEAN NOT NULL,
                message_type INTEGER NOT NULL,
                content TEXT NOT NULL,
                timestamp_ms INTEGER NOT NULL,
                status INTEGER NOT NULL, -- 0: Pending, 1: Sent, 2: Delivered, 3: Read
                FOREIGN KEY (peer_identity_hex) REFERENCES conversations(peer_identity_hex)
            );
            
            CREATE INDEX IF NOT EXISTS idx_messages_peer_timestamp 
            ON messages(peer_identity_hex, timestamp_ms DESC);",
        )?;
        Ok(())
    }

    /// Stores a message into local encrypted history
    pub fn store_message(
        &self,
        message_id: &str,
        peer_hex: &str,
        is_outgoing: bool,
        message_type: i32,
        content: &str,
        timestamp_ms: u64,
        status: i32,
    ) -> Result<(), StorageError> {
        self.conn.execute(
            "INSERT INTO messages (message_id, peer_identity_hex, is_outgoing, message_type, content, timestamp_ms, status)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)
             ON CONFLICT(message_id) DO UPDATE SET status = excluded.status;",
            params![message_id, peer_hex, is_outgoing, message_type, content, timestamp_ms, status],
        )?;

        // Update conversation row
        self.conn.execute(
            "INSERT INTO conversations (peer_identity_hex, display_name, last_message_text, last_message_timestamp, unread_count)
             VALUES (?1, ?1, ?2, ?3, CASE WHEN ?4 THEN 0 ELSE 1 END)
             ON CONFLICT(peer_identity_hex) DO UPDATE SET
                last_message_text = excluded.last_message_text,
                last_message_timestamp = excluded.last_message_timestamp,
                unread_count = CASE WHEN ?4 THEN conversations.unread_count ELSE conversations.unread_count + 1 END;",
            params![peer_hex, content, timestamp_ms, is_outgoing],
        )?;

        Ok(())
    }
}
