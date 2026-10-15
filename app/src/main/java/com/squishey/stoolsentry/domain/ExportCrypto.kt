package com.squishey.stoolsentry.domain

import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.security.spec.KeySpec
import java.text.Normalizer
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Sealed backup container. A new phone needs the passphrase and this file; nothing
 * about the device is required.
 *
 * Layout, big-endian:
 * `SSEN` | version | kdf id | iterations | 16-byte salt | 12-byte nonce | AES-256-GCM ciphertext and tag.
 */
object ExportCrypto {
    val MAGIC: ByteArray = byteArrayOf('S'.code.toByte(), 'S'.code.toByte(), 'E'.code.toByte(), 'N'.code.toByte())
    const val VERSION: Byte = 1
    const val KDF_PBKDF2_SHA256: Byte = 1
    const val ITERATIONS = 600_000
    const val MAX_ITERATIONS = 2_000_000
    const val SALT_LEN = 16
    const val NONCE_LEN = 12
    const val KEY_BITS = 256
    const val HEADER_LEN = 38
    const val OFFSET_ITERATIONS = 6
    const val OFFSET_SALT = 10
    const val OFFSET_NONCE = 26

    private const val GCM_TAG_BITS = 128

    class ExportKey(
        val key: ByteArray,
        val salt: ByteArray,
        val iterations: Int,
    )

    fun normalize(passphrase: String): String = Normalizer.normalize(passphrase, Normalizer.Form.NFC)

    fun encrypt(plaintext: ByteArray, passphrase: String, iterations: Int = ITERATIONS): ByteArray =
        encrypt(plaintext, derive(passphrase, iterations = iterations))

    fun encrypt(plaintext: ByteArray, key: ExportKey, random: SecureRandom = SecureRandom()): ByteArray {
        val nonce = ByteArray(NONCE_LEN)
        random.nextBytes(nonce)
        val body = aesGcm(Cipher.ENCRYPT_MODE, key.key, nonce, plaintext)
        val out = ByteArray(HEADER_LEN + body.size)
        MAGIC.copyInto(out)
        out[4] = VERSION
        out[5] = KDF_PBKDF2_SHA256
        writeInt(out, OFFSET_ITERATIONS, key.iterations)
        key.salt.copyInto(out, OFFSET_SALT)
        nonce.copyInto(out, OFFSET_NONCE)
        body.copyInto(out, HEADER_LEN)
        return out
    }

    fun decrypt(container: ByteArray, passphrase: String): ByteArray {
        if (container.size < HEADER_LEN + GCM_TAG_BITS / 8) throw ExportCryptoException()
        if (!looksEncrypted(container)) throw ExportCryptoException()
        if (container[4] != VERSION || container[5] != KDF_PBKDF2_SHA256) throw ExportCryptoException()
        val iterations = readInt(container, OFFSET_ITERATIONS)
        if (iterations !in 1..MAX_ITERATIONS) throw ExportCryptoException()
        val salt = container.copyOfRange(OFFSET_SALT, OFFSET_SALT + SALT_LEN)
        val nonce = container.copyOfRange(OFFSET_NONCE, HEADER_LEN)
        val body = container.copyOfRange(HEADER_LEN, container.size)
        val key = derive(passphrase, salt, iterations)
        return try {
            aesGcm(Cipher.DECRYPT_MODE, key.key, nonce, body)
        } catch (_: GeneralSecurityException) {
            throw ExportCryptoException()
        }
    }

    fun looksEncrypted(bytes: ByteArray): Boolean {
        if (bytes.size < MAGIC.size) return false
        return bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)
    }

    fun derive(
        passphrase: String,
        salt: ByteArray = randomSalt(),
        iterations: Int = ITERATIONS,
    ): ExportKey {
        val normalized = normalize(passphrase)
        require(normalized.isNotEmpty())
        require(salt.size == SALT_LEN)
        require(iterations in 1..MAX_ITERATIONS)
        val spec: KeySpec = PBEKeySpec(normalized.toCharArray(), salt, iterations, KEY_BITS)
        try {
            val encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
            return ExportKey(encoded, salt.copyOf(), iterations)
        } finally {
            (spec as PBEKeySpec).clearPassword()
        }
    }

    private fun randomSalt(): ByteArray = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }

    private fun aesGcm(mode: Int, key: ByteArray, nonce: ByteArray, input: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, nonce))
        return cipher.doFinal(input)
    }

    private fun writeInt(target: ByteArray, offset: Int, value: Int) {
        target[offset] = (value ushr 24).toByte()
        target[offset + 1] = (value ushr 16).toByte()
        target[offset + 2] = (value ushr 8).toByte()
        target[offset + 3] = value.toByte()
    }

    fun readInt(bytes: ByteArray, offset: Int): Int {
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)
    }
}

class ExportCryptoException : Exception()
