pub mod db;

use thiserror::Error;

#[derive(Error, Debug)]
pub enum StorageError {
    #[error("Database error: {0}")]
    Sqlite(#[from] rusqlite::Error),
    #[error("Encryption key error: {0}")]
    KeyDerivation(String),
    #[error("Session not found")]
    NotFound,
}
