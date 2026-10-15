package com.squishey.stoolsentry.domain

import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportCryptoTest {
    private val csv = "local_date,local_time\n2026-10-14,08:15\n"

    @Test
    fun roundTrip() {
        val sealed = ExportCrypto.encrypt(csv.toByteArray(), "correct horse", iterations = FAST)
        val opened = ExportCrypto.decrypt(sealed, "correct horse").toString(StandardCharsets.UTF_8)
        assertEquals(csv, opened)
    }

    @Test
    fun shortPassphraseRoundTrips() {
        val sealed = ExportCrypto.encrypt("a".toByteArray(), "x", iterations = FAST)
        assertEquals("a", ExportCrypto.decrypt(sealed, "x").toString(StandardCharsets.UTF_8))
    }

    @Test
    fun whitespacePassphraseRoundTrips() {
        val sealed = ExportCrypto.encrypt(csv.toByteArray(), " ", iterations = FAST)
        assertEquals(csv, ExportCrypto.decrypt(sealed, " ").toString(StandardCharsets.UTF_8))
    }

    @Test
    fun wrongPassphraseFails() {
        val sealed = ExportCrypto.encrypt(csv.toByteArray(), "one", iterations = FAST)
        assertThrows(ExportCryptoException::class.java) {
            ExportCrypto.decrypt(sealed, "two")
        }
    }

    @Test
    fun tamperedCiphertextFails() {
        val sealed = ExportCrypto.encrypt(csv.toByteArray(), "one", iterations = FAST)
        sealed[sealed.lastIndex] = (sealed[sealed.lastIndex].toInt() xor 0x01).toByte()
        assertThrows(ExportCryptoException::class.java) {
            ExportCrypto.decrypt(sealed, "one")
        }
    }

    @Test
    fun legacyCsvIsNotASealedFile() {
        val legacy = csv.toByteArray()
        assertFalse(ExportCrypto.looksEncrypted(legacy))
        assertThrows(ExportCryptoException::class.java) {
            ExportCrypto.decrypt(legacy, "anything")
        }
    }

    @Test
    fun nfcNormalizationMatchesComposedAndDecomposed() {
        val decomposed = "cafe\u0301"
        val composed = "caf\u00e9"
        val sealed = ExportCrypto.encrypt(csv.toByteArray(), decomposed, iterations = FAST)
        assertEquals(csv, ExportCrypto.decrypt(sealed, composed).toString(StandardCharsets.UTF_8))
        assertEquals(ExportCrypto.normalize(decomposed), ExportCrypto.normalize(composed))
    }

    @Test
    fun blankPassphraseIsRejected() {
        assertTrue(ExportCrypto.normalize("").isEmpty())
        assertThrows(IllegalArgumentException::class.java) {
            ExportCrypto.encrypt(csv.toByteArray(), "", iterations = FAST)
        }
    }

    @Test
    fun twoSealsOfTheSameTextDiffer() {
        val first = ExportCrypto.encrypt(csv.toByteArray(), "same", iterations = FAST)
        val second = ExportCrypto.encrypt(csv.toByteArray(), "same", iterations = FAST)
        assertFalse(first.contentEquals(second))
    }

    @Test
    fun defaultEncryptionStoresSixHundredThousandIterations() {
        val sealed = ExportCrypto.encrypt("ok".toByteArray(), "passphrase")
        assertEquals(ExportCrypto.ITERATIONS, ExportCrypto.readInt(sealed, ExportCrypto.OFFSET_ITERATIONS))
        assertEquals(600_000, ExportCrypto.ITERATIONS)
        assertTrue(ExportCrypto.looksEncrypted(sealed))
    }

    private companion object {
        const val FAST = 1_000
    }
}
