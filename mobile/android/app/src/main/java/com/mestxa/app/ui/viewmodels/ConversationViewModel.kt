package com.mestxa.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mestxa.app.engine.MestxaBridge
import com.mestxa.app.network.EnvelopeType
import com.mestxa.app.network.MessageEnvelope
import com.mestxa.app.network.MestxaNetworkService
import com.mestxa.app.ui.screens.MessageBubble
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import com.mestxa.app.storage.VaultManager

class ConversationViewModel(
    private val contactName: String,
    private val networkService: MestxaNetworkService = MestxaNetworkService.getInstance()
) : ViewModel() {

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val recipientKey = VaultManager.getRecipientKey(contactName)

    private val _messages = MutableStateFlow<List<MessageBubble>>(
        listOf(
            MessageBubble("1", "Good morning! Have you looked at the revised layout specs?", false, "10:41 AM"),
            MessageBubble("2", "The team wants to finalize it before noon.", false, "10:42 AM"),
            MessageBubble("3", "Yes, just reviewed them. Everything looks solid.", true, "10:44 AM"),
            MessageBubble("4", "Voice message (0:14)", true, "10:45 AM", isVoiceNote = true),
            MessageBubble("5", "Awesome! Let's get the final assets packaged.", false, "10:47 AM")
        )
    )
    val messages: StateFlow<List<MessageBubble>> = _messages.asStateFlow()

    init {
        // Connect network service if not already connected
        networkService.connect()

        // Observe incoming real-time network messages
        viewModelScope.launch {
            networkService.incomingMessages.collect { envelope ->
                // Decrypt incoming message with MestxaBridge
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
                val newBubble = MessageBubble(
                    id = envelope.messageId,
                    text = decryptedText,
                    isOutgoing = false,
                    timestamp = timeFormat.format(Date(envelope.timestampMs)),
                    isRead = true
                )
                _messages.value = _messages.value + newBubble
            }
        }
    }

    /**
     * Encrypt message with PQXDH + Double Ratchet and transmit over WebSocket
     */
    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val msgId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val plainBytes = trimmed.toByteArray(Charsets.UTF_8)

        // Cryptographic core encryption with real recipient key
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
            messageType = com.mestxa.app.network.MessageType.TEXT,
            timestampMs = timestamp,
            sequenceNumber = 1
        )

        // Dispatch over live WebSocket to bare-metal relay
        networkService.sendEnvelope(envelope)

        // Append to UI state
        val outgoingBubble = MessageBubble(
            id = msgId,
            text = trimmed,
            isOutgoing = true,
            timestamp = timeFormat.format(Date(timestamp)),
            isRead = false
        )
        _messages.value = _messages.value + outgoingBubble
    }

    /**
     * Retract / delete message from conversation
     */
    fun retractMessage(messageId: String) {
        _messages.value = _messages.value.filterNot { it.id == messageId }
    }
}
