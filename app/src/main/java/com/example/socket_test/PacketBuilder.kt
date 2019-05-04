package com.example.socket_test

import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import kotlin.experimental.and

class PacketBuilder(val type: Byte, val payload: ByteArray, val sequenceNumber: Short = 0, val identifier: Short = 0xDBB) {
    private val MAX_PAYLOAD = 65507
    private val CODE: Byte = 0

    init {
        if (payload.size > MAX_PAYLOAD) throw Exception("Payload limited to $MAX_PAYLOAD")
    }

    fun build(): ByteArray {
        val buffer = ByteArray(8 + payload.size)
        val byteBuffer = MappedByteBuffer.wrap(buffer)

        byteBuffer.put(type)
        byteBuffer.put(CODE)
        val checkPos = byteBuffer.position()
        byteBuffer.position(checkPos + 2)
        byteBuffer.putShort(identifier)
        byteBuffer.putShort(sequenceNumber)
        byteBuffer.put(payload)
        byteBuffer.putShort(checkPos, checksum(buffer))
        byteBuffer.flip()
        return buffer
    }

    fun withSequenceNumber(sequenceNumber: Short): PacketBuilder {
        return PacketBuilder(type, payload, sequenceNumber, identifier)
    }

    /**
     * RFC 1071 checksum
     */
    private fun checksum(data: ByteArray): Short {
        var sum = 0
        // High bytes (even indices)
        for (i in 0 until data.size step 2) {
            sum += data[i].and(0xFF.toByte()).toInt() shl 8
            sum = (sum and 0xFFFF) + (sum shr 16)
        }

        // Low bytes (odd indices)
        for (i in 1 until data.size step 2) {
            sum += data[i] and 0xFF.toByte()
            sum = (sum and 0xFFFF) + (sum shr 16)
        }

        sum = (sum and 0xFFFF) + (sum shr 16)
        return (sum xor 0xFFFF).toShort()
    }

    companion object {
        val TYPE_ICMP_V4: Byte = 8
        val TYPE_ICMP_V6 = 128.toByte()
    }
}
