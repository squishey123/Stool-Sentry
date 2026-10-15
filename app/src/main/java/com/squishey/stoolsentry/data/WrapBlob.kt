package com.squishey.stoolsentry.data

import com.squishey.stoolsentry.domain.ExportCrypto

/** Plaintext layout of the Keystore-wrapped convenience copy. The file on disk is encrypted again. */
object WrapBlob {
    private val MAGIC = byteArrayOf('S'.code.toByte(), 'S'.code.toByte(), 'W'.code.toByte(), 'K'.code.toByte())
    private const val VERSION: Byte = 1

    class Decoded(
        val passphrase: ByteArray,
        val key: ByteArray,
        val salt: ByteArray,
        val iterations: Int,
    )

    fun encode(passphrase: ByteArray, key: ByteArray, salt: ByteArray, iterations: Int): ByteArray {
        require(passphrase.isNotEmpty())
        require(passphrase.size <= 0xFFFF)
        require(key.size == ExportCrypto.KEY_BITS / 8)
        require(salt.size == ExportCrypto.SALT_LEN)
        val out = ByteArray(4 + 1 + 2 + passphrase.size + key.size + salt.size + 4)
        MAGIC.copyInto(out)
        out[4] = VERSION
        out[5] = (passphrase.size ushr 8).toByte()
        out[6] = passphrase.size.toByte()
        var cursor = 7
        passphrase.copyInto(out, cursor)
        cursor += passphrase.size
        key.copyInto(out, cursor)
        cursor += key.size
        salt.copyInto(out, cursor)
        cursor += salt.size
        out[cursor] = (iterations ushr 24).toByte()
        out[cursor + 1] = (iterations ushr 16).toByte()
        out[cursor + 2] = (iterations ushr 8).toByte()
        out[cursor + 3] = iterations.toByte()
        return out
    }

    fun decode(bytes: ByteArray): Decoded? {
        if (bytes.size < 7 + 32 + ExportCrypto.SALT_LEN + 4) return null
        if (!bytes.copyOfRange(0, 4).contentEquals(MAGIC) || bytes[4] != VERSION) return null
        val passphraseLen = ((bytes[5].toInt() and 0xFF) shl 8) or (bytes[6].toInt() and 0xFF)
        val keyLen = ExportCrypto.KEY_BITS / 8
        val expected = 7 + passphraseLen + keyLen + ExportCrypto.SALT_LEN + 4
        if (passphraseLen == 0 || bytes.size != expected) return null
        var cursor = 7
        val passphrase = bytes.copyOfRange(cursor, cursor + passphraseLen)
        cursor += passphraseLen
        val key = bytes.copyOfRange(cursor, cursor + keyLen)
        cursor += keyLen
        val salt = bytes.copyOfRange(cursor, cursor + ExportCrypto.SALT_LEN)
        cursor += ExportCrypto.SALT_LEN
        val iterations = ((bytes[cursor].toInt() and 0xFF) shl 24) or
            ((bytes[cursor + 1].toInt() and 0xFF) shl 16) or
            ((bytes[cursor + 2].toInt() and 0xFF) shl 8) or
            (bytes[cursor + 3].toInt() and 0xFF)
        if (iterations !in 1..ExportCrypto.MAX_ITERATIONS) return null
        return Decoded(passphrase, key, salt, iterations)
    }
}
