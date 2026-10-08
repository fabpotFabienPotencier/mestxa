use axum::{
    extract::{
        ws::WebSocketUpgrade,
        Path, Query, State,
    },
    http::{HeaderMap, StatusCode},
    response::{IntoResponse, Json},
    routing::{get, post},
    Router,
};
use serde::{Deserialize, Serialize};
use std::sync::Arc;
use tower_http::cors::{Any, CorsLayer};
use tower_http::trace::TraceLayer;
use tracing::{error, info};

mod config;
mod directory;
mod gateway;
mod mailbox;
mod prekeys;

use config::ServerConfig;
use directory::{Directory, RegisterOutcome, SharedDirectory};
use gateway::{GatewayHub, SharedGateway};
use mailbox::EphemeralMailbox;
use prekeys::{PrekeyRegistry, SharedPrekeys};

use ed25519_dalek::{Signature, VerifyingKey, Verifier};

struct AppState {
    gateway: SharedGateway,
    prekeys: SharedPrekeys,
    directory: SharedDirectory,
}

#[derive(Deserialize)]
struct GatewayQuery {
    user: String, // Hex-encoded 32-byte public identity key
    ts: Option<u64>,
    sig: Option<String>,
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // 1. Initialize structured logging
    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| "info,mestxa_relay=debug".into()),
        )
        .init();

    info!("Starting Mestxa Bare-Metal Relay Server...");

    // 2. Load configuration
    let config = ServerConfig::default();
    info!("Binding to: {}", config.bind_addr);

    // 3. Connect to Ephemeral Redis Mailbox (Unix socket or TCP)
    let mailbox = Arc::new(
        EphemeralMailbox::new(&config.redis_url, config.envelope_ttl_seconds).await?,
    );

    let redis_client = redis::Client::open(config.redis_url.clone())?;
    let redis_conn = redis::aio::ConnectionManager::new(redis_client).await?;
    let directory = Arc::new(Directory::new(redis_conn.clone()));
    let prekeys = Arc::new(PrekeyRegistry::new(redis_conn));

    // 4. Initialize Gateway Hub
    let gateway = Arc::new(GatewayHub::new(Arc::clone(&mailbox), Arc::clone(&prekeys)));

    let state = Arc::new(AppState { gateway, prekeys, directory });

    // 5. Build Axum Router
    let app = Router::new()
        .route("/health", get(health_check))
        .route("/v1/gateway", get(ws_gateway_handler))
        .route("/v1/prekeys/upload", post(upload_prekeys_handler))
        .route("/v1/prekeys/:target", get(fetch_prekeys_handler))
        // Accounts & Directory
        .route("/v1/numbers/countries", get(countries_handler))
        .route("/v1/geo", get(geo_handler))
        .route("/v1/numbers/offer", post(offer_number_handler))
        .route("/v1/username/check/:username", get(username_check_handler))
        .route("/v1/account/register", post(register_handler))
        .route("/v1/account/:user_hex", get(account_handler))
        .route("/v1/directory/:handle", get(directory_handler))
        .layer(
            CorsLayer::new()
                .allow_origin(Any)
                .allow_methods(Any)
                .allow_headers(Any),
        )
        .layer(TraceLayer::new_for_http())
        .with_state(state);

    // 6. Bind Native Listener
    let listener = tokio::net::TcpListener::bind(config.bind_addr).await?;
    info!("Mestxa Relay is running at http://{}", config.bind_addr);

    axum::serve(listener, app)
        .with_graceful_shutdown(shutdown_signal())
        .await?;

    info!("Mestxa Relay shut down gracefully.");
    Ok(())
}

async fn health_check() -> impl IntoResponse {
    (StatusCode::OK, "OK - Mestxa Relay Active")
}

async fn ws_gateway_handler(
    ws: WebSocketUpgrade,
    Query(query): Query<GatewayQuery>,
    State(state): State<Arc<AppState>>,
) -> impl IntoResponse {
    let user_hex = query.user.to_lowercase();
    if user_hex.len() != 64 {
        return (StatusCode::BAD_REQUEST, "Invalid 32-byte public key hex").into_response();
    }

    let user_bytes = match hex::decode(&user_hex) {
        Ok(b) if b.len() == 32 => {
            let mut arr = [0u8; 32];
            arr.copy_from_slice(&b);
            arr
        }
        _ => return (StatusCode::BAD_REQUEST, "Invalid 32-byte public key hex").into_response(),
    };

    let verifying_key = match VerifyingKey::from_bytes(&user_bytes) {
        Ok(k) => k,
        Err(_) => return (StatusCode::BAD_REQUEST, "Invalid Ed25519 public key").into_response(),
    };

    // If signature authentication is provided, verify it strictly with replay protection
    if let (Some(ts), Some(sig_hex)) = (query.ts, query.sig) {
        let now = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .map(|d| d.as_secs())
            .unwrap_or(0);

        if (now as i64 - ts as i64).abs() > 300 {
            return (StatusCode::UNAUTHORIZED, "Handshake timestamp expired").into_response();
        }

        let sig_bytes = match hex::decode(&sig_hex) {
            Ok(b) if b.len() == 64 => {
                let mut arr = [0u8; 64];
                arr.copy_from_slice(&b);
                arr
            }
            _ => return (StatusCode::UNAUTHORIZED, "Invalid signature format").into_response(),
        };

        let signature = Signature::from_bytes(&sig_bytes);
        let auth_msg = format!("mestxa-auth:{}:{}", user_hex, ts);
        if verifying_key.verify(auth_msg.as_bytes(), &signature).is_err() {
            return (StatusCode::UNAUTHORIZED, "Handshake signature verification failed").into_response();
        }
    }

    let gateway = Arc::clone(&state.gateway);
    ws.on_upgrade(move |socket| gateway.handle_socket(socket, user_hex))
}

#[derive(Deserialize)]
struct UploadPrekeysPayload {
    user_hex: String,
    identity_key_hex: String,
    signed_prekey_hex: String,
    signed_prekey_sig_hex: String,
    one_time_prekeys_hex: Vec<String>,
    kyber_prekey_hex: String,
    kyber_sig_hex: String,
    username: Option<String>,
    phone_hash: Option<String>,
}

async fn upload_prekeys_handler(
    State(state): State<Arc<AppState>>,
    Json(payload): Json<UploadPrekeysPayload>,
) -> impl IntoResponse {
    let ik = match hex::decode(&payload.identity_key_hex) {
        Ok(k) if k.len() == 32 => k,
        _ => return StatusCode::BAD_REQUEST,
    };
    let spk = match hex::decode(&payload.signed_prekey_hex) {
        Ok(k) => k,
        Err(_) => return StatusCode::BAD_REQUEST,
    };
    let spk_sig = match hex::decode(&payload.signed_prekey_sig_hex) {
        Ok(s) if s.len() == 64 => s,
        _ => return StatusCode::BAD_REQUEST,
    };
    let kyber = match hex::decode(&payload.kyber_prekey_hex) {
        Ok(k) => k,
        Err(_) => return StatusCode::BAD_REQUEST,
    };
    let kyber_sig = match hex::decode(&payload.kyber_sig_hex) {
        Ok(s) => s,
        Err(_) => return StatusCode::BAD_REQUEST,
    };

    // Verify cryptographic signatures using Ed25519 identity key
    let mut ik_bytes = [0u8; 32];
    ik_bytes.copy_from_slice(&ik);
    let verifying_key = match VerifyingKey::from_bytes(&ik_bytes) {
        Ok(k) => k,
        Err(_) => return StatusCode::BAD_REQUEST,
    };

    let mut spk_sig_bytes = [0u8; 64];
    spk_sig_bytes.copy_from_slice(&spk_sig);
    let spk_signature = Signature::from_bytes(&spk_sig_bytes);
    if verifying_key.verify(&spk, &spk_signature).is_err() {
        error!("Signed prekey signature verification failed for user {}", payload.user_hex);
        return StatusCode::UNAUTHORIZED;
    }

    if kyber_sig.len() == 64 {
        let mut kyber_sig_bytes = [0u8; 64];
        kyber_sig_bytes.copy_from_slice(&kyber_sig);
        let kyber_signature = Signature::from_bytes(&kyber_sig_bytes);
        if verifying_key.verify(&kyber, &kyber_signature).is_err() {
            error!("Kyber prekey signature verification failed for user {}", payload.user_hex);
            return StatusCode::UNAUTHORIZED;
        }
    }

    let otks: Vec<Vec<u8>> = payload
        .one_time_prekeys_hex
        .iter()
        .filter_map(|h| hex::decode(h).ok())
        .collect();

    let res = state
        .prekeys
        .upload_bundle(
            &payload.user_hex,
            &ik,
            &spk,
            &spk_sig,
            &otks,
            &kyber,
            &kyber_sig,
            payload.username.as_deref(),
            payload.phone_hash.as_deref(),
        )
        .await;

    match res {
        Ok(_) => StatusCode::OK,
        Err(e) => {
            error!("Failed to upload prekeys: {}", e);
            StatusCode::INTERNAL_SERVER_ERROR
        }
    }
}

#[derive(Serialize)]
struct PrekeyBundleResponse {
    identity_key_hex: String,
    signed_prekey_hex: String,
    signed_prekey_sig_hex: String,
    one_time_prekey_hex: Option<String>,
    kyber_prekey_hex: String,
    kyber_sig_hex: String,
    username: Option<String>,
}

async fn fetch_prekeys_handler(
    Path(target): Path<String>,
    State(state): State<Arc<AppState>>,
) -> impl IntoResponse {
    let user_hex = if target.starts_with('@') {
        match state.prekeys.resolve_username(&target).await {
            Ok(Some(hex)) => hex,
            _ => return (StatusCode::NOT_FOUND, "Username not found").into_response(),
        }
    } else {
        target
    };

    match state.prekeys.fetch_bundle(&user_hex).await {
        Ok(Some(b)) => {
            let resp = PrekeyBundleResponse {
                identity_key_hex: hex::encode(b.identity_key),
                signed_prekey_hex: hex::encode(b.signed_prekey),
                signed_prekey_sig_hex: hex::encode(b.signed_prekey_signature),
                one_time_prekey_hex: b.one_time_prekey.map(|k| hex::encode(k)),
                kyber_prekey_hex: hex::encode(b.kyber_prekey),
                kyber_sig_hex: hex::encode(b.kyber_signature),
                username: None,
            };
            Json(resp).into_response()
        }
        Ok(None) => (StatusCode::NOT_FOUND, "User prekeys not found").into_response(),
        Err(e) => {
            error!("Database error fetching prekeys: {}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, "Server Error").into_response()
        }
    }
}

// ---------------------------------------------------------------------------
// Accounts & Directory Handlers
// ---------------------------------------------------------------------------

fn now_secs() -> u64 {
    std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .map(|d| d.as_secs())
        .unwrap_or(0)
}

fn err_json(status: StatusCode, msg: &str) -> axum::response::Response {
    (status, Json(serde_json::json!({ "error": msg }))).into_response()
}

async fn countries_handler() -> impl IntoResponse {
    Json(directory::all_countries())
}

async fn geo_handler(headers: HeaderMap) -> impl IntoResponse {
    let iso = ["cf-ipcountry", "x-country-code"]
        .iter()
        .filter_map(|h| headers.get(*h).and_then(|v| v.to_str().ok()))
        .map(|s| s.trim().to_uppercase())
        .find(|s| s.len() == 2 && s != "XX" && s != "T1");
    let info = iso.as_deref().and_then(directory::country);
    Json(serde_json::json!({ "country": info }))
}

#[derive(Deserialize)]
struct OfferPayload {
    country: String,
}

async fn offer_number_handler(
    State(state): State<Arc<AppState>>,
    Json(payload): Json<OfferPayload>,
) -> axum::response::Response {
    let Some(c) = directory::country(&payload.country) else {
        return err_json(StatusCode::BAD_REQUEST, "Unsupported country");
    };
    match state.directory.offer(c).await {
        Ok(Some(offer)) => Json(offer).into_response(),
        Ok(None) => err_json(StatusCode::SERVICE_UNAVAILABLE, "Could not allocate number, try again"),
        Err(e) => {
            error!("Number offer error: {}", e);
            err_json(StatusCode::INTERNAL_SERVER_ERROR, "Server error")
        }
    }
}

async fn username_check_handler(
    Path(username): Path<String>,
    State(state): State<Arc<AppState>>,
) -> axum::response::Response {
    let u = match directory::normalize_username(&username) {
        Ok(u) => u,
        Err(reason) => {
            return Json(serde_json::json!({ "available": false, "reason": reason })).into_response();
        }
    };
    match state.directory.username_available(&u).await {
        Ok(true) => Json(serde_json::json!({ "available": true, "username": u })).into_response(),
        Ok(false) => Json(serde_json::json!({ "available": false, "reason": "Username is taken" })).into_response(),
        Err(e) => {
            error!("Username check error: {}", e);
            err_json(StatusCode::INTERNAL_SERVER_ERROR, "Server error")
        }
    }
}

#[derive(Deserialize)]
struct RegisterPayload {
    user_hex: String,
    number: String,
    offer_token: String,
    username: String,
    #[serde(default)]
    name: String,
    country: String,
    ts: u64,
    sig: String,
}

async fn register_handler(
    State(state): State<Arc<AppState>>,
    Json(p): Json<RegisterPayload>,
) -> axum::response::Response {
    let user_hex = p.user_hex.to_lowercase();
    let Some(number) = directory::normalize_number(&p.number) else {
        return err_json(StatusCode::BAD_REQUEST, "Invalid MX number");
    };
    let username = match directory::normalize_username(&p.username) {
        Ok(u) => u,
        Err(reason) => return err_json(StatusCode::BAD_REQUEST, reason),
    };
    let Some(c) = directory::country(&p.country) else {
        return err_json(StatusCode::BAD_REQUEST, "Unsupported country");
    };
    if !number.starts_with(&format!("MX-{}-", c.calling_code)) {
        return err_json(StatusCode::BAD_REQUEST, "Number does not match country");
    }
    let name: String = p.name.trim().chars().take(64).collect();

    if (now_secs() as i64 - p.ts as i64).abs() > 300 {
        return err_json(StatusCode::UNAUTHORIZED, "Registration timestamp expired");
    }

    let key_bytes: [u8; 32] = match hex::decode(&user_hex) {
        Ok(b) if b.len() == 32 => b.try_into().unwrap(),
        _ => return err_json(StatusCode::BAD_REQUEST, "Invalid identity key"),
    };
    let Ok(vk) = VerifyingKey::from_bytes(&key_bytes) else {
        return err_json(StatusCode::BAD_REQUEST, "Invalid identity key");
    };
    let sig_bytes: [u8; 64] = match hex::decode(&p.sig) {
        Ok(b) if b.len() == 64 => b.try_into().unwrap(),
        _ => return err_json(StatusCode::UNAUTHORIZED, "Invalid signature"),
    };
    let msg = format!("mestxa-register:{}:{}:{}:{}", number, username, user_hex, p.ts);
    if vk.verify(msg.as_bytes(), &Signature::from_bytes(&sig_bytes)).is_err() {
        return err_json(StatusCode::UNAUTHORIZED, "Signature verification failed");
    }

    match state
        .directory
        .register(&user_hex, &number, &p.offer_token, &username, &name, c.iso, now_secs())
        .await
    {
        Ok(RegisterOutcome::Ok) => {
            info!("Registered {} as @{}", number, username);
            Json(serde_json::json!({ "number": number, "username": username, "name": name })).into_response()
        }
        Ok(RegisterOutcome::OfferInvalid) => {
            err_json(StatusCode::GONE, "Number offer expired, request a new number")
        }
        Ok(RegisterOutcome::NumberTaken) => err_json(StatusCode::CONFLICT, "Number already taken"),
        Ok(RegisterOutcome::UsernameTaken) => err_json(StatusCode::CONFLICT, "Username is taken"),
        Ok(RegisterOutcome::AlreadyRegistered) => {
            err_json(StatusCode::CONFLICT, "This identity is already registered")
        }
        Err(e) => {
            error!("Registration error: {}", e);
            err_json(StatusCode::INTERNAL_SERVER_ERROR, "Server error")
        }
    }
}

async fn account_handler(
    Path(user_hex): Path<String>,
    State(state): State<Arc<AppState>>,
) -> axum::response::Response {
    match state.directory.account(&user_hex.to_lowercase()).await {
        Ok(Some(entry)) => Json(entry).into_response(),
        Ok(None) => err_json(StatusCode::NOT_FOUND, "Account not found"),
        Err(e) => {
            error!("Account fetch error: {}", e);
            err_json(StatusCode::INTERNAL_SERVER_ERROR, "Server error")
        }
    }
}

async fn directory_handler(
    Path(handle): Path<String>,
    State(state): State<Arc<AppState>>,
) -> axum::response::Response {
    match state.directory.lookup(&handle).await {
        Ok(Some(entry)) => Json(entry).into_response(),
        Ok(None) => err_json(StatusCode::NOT_FOUND, "No Mestxa user with that username or number"),
        Err(e) => {
            error!("Directory lookup error: {}", e);
            err_json(StatusCode::INTERNAL_SERVER_ERROR, "Server error")
        }
    }
}

async fn shutdown_signal() {
    let ctrl_c = async {
        tokio::signal::ctrl_c()
            .await
            .expect("failed to install Ctrl+C handler");
    };

    #[cfg(unix)]
    let terminate = async {
        tokio::signal::unix::signal(tokio::signal::unix::SignalKind::terminate())
            .expect("failed to install signal handler")
            .recv()
            .await;
    };

    #[cfg(not(unix))]
    let terminate = std::future::pending::<()>();

    tokio::select! {
        _ = ctrl_c => {},
        _ = terminate => {},
    }
}
