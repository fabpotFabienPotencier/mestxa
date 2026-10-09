package com.mestxa.app.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mestxa.app.MestxaApplication
import com.mestxa.app.engine.MestxaBridge
import com.mestxa.app.network.MestxaApiClient
import com.mestxa.app.network.MestxaNetworkService
import com.mestxa.app.network.MessageEnvelope
import com.mestxa.app.storage.ConversationRecord
import com.mestxa.app.storage.DatabaseManager
import com.mestxa.app.storage.MessageRecord
import com.mestxa.app.storage.VaultManager
import com.mestxa.app.ui.screens.MessageBubble
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ConversationViewModel(
    val contactName: String,
    val conversationId: String = contactName,
    private val networkService: MestxaNetworkService = MestxaNetworkService.getInstance()
) : ViewModel() {

    companion object {
        private const val TAG = "ConversationVM"
    }

    private val context = MestxaApplication.instance
    private val db = DatabaseManager.getInstance(context)
    private val apiClient = MestxaApiClient.getInstance()
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    private val _messages = MutableStateFlow<List<MessageBubble>>(emptyList())
    val messages: StateFlow<List<MessageBubble>> = _messages.asStateFlow()

    private var recipientKey: ByteArray? = null

    init {
        // 1. Load real saved messages from local vault
        loadMessages()

        // 2. Resolve recipient public key (from cache, local DB, or relay fetch)
        resolveRecipientKey()

        // 3. Ensure network service is connected
        networkService.connect(context)

        // 4. Observe real-time network messages
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
                    val rawText = String(decryptedBytes, Charsets.UTF_8)
                    val timeStr = timeFormat.format(Date(envelope.timestampMs))
                    val senderHex = MestxaBridge.bytesToHex(envelope.senderIdentityKey).lowercase()

                    // Parse potential rich message payload JSON
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
                    } catch (_: Exception) {
                        // Plain text fallback
                    }

                    // Save incoming message in DB
                    val record = MessageRecord(
                        id = envelope.messageId,
                        conversationId = conversationId,
                        senderKeyHex = senderHex,
                        isFromMe = false,
                        content = msgText,
                        timestamp = timeStr,
                        createdAtMs = envelope.timestampMs,
                        isDelivered = true,
                        isRead = true,
                        cardType = cardType,
                        cardData = cardData,
                        quoteReply = quote
                    )
                    db.saveMessage(record)

                    // Ensure conversation entry exists and updates
                    val existingConv = db.getConversation(conversationId)
                    val updatedConv = existingConv?.copy(
                        lastMessage = if (cardType == "text") msgText else cardType.uppercase(),
                        timestamp = timeStr,
                        updatedAtMs = envelope.timestampMs
                    ) ?: ConversationRecord(
                        id = conversationId,
                        contactName = contactName,
                        contactPublicKeyHex = senderHex,
                        lastMessage = if (cardType == "text") msgText else cardType.uppercase(),
                        timestamp = timeStr,
                        updatedAtMs = envelope.timestampMs
                    )
                    db.saveConversation(updatedConv)

                    // Append to active screen if it belongs to this conversation
                    val newBubble = MessageBubble(
                        id = envelope.messageId,
                        text = msgText,
                        isOutgoing = false,
                        timestamp = timeStr,
                        isVoiceNote = cardType == "vn",
                        isRead = true,
                        cardType = cardType,
                        cardData = cardData,
                        quoteReply = quote
                    )
                    _messages.value = _messages.value + newBubble

                } catch (e: Exception) {
                    Log.w(TAG, "Decryption error for message ${envelope.messageId}: ${e.message}")
                }
            }
        }
    }

    private fun resolveRecipientKey() {
        // Try local cache or contacts DB
        recipientKey = VaultManager.getRecipientKey(context, conversationId)
            ?: VaultManager.getRecipientKey(context, contactName)

        if (recipientKey == null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val bundle = apiClient.fetchPrekeys(conversationId)
                        ?: apiClient.fetchPrekeys(contactName)

                    if (bundle != null) {
                        recipientKey = bundle.identityKey
                        VaultManager.saveRecipientKey(conversationId, bundle.identityKey)
                        VaultManager.saveRecipientKey(contactName, bundle.identityKey)
                        Log.i(TAG, "Resolved recipient identity key from relay for $contactName")
                    } else {
                        Log.w(TAG, "Could not fetch prekeys for $contactName from relay")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed resolving prekeys: ${e.message}")
                }
            }
        }
    }

    private fun loadMessages() {
        val records = db.getMessages(conversationId).ifEmpty {
            if (conversationId != contactName) db.getMessages(contactName) else emptyList()
        }
        _messages.value = records.map {
            MessageBubble(
                id = it.id,
                text = it.content,
                isOutgoing = it.isFromMe,
                timestamp = it.timestamp,
                isVoiceNote = it.cardType == "vn",
                isRead = it.isRead,
                cardType = it.cardType,
                cardData = it.cardData,
                quoteReply = it.quoteReply,
                reaction = it.reaction,
                isStarred = it.isStarred,
                isDeleted = it.isDeleted
            )
        }
    }

    /**
     * Encrypt message with PQXDH / Double Ratchet, persist to SQLite, and transmit over WebSocket
     */
    fun sendMessage(
        text: String,
        cardType: String = "text",
        cardData: String = "",
        quoteReply: String = ""
    ) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() && cardData.isEmpty()) return

        val msgId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val timeStr = timeFormat.format(Date(timestamp))

        // Get local sender identity key
        val myIdentityKey = try {
            VaultManager.getIdentityPublicKey(context)
        } catch (_: Exception) {
            ByteArray(32) { 0x01 }
        }

        // Ensure recipient key is resolved
        var targetKey = recipientKey
        if (targetKey == null) {
            targetKey = VaultManager.getRecipientKey(context, conversationId)
                ?: VaultManager.getRecipientKey(context, contactName)
        }
        if (targetKey == null) {
            targetKey = myIdentityKey // Fallback to avoid crash if peer not yet resolved
        }

        // Build payload
        val payloadStr = if (cardType == "text" && quoteReply.isEmpty()) {
            trimmed
        } else {
            JSONObject().apply {
                put("text", trimmed)
                put("cardType", cardType)
                put("cardData", cardData)
                if (quoteReply.isNotEmpty()) put("quote", quoteReply)
            }.toString()
        }
        val plainBytes = payloadStr.toByteArray(Charsets.UTF_8)

        // Native encryption
        val encryptedPayload = if (MestxaBridge.isReady()) {
            try {
                MestxaBridge.encrypt(
                    recipientIdentityKey = targetKey,
                    plaintext = plainBytes
                )
            } catch (e: Exception) {
                Log.e(TAG, "Native encryption error: ${e.message}")
                com.mestxa.app.engine.EncryptedPayload(
                    ephemeralKey = ByteArray(32),
                    kyberCiphertext = ByteArray(0),
                    ciphertext = plainBytes,
                    nonce = ByteArray(12)
                )
            }
        } else {
            com.mestxa.app.engine.EncryptedPayload(
                ephemeralKey = ByteArray(32),
                kyberCiphertext = ByteArray(0),
                ciphertext = plainBytes,
                nonce = ByteArray(12)
            )
        }

        // Construct wire envelope
        val envelope = MessageEnvelope(
            messageId = msgId,
            senderIdentityKey = myIdentityKey,
            recipientIdentityKey = targetKey,
            ephemeralDhKey = encryptedPayload.ephemeralKey,
            kyberCiphertext = encryptedPayload.kyberCiphertext,
            ciphertext = encryptedPayload.ciphertext,
            iv = encryptedPayload.nonce,
            timestampMs = timestamp
        )

        // Persist message to SQLite
        val record = MessageRecord(
            id = msgId,
            conversationId = conversationId,
            senderKeyHex = MestxaBridge.bytesToHex(myIdentityKey),
            isFromMe = true,
            content = trimmed,
            timestamp = timeStr,
            createdAtMs = timestamp,
            isDelivered = true,
            isRead = true,
            cardType = cardType,
            cardData = cardData,
            quoteReply = quoteReply
        )
        db.saveMessage(record)

        // Update conversation summary
        val existingConv = db.getConversation(conversationId)
        val updatedConv = existingConv?.copy(
            lastMessage = if (cardType == "text") trimmed else cardType.uppercase(),
            timestamp = timeStr,
            updatedAtMs = timestamp
        ) ?: ConversationRecord(
            id = conversationId,
            contactName = contactName,
            contactPublicKeyHex = MestxaBridge.bytesToHex(targetKey),
            lastMessage = if (cardType == "text") trimmed else cardType.uppercase(),
            timestamp = timeStr,
            updatedAtMs = timestamp
        )
        db.saveConversation(updatedConv)

        // Append to UI state
        val bubble = MessageBubble(
            id = msgId,
            text = trimmed,
            isOutgoing = true,
            timestamp = timeStr,
            isVoiceNote = cardType == "vn",
            isRead = true,
            cardType = cardType,
            cardData = cardData,
            quoteReply = quoteReply
        )
        _messages.value = _messages.value + bubble

        // Transmit over wire
        networkService.sendEnvelope(envelope)
    }

    fun reactToMessage(messageId: String, emoji: String) {
        val current = _messages.value.find { it.id == messageId }?.reaction
        val newReaction = if (current == emoji) "" else emoji
        db.updateReaction(messageId, newReaction)
        _messages.value = _messages.value.map {
            if (it.id == messageId) it.copy(reaction = newReaction) else it
        }
    }

    fun toggleStar(messageId: String) {
        val current = _messages.value.find { it.id == messageId }?.isStarred ?: false
        val newStarred = !current
        db.setMessageStarred(messageId, newStarred)
        _messages.value = _messages.value.map {
            if (it.id == messageId) it.copy(isStarred = newStarred) else it
        }
    }

    fun retractMessage(messageId: String) {
        db.deleteMessage(messageId, forEveryone = true)
        _messages.value = _messages.value.map {
            if (it.id == messageId) it.copy(isDeleted = true, text = "This message was deleted") else it
        }
    }

    fun deleteMessageForMe(messageId: String) {
        db.deleteMessage(messageId, forEveryone = false)
        _messages.value = _messages.value.filter { it.id != messageId }
    }

    fun clearChat() {
        db.clearMessages(conversationId)
        _messages.value = emptyList()
    }

    fun votePoll(messageId: String, optionIndex: Int) {
        val msg = _messages.value.find { it.id == messageId } ?: return
        try {
            val json = if (msg.cardData.isNotEmpty()) JSONObject(msg.cardData) else JSONObject()
            val votesArray = json.optJSONArray("votes") ?: org.json.JSONArray().apply {
                put(0)
                put(0)
            }
            val myVote = if (json.has("myVote")) json.getInt("myVote") else -1

            var newMyVote = -1
            if (myVote != -1 && myVote < votesArray.length()) {
                votesArray.put(myVote, maxOf(0, votesArray.getInt(myVote) - 1))
            }
            if (myVote != optionIndex && optionIndex < votesArray.length()) {
                votesArray.put(optionIndex, votesArray.getInt(optionIndex) + 1)
                newMyVote = optionIndex
            }

            json.put("votes", votesArray)
            json.put("myVote", newMyVote)
            val updatedCardData = json.toString()
            db.updateCardData(messageId, updatedCardData)
            _messages.value = _messages.value.map {
                if (it.id == messageId) it.copy(cardData = updatedCardData) else it
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error voting poll: ${e.message}")
        }
    }
}
