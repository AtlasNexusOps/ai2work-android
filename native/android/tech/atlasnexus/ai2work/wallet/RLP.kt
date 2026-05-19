package tech.atlasnexus.ai2work.wallet

import org.bouncycastle.util.encoders.Hex
import java.math.BigInteger

/** Minimal RLP encoder for Ethereum transactions. */
object RLP {
    fun bytes(data: ByteArray): ByteArray {
        if (data.isEmpty()) return byteArrayOf(0x80)
        if (data.size == 1 && (data[0].toInt() and 0xFF) < 0x80) return data
        return lenPrefix(data.size, 0x80) + data
    }

    fun long(v: Long): ByteArray = bigint(BigInteger.valueOf(v))
    fun bigint(v: BigInteger): ByteArray = bytes(v.toByteArray().dropWhile { it == 0.toByte() }.toByteArray())

    fun address(hex: String): ByteArray {
        val clean = hex.removePrefix("0x")
        return bytes(Hex.decode(if (clean.length % 2 == 1) "0$clean" else clean))
    }

    fun list(vararg items: ByteArray): ByteArray {
        val payload = items.fold(byteArrayOf()) { a, b -> a + b }
        return lenPrefix(payload.size, 0xC0) + payload
    }

    private fun lenPrefix(len: Int, offset: Int): ByteArray = when {
        len < 56 -> byteArrayOf((offset + len).toByte())
        else -> {
            val bin = BigInteger.valueOf(len.toLong()).toByteArray()
            byteArrayOf((offset + 0x37 + bin.size).toByte()) + bin
        }
    }
}
