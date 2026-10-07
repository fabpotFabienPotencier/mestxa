use axum::extract::ws::{Message, WebSocket};
use bytes::Bytes;
use futures_util::{SinkExt, StreamExt};
use prost::Message as ProstMessage;
use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::{mpsc, RwLock};
use tracing::{error, info, warn};

use crate::mailbox::SharedMailbox;
use crate::prekeys::SharedPrekeys;

// Include generated Protocol Buffers code
pub mod proto {
    include!(concat!(env!("OUT_DIR"), "/mestxa.wire.rs"));
}

use proto::*;

pub struct GatewayHub {
    // Active connections: user_hex -> sender channel
    active_sessions: Arc<RwLock<HashMap<String, mpsc::Sender<Message>>>>,
    mailbox: SharedMailbox,
    prekeys: SharedPrekeys,
}

impl GatewayHub {
    pub fn new(mailbox: SharedMailbox, prekeys: SharedPrekeys) -> Self {
        Self {
            active_sessions: Arc::new(RwLock::new(HashMap::new())),
            mailbox,
            prekeys,
        }
    }

    /// Handles a new WebSocket connection from an authenticated client
    pub async fn handle_socket(self: Arc<Self>, mut socket: WebSocket, user_hex: String) {
        info!("Client connected to Gateway: {}", user_hex);
        let (mut ws_sender, mut ws_receiver) = socket.split();

        let (client_tx, mut client_rx) = mpsc::channel::<Message>(256);

        // Register session
        {
            let mut sessions = self.active_sessions.write().await;
            sessions.insert(user_hex.clone(), client_tx);
        }

        // Spawn task to forward outgoing channel messages to WebSocket
        let forward_task = tokio::spawn(async move {
            while let Some(msg) = client_rx.recv().await {
                if ws_sender.send(msg).await.is_err() {
                    break;
                }
            }
        });

        // Drain pending offline envelopes from the mailbox and deliver immediately
        if let Ok(pending) = self.mailbox.fetch_all(&user_hex).await {
            let sessions = self.active_sessions.read().await;
            if let Some(tx) = sessions.get(&user_hex) {
                for (_msg_id, raw_bytes) in pending {
                    let _ = tx.send(Message::Binary(raw_bytes)).await;
                }
            }
        }

        // Process incoming wire frames from client
        let hub = Arc::clone(&self);
        let current_user = user_hex.clone();

        while let Some(Ok(msg)) = ws_receiver.next().await {
            match msg {
                Message::Binary(bin_bytes) => {
                    if let Ok(wire_frame) = WireFrame::decode(Bytes::from(bin_bytes.clone())) {
                        hub.process_frame(&current_user, wire_frame, bin_bytes).await;
                    } else {
                        warn!("Received invalid protobuf payload from {}", current_user);
                    }
                }
                Message::Ping(p) => {
                    let sessions = hub.active_sessions.read().await;
                    if let Some(tx) = sessions.get(&current_user) {
                        let _ = tx.send(Message::Pong(p)).await;
                    }
                }
                Message::Close(_) => break,
                _ => {}
            }
        }

        // Cleanup on disconnect
        {
            let mut sessions = self.active_sessions.write().await;
            sessions.remove(&user_hex);
        }
        forward_task.abort();
        info!("Client disconnected from Gateway: {}", user_hex);
    }

    async fn process_frame(
        &self,
        sender_hex: &str,
        frame: WireFrame,
        raw_bytes: Vec<u8>,
    ) {
        match frame.payload {
            Some(wire_frame::Payload::Envelope(envelope)) => {
                let recipient_hex = hex::encode(&envelope.recipient_identity_key);
                let message_id = envelope.message_id.clone();

                let sessions = self.active_sessions.read().await;
                if let Some(recipient_tx) = sessions.get(&recipient_hex) {
                    // Recipient is online: stream immediately over direct socket
                    let _ = recipient_tx.send(Message::Binary(raw_bytes)).await;
                } else {
                    // Recipient is offline: enqueue in ephemeral store-and-forward mailbox
                    drop(sessions);
                    if let Err(e) = self.mailbox.enqueue(&recipient_hex, &message_id, &raw_bytes).await {
                        error!("Failed to enqueue message {}: {}", message_id, e);
                    }
                    // Trigger silent background push notification wakeup (APNs/FCM)
                    info!("Queued message {} for offline recipient {}", message_id, recipient_hex);
                }
            }

            Some(wire_frame::Payload::Ack(ack)) => {
                let recipient_hex = hex::encode(&ack.recipient_id);
                // INSTANT ZERO-STORAGE PURGE: Delete ciphertext from mailbox immediately
                let _ = self.mailbox.acknowledge_and_purge(&recipient_hex, &ack.message_id).await;

                // If original sender is online, notify them with delivery tick
                let sessions = self.active_sessions.read().await;
                if let Some(sender_tx) = sessions.get(sender_hex) {
                    let _ = sender_tx.send(Message::Binary(raw_bytes)).await;
                }
            }

            Some(wire_frame::Payload::CallSignal(signal)) => {
                let callee_hex = hex::encode(&signal.callee_id);
                let sessions = self.active_sessions.read().await;
                if let Some(callee_tx) = sessions.get(&callee_hex) {
                    // Real-time zero-delay relay of encrypted WebRTC call signals
                    let _ = callee_tx.send(Message::Binary(raw_bytes)).await;
                } else {
                    info!("Callee {} is offline for call {}", callee_hex, signal.call_id);
                }
            }

            Some(wire_frame::Payload::Ping(ping)) => {
                let sessions = self.active_sessions.read().await;
                if let Some(tx) = sessions.get(sender_hex) {
                    let pong_frame = WireFrame {
                        payload: Some(wire_frame::Payload::Pong(Heartbeat {
                            timestamp_ms: ping.timestamp_ms,
                        })),
                    };
                    let mut buf = Vec::new();
                    if pong_frame.encode(&mut buf).is_ok() {
                        let _ = tx.send(Message::Binary(buf)).await;
                    }
                }
            }

            _ => {}
        }
    }
}

pub type SharedGateway = Arc<GatewayHub>;
