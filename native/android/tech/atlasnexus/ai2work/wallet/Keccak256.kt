package tech.atlasnexus.ai2work.wallet

/** Pure Kotlin Keccak-256 (SHA-3 variant, 256-bit output). */
object Keccak256 {
    private val RC = longArrayOf(
        0x0000000000000001L, 0x0000000000008082L, 0x800000000000808AL,
        0x8000000080008000L, 0x000000000000808BL, 0x0000000080000001L,
        0x8000000080008081L, 0x8000000000008009L, 0x000000000000008AL,
        0x0000000000000088L, 0x0000000080008009L, 0x000000008000000AL,
        0x000000008000808BL, 0x800000000000008BL, 0x8000000000008089L,
        0x8000000000008003L, 0x8000000000008002L, 0x8000000000000080L,
        0x000000000000800AL, 0x800000008000000AL, 0x8000000080008081L,
        0x8000000000008080L, 0x0000000080000001L, 0x8000000080008008L
    )
    private val ROT = intArrayOf(
        1, 3, 6, 10, 15, 21, 28, 36, 45, 55, 2, 14,
        27, 41, 56, 8, 25, 43, 62, 18, 39, 61, 20, 44
    )

    fun digest(input: ByteArray): ByteArray {
        val state = LongArray(25)
        val buf = ByteArray(136)
        var pos = 0

        // Absorb
        for (byte in input) {
            buf[pos++] = byte
            if (pos == 136) {
                absorbBlock(state, buf)
                pos = 0
            }
        }
        // Padding
        buf[pos] = 0x01.toByte()
        for (i in pos + 1 until 135) buf[i] = 0
        buf[135] = (buf[135].toInt() xor 0x80).toByte()
        absorbBlock(state, buf)

        // Squeeze
        val out = ByteArray(32)
        var outPos = 0
        while (outPos < 32) {
            keccakF(state)
            val bytes = longArrayToBytes(state).take(136 - outPos.coerceAtMost(136))
            for (b in bytes) {
                if (outPos >= 32) break
                out[outPos++] = b
            }
        }
        return out
    }

    private fun absorbBlock(state: LongArray, block: ByteArray) {
        for (i in 0 until 17) {
            var v = 0L
            for (j in 0 until 8) {
                val idx = i * 8 + j
                if (idx < block.size) v = v or ((block[idx].toLong() and 0xFF) shl (8 * j))
            }
            state[i] = state[i] xor v
        }
        keccakF(state)
    }

    private fun keccakF(state: LongArray) {
        val a = state
        for (round in 0 until 24) {
            // Theta
            val c = LongArray(5) { a[it] xor a[it + 5] xor a[it + 10] xor a[it + 15] xor a[it + 20] }
            val d = LongArray(5) { c[(it + 4) % 5] xor java.lang.Long.rotateLeft(c[(it + 1) % 5], 1) }
            for (x in 0 until 5) for (y in 0 until 5) a[x + 5 * y] = a[x + 5 * y] xor d[x]

            // Rho + Pi
            var cur = a[1]
            var cx = 0; var cy = 1
            for (t in 0 until 24) {
                val nx = (cx + 3 * cy) % 5
                val ny = cx
                val tmp = a[nx + 5 * ny]
                a[nx + 5 * ny] = java.lang.Long.rotateLeft(cur, ROT[t])
                cur = tmp
                cx = nx; cy = ny
            }

            // Chi
            for (y in 0 until 5) {
                val t = LongArray(5) { a[it + 5 * y] }
                for (x in 0 until 5) a[x + 5 * y] = t[x] xor (t[(x + 1) % 5].inv() and t[(x + 2) % 5])
            }

            // Iota
            a[0] = a[0] xor RC[round]
        }
    }

    private fun longArrayToBytes(state: LongArray): List<Byte> {
        val out = mutableListOf<Byte>()
        for (v in state) {
            for (i in 0 until 8) out.add(((v ushr (8 * i)) and 0xFF).toByte())
        }
        return out
    }
}
