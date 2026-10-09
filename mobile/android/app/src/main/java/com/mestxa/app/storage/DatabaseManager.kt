package com.mestxa.app.storage

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

data class ConversationRecord(
    val id: String,
    val contactName: String,
    val contactHandle: String = "",
    val contactNumber: String = "",
    val contactPublicKeyHex: String = "",
    val lastMessage: String = "",
    val timestamp: String = "",
    val unreadCount: Int = 0,
    val isDelivered: Boolean = true,
    val isRead: Boolean = true,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isMuted: Boolean = false,
    val isFavorite: Boolean = false,
    val colorGradientIdx: Int = 0,
    val updatedAtMs: Long = System.currentTimeMillis()
)

data class MessageRecord(
    val id: String,
    val conversationId: String,
    val senderKeyHex: String,
    val isFromMe: Boolean,
    val content: String,
    val timestamp: String,
    val createdAtMs: Long = System.currentTimeMillis(),
    val isDelivered: Boolean = true,
    val isRead: Boolean = true,
    val cardType: String = "text",
    val cardData: String = "",
    val quoteReply: String = "",
    val reaction: String = "",
    val isStarred: Boolean = false,
    val isDeleted: Boolean = false
)

data class ContactRecord(
    val userHex: String,
    val username: String,
    val number: String,
    val name: String,
    val about: String = "Available",
    val addedAtMs: Long = System.currentTimeMillis()
)

data class CallRecordEntity(
    val id: String,
    val contactName: String,
    val isOutgoing: Boolean,
    val isMissed: Boolean,
    val timestamp: String,
    val isVideo: Boolean = false,
    val durationSec: Int = 0
)

class DatabaseManager private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "DatabaseManager"
        private const val DATABASE_NAME = "mestxa_local_vault.db"
        private const val DATABASE_VERSION = 1

        @Volatile
        private var instance: DatabaseManager? = null

        fun getInstance(context: Context): DatabaseManager {
            return instance ?: synchronized(this) {
                instance ?: DatabaseManager(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE conversations (
                id TEXT PRIMARY KEY,
                contact_name TEXT NOT NULL,
                contact_handle TEXT,
                contact_number TEXT,
                contact_public_key_hex TEXT,
                last_message TEXT,
                timestamp TEXT,
                unread_count INTEGER DEFAULT 0,
                is_delivered INTEGER DEFAULT 1,
                is_read INTEGER DEFAULT 1,
                is_pinned INTEGER DEFAULT 0,
                is_archived INTEGER DEFAULT 0,
                is_muted INTEGER DEFAULT 0,
                is_favorite INTEGER DEFAULT 0,
                color_gradient_idx INTEGER DEFAULT 0,
                updated_at_ms INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE messages (
                id TEXT PRIMARY KEY,
                conversation_id TEXT NOT NULL,
                sender_key_hex TEXT,
                is_from_me INTEGER NOT NULL,
                content TEXT NOT NULL,
                timestamp TEXT NOT NULL,
                created_at_ms INTEGER,
                is_delivered INTEGER DEFAULT 1,
                is_read INTEGER DEFAULT 1,
                card_type TEXT DEFAULT 'text',
                card_data TEXT,
                quote_reply TEXT,
                reaction TEXT,
                is_starred INTEGER DEFAULT 0,
                is_deleted INTEGER DEFAULT 0
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE contacts (
                user_hex TEXT PRIMARY KEY,
                username TEXT NOT NULL,
                number TEXT NOT NULL,
                name TEXT NOT NULL,
                about TEXT,
                added_at_ms INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE calls (
                id TEXT PRIMARY KEY,
                contact_name TEXT NOT NULL,
                is_outgoing INTEGER NOT NULL,
                is_missed INTEGER NOT NULL,
                timestamp TEXT NOT NULL,
                is_video INTEGER DEFAULT 0,
                duration_sec INTEGER DEFAULT 0
            )
            """.trimIndent()
        )

        Log.i(TAG, "Mestxa local vault tables initialized.")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS messages")
        db.execSQL("DROP TABLE IF EXISTS conversations")
        db.execSQL("DROP TABLE IF EXISTS contacts")
        db.execSQL("DROP TABLE IF EXISTS calls")
        onCreate(db)
    }

    // -------------------------------------------------------------------------
    // Conversations
    // -------------------------------------------------------------------------

    fun getConversations(includeArchived: Boolean = false): List<ConversationRecord> {
        val list = mutableListOf<ConversationRecord>()
        val db = readableDatabase
        val selection = if (includeArchived) null else "is_archived = 0"
        val cursor = db.query(
            "conversations",
            null,
            selection,
            null,
            null,
            null,
            "is_pinned DESC, updated_at_ms DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    ConversationRecord(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        contactName = it.getString(it.getColumnIndexOrThrow("contact_name")),
                        contactHandle = it.getString(it.getColumnIndexOrThrow("contact_handle")) ?: "",
                        contactNumber = it.getString(it.getColumnIndexOrThrow("contact_number")) ?: "",
                        contactPublicKeyHex = it.getString(it.getColumnIndexOrThrow("contact_public_key_hex")) ?: "",
                        lastMessage = it.getString(it.getColumnIndexOrThrow("last_message")) ?: "",
                        timestamp = it.getString(it.getColumnIndexOrThrow("timestamp")) ?: "",
                        unreadCount = it.getInt(it.getColumnIndexOrThrow("unread_count")),
                        isDelivered = it.getInt(it.getColumnIndexOrThrow("is_delivered")) == 1,
                        isRead = it.getInt(it.getColumnIndexOrThrow("is_read")) == 1,
                        isPinned = it.getInt(it.getColumnIndexOrThrow("is_pinned")) == 1,
                        isArchived = it.getInt(it.getColumnIndexOrThrow("is_archived")) == 1,
                        isMuted = it.getInt(it.getColumnIndexOrThrow("is_muted")) == 1,
                        isFavorite = it.getInt(it.getColumnIndexOrThrow("is_favorite")) == 1,
                        colorGradientIdx = it.getInt(it.getColumnIndexOrThrow("color_gradient_idx")),
                        updatedAtMs = it.getLong(it.getColumnIndexOrThrow("updated_at_ms"))
                    )
                )
            }
        }
        return list
    }

    fun getArchivedConversations(): List<ConversationRecord> {
        val list = mutableListOf<ConversationRecord>()
        val db = readableDatabase
        val cursor = db.query(
            "conversations",
            null,
            "is_archived = 1",
            null,
            null,
            null,
            "updated_at_ms DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    ConversationRecord(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        contactName = it.getString(it.getColumnIndexOrThrow("contact_name")),
                        contactHandle = it.getString(it.getColumnIndexOrThrow("contact_handle")) ?: "",
                        contactNumber = it.getString(it.getColumnIndexOrThrow("contact_number")) ?: "",
                        contactPublicKeyHex = it.getString(it.getColumnIndexOrThrow("contact_public_key_hex")) ?: "",
                        lastMessage = it.getString(it.getColumnIndexOrThrow("last_message")) ?: "",
                        timestamp = it.getString(it.getColumnIndexOrThrow("timestamp")) ?: "",
                        unreadCount = it.getInt(it.getColumnIndexOrThrow("unread_count")),
                        isDelivered = it.getInt(it.getColumnIndexOrThrow("is_delivered")) == 1,
                        isRead = it.getInt(it.getColumnIndexOrThrow("is_read")) == 1,
                        isPinned = it.getInt(it.getColumnIndexOrThrow("is_pinned")) == 1,
                        isArchived = it.getInt(it.getColumnIndexOrThrow("is_archived")) == 1,
                        isMuted = it.getInt(it.getColumnIndexOrThrow("is_muted")) == 1,
                        isFavorite = it.getInt(it.getColumnIndexOrThrow("is_favorite")) == 1,
                        colorGradientIdx = it.getInt(it.getColumnIndexOrThrow("color_gradient_idx")),
                        updatedAtMs = it.getLong(it.getColumnIndexOrThrow("updated_at_ms"))
                    )
                )
            }
        }
        return list
    }

    fun saveConversation(conv: ConversationRecord) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", conv.id)
            put("contact_name", conv.contactName)
            put("contact_handle", conv.contactHandle)
            put("contact_number", conv.contactNumber)
            put("contact_public_key_hex", conv.contactPublicKeyHex)
            put("last_message", conv.lastMessage)
            put("timestamp", conv.timestamp)
            put("unread_count", conv.unreadCount)
            put("is_delivered", if (conv.isDelivered) 1 else 0)
            put("is_read", if (conv.isRead) 1 else 0)
            put("is_pinned", if (conv.isPinned) 1 else 0)
            put("is_archived", if (conv.isArchived) 1 else 0)
            put("is_muted", if (conv.isMuted) 1 else 0)
            put("is_favorite", if (conv.isFavorite) 1 else 0)
            put("color_gradient_idx", conv.colorGradientIdx)
            put("updated_at_ms", conv.updatedAtMs)
        }
        db.insertWithOnConflict("conversations", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun setConversationPinned(id: String, isPinned: Boolean) {
        val db = writableDatabase
        val cv = ContentValues().apply { put("is_pinned", if (isPinned) 1 else 0) }
        db.update("conversations", cv, "id = ?", arrayOf(id))
    }

    fun setConversationArchived(id: String, isArchived: Boolean) {
        val db = writableDatabase
        val cv = ContentValues().apply { put("is_archived", if (isArchived) 1 else 0) }
        db.update("conversations", cv, "id = ?", arrayOf(id))
    }

    fun deleteConversation(id: String) {
        val db = writableDatabase
        db.delete("conversations", "id = ?", arrayOf(id))
        db.delete("messages", "conversation_id = ?", arrayOf(id))
    }

    fun setConversationMuted(id: String, isMuted: Boolean) {
        val db = writableDatabase
        val cv = ContentValues().apply { put("is_muted", if (isMuted) 1 else 0) }
        db.update("conversations", cv, "id = ?", arrayOf(id))
    }

    fun setConversationRead(id: String, isRead: Boolean) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("is_read", if (isRead) 1 else 0)
            if (isRead) put("unread_count", 0)
        }
        db.update("conversations", cv, "id = ?", arrayOf(id))
    }

    fun markAllConversationsRead() {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("is_read", 1)
            put("unread_count", 0)
        }
        db.update("conversations", cv, null, null)
    }

    // -------------------------------------------------------------------------
    // Messages
    // -------------------------------------------------------------------------

    fun getMessages(conversationId: String): List<MessageRecord> {
        val list = mutableListOf<MessageRecord>()
        val db = readableDatabase
        val cursor = db.query(
            "messages",
            null,
            "conversation_id = ?",
            arrayOf(conversationId),
            null,
            null,
            "created_at_ms ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    MessageRecord(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        conversationId = it.getString(it.getColumnIndexOrThrow("conversation_id")),
                        senderKeyHex = it.getString(it.getColumnIndexOrThrow("sender_key_hex")) ?: "",
                        isFromMe = it.getInt(it.getColumnIndexOrThrow("is_from_me")) == 1,
                        content = it.getString(it.getColumnIndexOrThrow("content")),
                        timestamp = it.getString(it.getColumnIndexOrThrow("timestamp")),
                        createdAtMs = it.getLong(it.getColumnIndexOrThrow("created_at_ms")),
                        isDelivered = it.getInt(it.getColumnIndexOrThrow("is_delivered")) == 1,
                        isRead = it.getInt(it.getColumnIndexOrThrow("is_read")) == 1,
                        cardType = it.getString(it.getColumnIndexOrThrow("card_type")) ?: "text",
                        cardData = it.getString(it.getColumnIndexOrThrow("card_data")) ?: "",
                        quoteReply = it.getString(it.getColumnIndexOrThrow("quote_reply")) ?: "",
                        reaction = it.getString(it.getColumnIndexOrThrow("reaction")) ?: "",
                        isStarred = it.getInt(it.getColumnIndexOrThrow("is_starred")) == 1,
                        isDeleted = it.getInt(it.getColumnIndexOrThrow("is_deleted")) == 1
                    )
                )
            }
        }
        return list
    }

    fun saveMessage(msg: MessageRecord) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", msg.id)
            put("conversation_id", msg.conversationId)
            put("sender_key_hex", msg.senderKeyHex)
            put("is_from_me", if (msg.isFromMe) 1 else 0)
            put("content", msg.content)
            put("timestamp", msg.timestamp)
            put("created_at_ms", msg.createdAtMs)
            put("is_delivered", if (msg.isDelivered) 1 else 0)
            put("is_read", if (msg.isRead) 1 else 0)
            put("card_type", msg.cardType)
            put("card_data", msg.cardData)
            put("quote_reply", msg.quoteReply)
            put("reaction", msg.reaction)
            put("is_starred", if (msg.isStarred) 1 else 0)
            put("is_deleted", if (msg.isDeleted) 1 else 0)
        }
        db.insertWithOnConflict("messages", null, values, SQLiteDatabase.CONFLICT_REPLACE)

        // Update conversation's last message snippet
        val updateConv = ContentValues().apply {
            put("last_message", if (msg.isDeleted) "This message was deleted" else msg.content.ifBlank { msg.cardType })
            put("timestamp", msg.timestamp)
            put("updated_at_ms", msg.createdAtMs)
            if (!msg.isFromMe) {
                // If unread
                // unread_count logic can be maintained
            }
        }
        db.update("conversations", updateConv, "id = ?", arrayOf(msg.conversationId))
    }

    fun updateReaction(messageId: String, reaction: String) {
        val db = writableDatabase
        val cv = ContentValues().apply { put("reaction", reaction) }
        db.update("messages", cv, "id = ?", arrayOf(messageId))
    }

    fun updateCardData(messageId: String, cardData: String) {
        val db = writableDatabase
        val cv = ContentValues().apply { put("card_data", cardData) }
        db.update("messages", cv, "id = ?", arrayOf(messageId))
    }

    fun deleteMessage(messageId: String, forEveryone: Boolean) {
        val db = writableDatabase
        if (forEveryone) {
            val cv = ContentValues().apply {
                put("is_deleted", 1)
                put("content", "This message was deleted")
            }
            db.update("messages", cv, "id = ?", arrayOf(messageId))
        } else {
            db.delete("messages", "id = ?", arrayOf(messageId))
        }
    }

    fun clearMessages(conversationId: String) {
        val db = writableDatabase
        db.delete("messages", "conversation_id = ?", arrayOf(conversationId))
        val cv = ContentValues().apply {
            put("last_message", "")
        }
        db.update("conversations", cv, "id = ?", arrayOf(conversationId))
    }

    // -------------------------------------------------------------------------
    // Contacts
    // -------------------------------------------------------------------------

    fun getContacts(): List<ContactRecord> {
        val list = mutableListOf<ContactRecord>()
        val db = readableDatabase
        val cursor = db.query("contacts", null, null, null, null, null, "name ASC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    ContactRecord(
                        userHex = it.getString(it.getColumnIndexOrThrow("user_hex")),
                        username = it.getString(it.getColumnIndexOrThrow("username")),
                        number = it.getString(it.getColumnIndexOrThrow("number")),
                        name = it.getString(it.getColumnIndexOrThrow("name")),
                        about = it.getString(it.getColumnIndexOrThrow("about")) ?: "Available",
                        addedAtMs = it.getLong(it.getColumnIndexOrThrow("added_at_ms"))
                    )
                )
            }
        }
        return list
    }

    fun saveContact(c: ContactRecord) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("user_hex", c.userHex)
            put("username", c.username)
            put("number", c.number)
            put("name", c.name)
            put("about", c.about)
            put("added_at_ms", c.addedAtMs)
        }
        db.insertWithOnConflict("contacts", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getContactByIdentifier(query: String): ContactRecord? {
        val db = readableDatabase
        val clean = query.trim().trimStart('@')
        val cursor = db.query(
            "contacts",
            null,
            "user_hex = ? OR username = ? OR number = ? OR name = ?",
            arrayOf(clean, clean, clean, clean),
            null, null, null, "1"
        )
        cursor.use {
            if (it.moveToFirst()) {
                return ContactRecord(
                    userHex = it.getString(it.getColumnIndexOrThrow("user_hex")),
                    username = it.getString(it.getColumnIndexOrThrow("username")),
                    number = it.getString(it.getColumnIndexOrThrow("number")),
                    name = it.getString(it.getColumnIndexOrThrow("name")),
                    about = it.getString(it.getColumnIndexOrThrow("about")) ?: "Available",
                    addedAtMs = it.getLong(it.getColumnIndexOrThrow("added_at_ms"))
                )
            }
        }
        return null
    }

    fun getConversation(id: String): ConversationRecord? {
        val db = readableDatabase
        val cursor = db.query(
            "conversations",
            null,
            "id = ? OR contact_name = ? OR contact_handle = ?",
            arrayOf(id, id, id),
            null, null, null, "1"
        )
        cursor.use {
            if (it.moveToFirst()) {
                return ConversationRecord(
                    id = it.getString(it.getColumnIndexOrThrow("id")),
                    contactName = it.getString(it.getColumnIndexOrThrow("contact_name")),
                    contactHandle = it.getString(it.getColumnIndexOrThrow("contact_handle")) ?: "",
                    contactNumber = it.getString(it.getColumnIndexOrThrow("contact_number")) ?: "",
                    contactPublicKeyHex = it.getString(it.getColumnIndexOrThrow("contact_public_key_hex")) ?: "",
                    lastMessage = it.getString(it.getColumnIndexOrThrow("last_message")) ?: "",
                    timestamp = it.getString(it.getColumnIndexOrThrow("timestamp")) ?: "",
                    unreadCount = it.getInt(it.getColumnIndexOrThrow("unread_count")),
                    isDelivered = it.getInt(it.getColumnIndexOrThrow("is_delivered")) == 1,
                    isRead = it.getInt(it.getColumnIndexOrThrow("is_read")) == 1,
                    isPinned = it.getInt(it.getColumnIndexOrThrow("is_pinned")) == 1,
                    isArchived = it.getInt(it.getColumnIndexOrThrow("is_archived")) == 1,
                    isMuted = it.getInt(it.getColumnIndexOrThrow("is_muted")) == 1,
                    isFavorite = it.getInt(it.getColumnIndexOrThrow("is_favorite")) == 1,
                    colorGradientIdx = it.getInt(it.getColumnIndexOrThrow("color_gradient_idx")),
                    updatedAtMs = it.getLong(it.getColumnIndexOrThrow("updated_at_ms"))
                )
            }
        }
        return null
    }

    fun setMessageStarred(messageId: String, isStarred: Boolean) {
        val db = writableDatabase
        val cv = ContentValues().apply { put("is_starred", if (isStarred) 1 else 0) }
        db.update("messages", cv, "id = ?", arrayOf(messageId))
    }

    // -------------------------------------------------------------------------
    // Calls
    // -------------------------------------------------------------------------

    fun getCalls(): List<CallRecordEntity> {
        val list = mutableListOf<CallRecordEntity>()
        val db = readableDatabase
        val cursor = db.query("calls", null, null, null, null, null, "timestamp DESC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    CallRecordEntity(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        contactName = it.getString(it.getColumnIndexOrThrow("contact_name")),
                        isOutgoing = it.getInt(it.getColumnIndexOrThrow("is_outgoing")) == 1,
                        isMissed = it.getInt(it.getColumnIndexOrThrow("is_missed")) == 1,
                        timestamp = it.getString(it.getColumnIndexOrThrow("timestamp")),
                        isVideo = it.getInt(it.getColumnIndexOrThrow("is_video")) == 1,
                        durationSec = it.getInt(it.getColumnIndexOrThrow("duration_sec"))
                    )
                )
            }
        }
        return list
    }

    fun saveCall(call: CallRecordEntity) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("id", call.id)
            put("contact_name", call.contactName)
            put("is_outgoing", if (call.isOutgoing) 1 else 0)
            put("is_missed", if (call.isMissed) 1 else 0)
            put("timestamp", call.timestamp)
            put("is_video", if (call.isVideo) 1 else 0)
            put("duration_sec", call.durationSec)
        }
        db.insertWithOnConflict("calls", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }
}
