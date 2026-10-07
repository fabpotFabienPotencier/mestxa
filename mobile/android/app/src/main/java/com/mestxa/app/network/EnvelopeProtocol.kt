package com.mestxa.app.network

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Native Protobuf wire implementation for mestxa.wire.WireFrame
 * Perfectly matches backend envelope.proto and prost::Message decoding.
 */

enum class MessageType(val value: Int) {
    UNSPECIFIED(0),
    TEXT(1),
    MEDIA_ATTACHMENT(2),
    VOICE_NOTE(3),
    VIEW_ONCE(4),
    RETRACT(5),
    EDIT(6),
    READ_RECEIPT(7);

    companion object {
        fun fromInt(v: Int) = entries.firstOrNull { it.value == v } ?: UNSPECIFIED
    }
}

enum class AckStatus(val value: Int) {
    UNSPECIFIED(0),
    DELIVERED(1),
    READ(2);

    companion object {
        fun fromInt(v: Int) = entries.firstOrNull { it.value == v } ?: UNSPECIFIED
    }
}

enum class CallSignalType(val value: Int) {
    UNSPECIFIED(0),
    OFFER(1),
    ANSWER(2),
    ICE_CANDIDATE(3),
    REJECT(4),
    HANGUP(5),
    RINGING(6);

    companion object {
        fun fromInt(v: Int) = entries.firstOrNull { it.value == v } ?: UNSPECIFIED
    }
}

data class MessageEnvelope(
    val messageId: String,
    val senderIdentityKey: ByteArray,
    val recipientIdentityKey: ByteArray,
    val ephemeralDhKey: ByteArray,
    val kyberCiphertext: ByteArray,
    val ciphertext: ByteArray,
    val iv: ByteArray,
    val messageType: MessageType = MessageType.TEXT,
    val timestampMs: Long = System.currentTimeMillis(),
    val sequenceNumber: Int = 0
) {
    fun encode(): ByteArray {
        val bos = ByteArrayOutputStream()
        ProtoWriter.writeString(bos, 1, messageId)
        ProtoWriter.writeBytes(bos, 2, senderIdentityKey)
        ProtoWriter.writeBytes(bos, 3, recipientIdentityKey)
        ProtoWriter.writeBytes(bos, 4, ephemeralDhKey)
        ProtoWriter.writeBytes(bos, 5, kyberCiphertext)
        ProtoWriter.writeBytes(bos, 6, ciphertext)
        ProtoWriter.writeBytes(bos, 7, iv)
        ProtoWriter.writeVarintField(bos, 8, messageType.value.toLong())
        ProtoWriter.writeVarintField(bos, 9, timestampMs)
        ProtoWriter.writeVarintField(bos, 10, sequenceNumber.toLong())
        return bos.toByteArray()
    }

    companion object {
        fun decode(bytes: ByteArray): MessageEnvelope {
            val reader = ProtoReader(bytes)
            var msgId = ""
            var sender = ByteArray(0)
            var recipient = ByteArray(0)
            var ephemeral = ByteArray(0)
            var kyber = ByteArray(0)
            var ct = ByteArray(0)
            var nonce = ByteArray(0)
            var type = MessageType.TEXT
            var time = 0L
            var seq = 0

            while (reader.hasNext()) {
                val tag = reader.readTag()
                val fieldNum = tag ushr 3
                when (fieldNum) {
                    1 -> msgId = reader.readString()
                    2 -> sender = reader.readBytes()
                    3 -> recipient = reader.readBytes()
                    4 -> ephemeral = reader.readBytes()
                    5 -> kyber = reader.readBytes()
                    6 -> ct = reader.readBytes()
                    7 -> nonce = reader.readBytes()
                    8 -> type = MessageType.fromInt(reader.readVarint().toInt())
                    9 -> time = reader.readVarint()
                    10 -> seq = reader.readVarint().toInt()
                    else -> reader.skipField(tag and 0x7)
                }
            }
            return MessageEnvelope(msgId, sender, recipient, ephemeral, kyber, ct, nonce, type, time, seq)
        }
    }
}

data class DeliveryAck(
    val messageId: String,
    val recipientId: ByteArray,
    val status: AckStatus = AckStatus.DELIVERED,
    val timestampMs: Long = System.currentTimeMillis()
) {
    fun encode(): ByteArray {
        val bos = ByteArrayOutputStream()
        ProtoWriter.writeString(bos, 1, messageId)
        ProtoWriter.writeBytes(bos, 2, recipientId)
        ProtoWriter.writeVarintField(bos, 3, status.value.toLong())
        ProtoWriter.writeVarintField(bos, 4, timestampMs)
        return bos.toByteArray()
    }

    companion object {
        fun decode(bytes: ByteArray): DeliveryAck {
            val reader = ProtoReader(bytes)
            var msgId = ""
            var recipient = ByteArray(0)
            var status = AckStatus.DELIVERED
            var time = 0L
            while (reader.hasNext()) {
                val tag = reader.readTag()
                when (tag ushr 3) {
                    1 -> msgId = reader.readString()
                    2 -> recipient = reader.readBytes()
                    3 -> status = AckStatus.fromInt(reader.readVarint().toInt())
                    4 -> time = reader.readVarint()
                    else -> reader.skipField(tag and 0x7)
                }
            }
            return DeliveryAck(msgId, recipient, status, time)
        }
    }
}

data class CallSignal(
    val callId: String,
    val callerId: ByteArray,
    val calleeId: ByteArray,
    val signalType: CallSignalType,
    val sdpOrCandidate: String,
    val sframeEpochKeyCiphertext: ByteArray = ByteArray(0)
) {
    fun encode(): ByteArray {
        val bos = ByteArrayOutputStream()
        ProtoWriter.writeString(bos, 1, callId)
        ProtoWriter.writeBytes(bos, 2, callerId)
        ProtoWriter.writeBytes(bos, 3, calleeId)
        ProtoWriter.writeVarintField(bos, 4, signalType.value.toLong())
        ProtoWriter.writeString(bos, 5, sdpOrCandidate)
        ProtoWriter.writeBytes(bos, 6, sframeEpochKeyCiphertext)
        return bos.toByteArray()
    }

    companion object {
        fun decode(bytes: ByteArray): CallSignal {
            val reader = ProtoReader(bytes)
            var callId = ""
            var caller = ByteArray(0)
            var callee = ByteArray(0)
            var sigType = CallSignalType.OFFER
            var sdp = ""
            var sframe = ByteArray(0)
            while (reader.hasNext()) {
                val tag = reader.readTag()
                when (tag ushr 3) {
                    1 -> callId = reader.readString()
                    2 -> caller = reader.readBytes()
                    3 -> callee = reader.readBytes()
                    4 -> sigType = CallSignalType.fromInt(reader.readVarint().toInt())
                    5 -> sdp = reader.readString()
                    6 -> sframe = reader.readBytes()
                    else -> reader.skipField(tag and 0x7)
                }
            }
            return CallSignal(callId, caller, callee, sigType, sdp, sframe)
        }
    }
}

sealed class WireFramePayload {
    data class EnvelopePayload(val envelope: MessageEnvelope) : WireFramePayload()
    data class AckPayload(val ack: DeliveryAck) : WireFramePayload()
    data class CallSignalPayload(val callSignal: CallSignal) : WireFramePayload()
    data class PingPayload(val timestampMs: Long) : WireFramePayload()
    data class PongPayload(val timestampMs: Long) : WireFramePayload()
}

object WireFrameSerializer {
    fun encode(payload: WireFramePayload): ByteArray {
        val bos = ByteArrayOutputStream()
        when (payload) {
            is WireFramePayload.EnvelopePayload -> {
                ProtoWriter.writeBytes(bos, 1, payload.envelope.encode())
            }
            is WireFramePayload.AckPayload -> {
                ProtoWriter.writeBytes(bos, 2, payload.ack.encode())
            }
            is WireFramePayload.CallSignalPayload -> {
                ProtoWriter.writeBytes(bos, 6, payload.callSignal.encode())
            }
            is WireFramePayload.PingPayload -> {
                val p = ByteArrayOutputStream()
                ProtoWriter.writeVarintField(p, 1, payload.timestampMs)
                ProtoWriter.writeBytes(bos, 7, p.toByteArray())
            }
            is WireFramePayload.PongPayload -> {
                val p = ByteArrayOutputStream()
                ProtoWriter.writeVarintField(p, 1, payload.timestampMs)
                ProtoWriter.writeBytes(bos, 8, p.toByteArray())
            }
        }
        return bos.toByteArray()
    }

    fun decode(bytes: ByteArray): WireFramePayload? {
        val reader = ProtoReader(bytes)
        while (reader.hasNext()) {
            val tag = reader.readTag()
            when (tag ushr 3) {
                1 -> return WireFramePayload.EnvelopePayload(MessageEnvelope.decode(reader.readBytes()))
                2 -> return WireFramePayload.AckPayload(DeliveryAck.decode(reader.readBytes()))
                6 -> return WireFramePayload.CallSignalPayload(CallSignal.decode(reader.readBytes()))
                7 -> {
                    val p = ProtoReader(reader.readBytes())
                    var ts = 0L
                    if (p.hasNext() && (p.readTag() ushr 3) == 1) ts = p.readVarint()
                    return WireFramePayload.PingPayload(ts)
                }
                8 -> {
                    val p = ProtoReader(reader.readBytes())
                    var ts = 0L
                    if (p.hasNext() && (p.readTag() ushr 3) == 1) ts = p.readVarint()
                    return WireFramePayload.PongPayload(ts)
                }
                else -> reader.skipField(tag and 0x7)
            }
        }
        return null
    }
}

// Low-level high-performance Protobuf wire primitives
internal object ProtoWriter {
    fun writeVarintField(bos: ByteArrayOutputStream, fieldNumber: Int, value: Long) {
        writeTag(bos, fieldNumber, 0)
        writeVarint(bos, value)
    }

    fun writeString(bos: ByteArrayOutputStream, fieldNumber: Int, value: String) {
        if (value.isNotEmpty()) {
            writeBytes(bos, fieldNumber, value.toByteArray(Charsets.UTF_8))
        }
    }

    fun writeBytes(bos: ByteArrayOutputStream, fieldNumber: Int, value: ByteArray) {
        if (value.isNotEmpty()) {
            writeTag(bos, fieldNumber, 2)
            writeVarint(bos, value.size.toLong())
            bos.write(value)
        }
    }

    private fun writeTag(bos: ByteArrayOutputStream, fieldNumber: Int, wireType: Int) {
        writeVarint(bos, ((fieldNumber shl 3) or wireType).toLong())
    }

    private fun writeVarint(bos: ByteArrayOutputStream, v: Long) {
        var value = v
        while (true) {
            if ((value and 0x7FL.inv()) == 0L) {
                bos.write(value.toInt())
                return
            } else {
                bos.write(((value.toInt() and 0x7F) or 0x80))
                value = value ushr 7
            }
        }
    }
}

internal class ProtoReader(private val buffer: ByteArray) {
    private var pos = 0

    fun hasNext(): Boolean = pos < buffer.size

    fun readTag(): Int = readVarint().toInt()

    fun readVarint(): Long {
        var result = 0L
        var shift = 0
        while (shift < 64 && pos < buffer.size) {
            val b = buffer[pos++].toLong()
            result = result or ((b and 0x7FL) shl shift)
            if ((b and 0x80L) == 0L) return result
            shift += 7
        }
        return result
    }

    fun readBytes(): ByteArray {
        val len = readVarint().toInt()
        val data = ByteArray(len)
        System.arraycopy(buffer, pos, data, 0, len)
        pos += len
        return data
    }

    fun readString(): String = String(readBytes(), Charsets.UTF_8)

    fun skipField(wireType: Int) {
        when (wireType) {
            0 -> readVarint()
            1 -> pos += 8
            2 -> {
                val len = readVarint().toInt()
                pos += len
            }
            5 -> pos += 4
        }
    }
}
