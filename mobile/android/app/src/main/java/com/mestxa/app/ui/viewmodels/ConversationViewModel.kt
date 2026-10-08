package com.mestxa.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mestxa.app.MestxaApplication
import com.mestxa.app.engine.MestxaBridge
import com.mestxa.app.network.MestxaNetworkService
import com.mestxa.app.network.MessageEnvelope
import com.mestxa.app.storage.DatabaseManager
import com.mestxa.app.storage.MessageRecord
import com.mestxa.app.storage.VaultManager
import com.mestxa.app.ui.screens.MessageBubble
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ConversationViewModel(
    private val contactName: String,
    private val networkService: MestxaNetworkService = MestxaNetworkService.getInstance()
) : ViewModel() {

    private val context = MestxaApplication.instance
    private val db = DatabaseManager.getInstance(context)
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val recipientKey = VaultManager.getRecipientKey(contactName)

    private val _messages = MutableStateFlow<List<MessageBubble>>(emptyList())
    val messages: StateFlow<List<MessageBubble>> = _messages.asStateFlow()

    init {
        // 1. Load real saved messages from local vault
        loadMessages()

        // 2. Connect network service if not already connected
        networkService.connect()

        // 3. Observe real-time network messages
        viewModelScope.launch {
            networkService.incomingMessages.collect { envelope ->
                try {
                    val decryptedBytes = MestxaBridge.decrypt(
                        senderIdentityKey = envelope.senderIdentityKey,
                        payload = com.mestxa.app.engine.EncryptedPayload(
                            ephemeralKey = envelope.ephemeralDhKey,
                            kyberCiphertext = envelope.kyberCiphertext,
                            ciphertext = envelope.ciphertext,
                            nonce = envelope.iv
                        )
                    )
                    val decryptedText = String(decryptedBytes, Charsets.UTF_8)
                    val timeStr = timeFormat.format(Date(envelope.timestampMs))

                    // Persist incoming message to local vault
                    val record = MessageRecord(
                        id = envelope.messageId,
                        conversationId = contactName,
                        senderKeyHex = MestxaBridge.bytesToHex(envelope.senderIdentityKey),
                        isFromMe = false,
                        content = decryptedText,
                        timestamp = timeStr,
                        createdAtMs = envelope.timestampMs,
                        isDelivered = true,
                        isRead = true
                    )
                    db.saveMessage(record)

                    val newBubble = MessageBubble(
                        id = envelope.messageId,
                        text = decryptedText,
                        isOutgoing = false,
                        timestamp = timeStr,
                        isRead = true
                    )
                    _messages.value = _messages.value + newBubble
                } catch (_: Exception) {
                    // Not for this conversation or decryption failed
                }
            }
        }
    }

    private fun loadMessages() {
        val records = db.getMessages(contactName)
        _messages.value = records.map {
            MessageBubble(
                id = it.id,
                text = it.content,
                isOutgoing = it.isFromMe,
                timestamp = it.timestamp,
                isVoiceNote = it.cardType == "vn",
                isRead = it.isRead
            )
        }
    }

    /**
     * Encrypt message with PQXDH + Double Ratchet, persist to DB, and transmit over WebSocket
     */
    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val msgId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val timeStr = timeFormat.format(Date(timestamp))
        val plainBytes = trimmed.toByteArray(Charsets.UTF_8)

        // Cryptographic core encryption with recipient key
        val encryptedPayload = MestxaBridge.encrypt(
            recipientIdentityKey = recipientKey,
            plaintext = plainBytes
        )

        // Construct wire envelope
        val envelope = MessageEnvelope(
            messageId = msgId,
            senderIdentityKey = recipientKey,
            recipientIdentityKey = recipientKey,
            ephemeralDhKey = encryptedPayload.ephemeralKey,
            kyberCiphertext = encryptedPayload.kyberCiphertext,
            ciphertext = encryptedPayload.ciphertext,
            iv = encryptedPayload.nonce,
            timestampMs = timestamp
        )

        // Persist real message to local vault
        val record = MessageRecord(
            id = msgId,
            conversationId = contactName,
            senderKeyHex = MestxaBridge.bytesToHex(recipientKey),
            isFromMe = true,
            content = trimmed,
            timestamp = timeStr,
            createdAtMs = timestamp,
            isDelivered = true,
            isRead = true
        )
        db.saveMessage(record)

        // Append to UI state
        val bubble = MessageBubble(
            id = msgId,
            text = trimmed,
            isOutgoing = true,
            timestamp = timeStr,
            isRead = true
        )
        _messages.value = _messages.value + bubble

        // Transmit over wire
        networkService.sendEnvelope(envelope)
    }

    fun retractMessage(messageId: String) {
        db.deleteMessage(messageId, forEveryone = true)
        _messages.value = _messages.value.filter { it.id != messageId }
    }
}
