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

class MestxaNetworkService(
    private val serverBaseUrl: String = "wss://api.mestxa.com/v1/gateway",
    private val clientPublicKeyHex: String = "0101010101010101010101010101010101010101010101010101010101010101",
    private val clientPrivateKey: ByteArray? = null
) {
    companion object {
        private const val TAG = "MestxaNetwork"
        @Volatile
        private var instance: MestxaNetworkService? = null

        fun getInstance(): MestxaNetworkService {
            return instance ?: synchronized(this) {
                instance ?: MestxaNetworkService().also { instance = it }
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

    fun connect() {
        if (isConnected.get()) return

        val ts = System.currentTimeMillis() / 1000
        val sigParam = if (clientPrivateKey != null && MestxaBridge.isReady()) {
            try {
                val authMsg = "mestxa-auth:$clientPublicKeyHex:$ts".toByteArray(Charsets.UTF_8)
                val sig = MestxaBridge.sign(clientPrivateKey, authMsg)
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
                            val env = wirePayload.envelope
                            scope.launch {
                                _incomingMessages.emit(env)
                                // Send delivery ACK back to trigger immediate server purge
                                sendDeliveryAck(env.messageId, env.senderIdentityKey)
                            }
                        }
                        is WireFramePayload.AckPayload -> {
                            scope.launch { _deliveryAcks.emit(wirePayload.ack) }
                        }
                        is WireFramePayload.CallSignalPayload -> {
                            scope.launch { _incomingCallSignals.emit(wirePayload.callSignal) }
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

    fun disconnect() {
        webSocket?.close(1000, "Client initiated disconnect")
        isConnected.set(false)
    }
}
