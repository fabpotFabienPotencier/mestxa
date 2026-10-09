package com.mestxa.app.network

import android.util.Log
import com.mestxa.app.engine.MestxaBridge
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

import android.content.Context
import com.mestxa.app.MestxaApplication
import com.mestxa.app.storage.ConversationRecord
import com.mestxa.app.storage.DatabaseManager
import com.mestxa.app.storage.MessageRecord
import com.mestxa.app.storage.VaultManager
import com.mestxa.app.webrtc.WebRtcCallManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject

class MestxaNetworkService(
    private val serverBaseUrl: String = "wss://api.mestxa.com/v1/gateway",
    private var clientPublicKeyHex: String = "",
    private var clientPrivateKey: ByteArray? = null
) {
    companion object {
        private const val TAG = "MestxaNetwork"
        @Volatile
        private var instance: MestxaNetworkService? = null

        fun getInstance(context: Context? = null): MestxaNetworkService {
            return instance ?: synchronized(this) {
                instance ?: MestxaNetworkService().also { svc ->
                    instance = svc
                    try {
                        val ctx = context ?: MestxaApplication.instance
                        svc.refreshCredentials(ctx)
                    } catch (e: Exception) {
                        Log.w(TAG, "Deferred credential initialization: ${e.message}")
                    }
                }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var webSocket: WebSocket? = null
    private val isConnected = AtomicBoolean(false)

    private val _incomingMessages = MutableSharedFlow<MessageEnvelope>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<MessageEnvelope> = _incomingMessages.asSharedFlow()

    private val _incomingCallSignals = MutableSharedFlow<CallSignal>(extraBufferCapacity = 16)
    val incomingCallSignals: SharedFlow<CallSignal> = _incomingCallSignals.asSharedFlow()

    private val _deliveryAcks = MutableSharedFlow<DeliveryAck>(extraBufferCapacity = 64)
    val deliveryAcks: SharedFlow<DeliveryAck> = _deliveryAcks.asSharedFlow()

    fun refreshCredentials(context: Context) {
        try {
            val bundle = VaultManager.getOrCreateIdentityBundle(context)
            clientPublicKeyHex = MestxaBridge.bytesToHex(bundle.identityKey).lowercase()
            clientPrivateKey = bundle.identityPrivateKey
            Log.i(TAG, "Network credentials loaded for identity: $clientPublicKeyHex")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load identity credentials: ${e.message}")
        }
    }

    fun connect(context: Context? = null) {
        if (isConnected.get()) return

        if (clientPublicKeyHex.isEmpty() || clientPrivateKey == null) {
            try {
                refreshCredentials(context ?: MestxaApplication.instance)
            } catch (e: Exception) {
                Log.w(TAG, "Credential refresh on connect failed: ${e.message}")
            }
        }

        if (clientPublicKeyHex.isEmpty()) {
            Log.w(TAG, "Skipping connect: User not yet registered with an identity bundle.")
            return
        }

        val ts = System.currentTimeMillis() / 1000
        val sigParam = if (clientPrivateKey != null && MestxaBridge.isReady()) {
            try {
                val authMsg = "mestxa-auth:$clientPublicKeyHex:$ts".toByteArray(Charsets.UTF_8)
                val sig = MestxaBridge.sign(clientPrivateKey!!, authMsg)
                "&sig=${MestxaBridge.bytesToHex(sig)}"
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sign handshake challenge: ${e.message}")
                ""
            }
        } else ""

        val separator = if (serverBaseUrl.contains("?")) "&" else "?"
        val connectUrl = "$serverBaseUrl${separator}user=$clientPublicKeyHex&ts=$ts$sigParam"

        val request = Request.Builder()
            .url(connectUrl)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected.set(true)
                Log.i(TAG, "Authenticated WebSocket connected to $connectUrl")
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val rawBytes = bytes.toByteArray()
                val wirePayload = WireFrameSerializer.decode(rawBytes)
                if (wirePayload != null) {
                    when (wirePayload) {
                        is WireFramePayload.EnvelopePayload -> {
                            handleIncomingEnvelope(wirePayload.envelope)
                        }
                        is WireFramePayload.AckPayload -> {
                            scope.launch { _deliveryAcks.emit(wirePayload.ack) }
                        }
                        is WireFramePayload.CallSignalPayload -> {
                            scope.launch {
                                _incomingCallSignals.emit(wirePayload.callSignal)
                                try {
                                    WebRtcCallManager.getInstance(MestxaApplication.instance)
                                        .handleIncomingSignal(wirePayload.callSignal)
                                } catch (e: Exception) {
                                    Log.w(TAG, "Call signal dispatch warning: ${e.message}")
                                }
                            }
                        }
                        is WireFramePayload.PingPayload -> {
                            val pongBytes = WireFrameSerializer.encode(WireFramePayload.PongPayload(wirePayload.timestampMs))
                            webSocket.send(pongBytes.toByteString())
                        }
                        is WireFramePayload.PongPayload -> {
                            Log.d(TAG, "Heartbeat pong received")
                        }
                    }
                } else {
                    Log.w(TAG, "Received unparseable wire frame (${rawBytes.size} bytes)")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "WebSocket closing: $code / $reason")
                webSocket.close(1000, null)
                isConnected.set(false)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket connection failed: ${t.message}")
                isConnected.set(false)
                scope.launch {
                    delay(3000)
                    connect()
                }
            }
        })
    }

    /**
     * Send encrypted envelope to recipient via relay
     */
    fun sendEnvelope(envelope: MessageEnvelope): Boolean {
        val wireBytes = WireFrameSerializer.encode(WireFramePayload.EnvelopePayload(envelope))
        return webSocket?.send(wireBytes.toByteString()) ?: false
    }

    /**
     * Dispatch delivery ACK to relay, which immediately deletes the ciphertext from server memory
     */
    fun sendDeliveryAck(messageId: String, senderIdentityKey: ByteArray) {
        val ack = DeliveryAck(
            messageId = messageId,
            recipientId = senderIdentityKey,
            status = AckStatus.DELIVERED,
            timestampMs = System.currentTimeMillis()
        )
        val wireBytes = WireFrameSerializer.encode(WireFramePayload.AckPayload(ack))
        webSocket?.send(wireBytes.toByteString())
        Log.i(TAG, "Instant delivery ACK transmitted for message: $messageId")
    }

    /**
     * Send WebRTC Call Signaling envelope
     */
    fun sendCallSignal(signal: CallSignal): Boolean {
        val wireBytes = WireFrameSerializer.encode(WireFramePayload.CallSignalPayload(signal))
        return webSocket?.send(wireBytes.toByteString()) ?: false
    }

    /**
     * Decrypt and persist incoming envelope to SQLite database, then notify UI
     */
     private fun handleIncomingEnvelope(env: MessageEnvelope) {
        scope.launch {
            try {
                val ctx = MestxaApplication.instance
                val db = DatabaseManager.getInstance(ctx)
                val senderHex = MestxaBridge.bytesToHex(env.senderIdentityKey).lowercase()

                // Decrypt PQXDH / Kyber payload
                val decryptedBytes = MestxaBridge.decrypt(
                    senderIdentityKey = env.senderIdentityKey,
                    payload = com.mestxa.app.engine.EncryptedPayload(
                        ephemeralKey = env.ephemeralDhKey,
                        kyberCiphertext = env.kyberCiphertext,
                        ciphertext = env.ciphertext,
                        nonce = env.iv
                    )
                )
                val rawText = String(decryptedBytes, Charsets.UTF_8)
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val timeStr = timeFormat.format(Date(env.timestampMs))

                var msgText = rawText
                var cardType = "text"
                var cardData = ""
                var quote = ""

                try {
                    if (rawText.startsWith("{") && rawText.endsWith("}")) {
                        val json = JSONObject(rawText)
                        msgText = json.optString("text", rawText)
                        cardType = json.optString("cardType", "text")
                        cardData = json.optString("cardData", "")
                        quote = json.optString("quote", "")
                    }
                } catch (_: Exception) {}

                // Save message in local vault
                val record = MessageRecord(
                    id = env.messageId,
                    conversationId = senderHex,
                    senderKeyHex = senderHex,
                    isFromMe = false,
                    content = msgText,
                    timestamp = timeStr,
                    createdAtMs = env.timestampMs,
                    isDelivered = true,
                    isRead = false,
                    cardType = cardType,
                    cardData = cardData,
                    quoteReply = quote
                )
                db.saveMessage(record)

                // Update / create conversation
                val existing = db.getConversation(senderHex)
                val contact = db.getContact(senderHex)
                val displayName = contact?.name ?: existing?.contactName ?: ("MX-" + senderHex.take(8).uppercase())

                val updatedConv = existing?.copy(
                    lastMessage = if (cardType == "text") msgText else cardType.uppercase(),
                    timestamp = timeStr,
                    updatedAtMs = env.timestampMs,
                    unreadCount = (existing.unreadCount + 1)
                ) ?: ConversationRecord(
                    id = senderHex,
                    contactName = displayName,
                    contactPublicKeyHex = senderHex,
                    lastMessage = if (cardType == "text") msgText else cardType.uppercase(),
                    timestamp = timeStr,
                    updatedAtMs = env.timestampMs,
                    unreadCount = 1
                )
                db.saveConversation(updatedConv)

                // Transmit delivery ACK to relay
                sendDeliveryAck(env.messageId, env.senderIdentityKey)

                // Notify active screen
                _incomingMessages.emit(env)
                Log.i(TAG, "Decrypted & stored incoming message ${env.messageId} from $senderHex")
            } catch (e: Exception) {
                Log.e(TAG, "Failed handling incoming envelope: ${e.message}")
            }
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "Client initiated disconnect")
        isConnected.set(false)
    }
}
