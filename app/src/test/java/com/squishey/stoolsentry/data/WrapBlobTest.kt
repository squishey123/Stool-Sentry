package com.squishey.stoolsentry.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WrapBlobTest {
    @Test
    fun roundTrip() {
        val decoded = WrapBlob.decode(
            WrapBlob.encode(
                passphrase = "sentence".toByteArray(),
                key = ByteArray(32) { it.toByte() },
                salt = ByteArray(16) { (it + 3).toByte() },
                iterations = 600_000,
            ),
        )
        requireNotNull(decoded)
        assertArrayEquals("sentence".toByteArray(), decoded.passphrase)
        assertArrayEquals(ByteArray(32) { it.toByte() }, decoded.key)
        assertArrayEquals(ByteArray(16) { (it + 3).toByte() }, decoded.salt)
        assertEquals(600_000, decoded.iterations)
    }

    @Test
    fun truncatedOrForeignBytesAreRejected() {
        assertNull(WrapBlob.decode(byteArrayOf(1, 2, 3)))
        val blob = WrapBlob.encode(
            passphrase = byteArrayOf(1),
            key = ByteArray(32),
            salt = ByteArray(16),
            iterations = 10,
        )
        assertNull(WrapBlob.decode(blob.copyOf(blob.size - 1)))
        blob[0] = 'X'.code.toByte()
        assertNull(WrapBlob.decode(blob))
    }
}
