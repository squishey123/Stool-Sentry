package com.squishey.stoolsentry.data

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseFilesTest {
    @Test
    fun sqliteHeaderIsPlaintext() {
        val dir = tempDir()
        val main = dir.resolve(DatabaseFiles.MAIN).toFile()
        main.writeBytes(DatabaseFiles.SQLITE_HEADER.toByteArray())
        assertEquals(DatabaseFiles.Kind.Plaintext, DatabaseFiles.kind(main))
    }

    @Test
    fun otherBytesAreEncryptedAndMissingIsAbsent() {
        val dir = tempDir()
        val main = dir.resolve(DatabaseFiles.MAIN).toFile()
        assertEquals(DatabaseFiles.Kind.Absent, DatabaseFiles.kind(main))
        main.writeBytes(byteArrayOf(1, 2, 3, 4))
        assertEquals(DatabaseFiles.Kind.Encrypted, DatabaseFiles.kind(main))
        main.writeBytes(ByteArray(0))
        assertEquals(DatabaseFiles.Kind.Absent, DatabaseFiles.kind(main))
    }

    @Test
    fun bothFilesKeepTheOriginalAndDropTheMigratingCopy() {
        val dir = tempDir()
        val main = dir.resolve(DatabaseFiles.MAIN).toFile()
        val migrating = dir.resolve(DatabaseFiles.MIGRATING).toFile()
        main.writeBytes(DatabaseFiles.SQLITE_HEADER.toByteArray())
        migrating.writeBytes(byteArrayOf(9, 9, 9))
        dir.resolve("${DatabaseFiles.MIGRATING}-wal").toFile().writeBytes(byteArrayOf(1))
        assertEquals(DatabaseFiles.Recovery.DeletedMigrating, DatabaseFiles.recover(dir.toFile()))
        assertTrue(main.exists())
        assertFalse(migrating.exists())
        assertFalse(dir.resolve("${DatabaseFiles.MIGRATING}-wal").toFile().exists())
    }

    @Test
    fun missingOriginalFinishesTheRenameIncludingSidecars() {
        val dir = tempDir()
        val migrating = dir.resolve(DatabaseFiles.MIGRATING).toFile()
        migrating.writeBytes(byteArrayOf(4, 5, 6))
        dir.resolve("${DatabaseFiles.MIGRATING}-wal").toFile().writeBytes(byteArrayOf(7))
        assertEquals(DatabaseFiles.Recovery.FinishedRename, DatabaseFiles.recover(dir.toFile()))
        val main = dir.resolve(DatabaseFiles.MAIN).toFile()
        assertTrue(main.exists())
        assertEquals(listOf<Byte>(4, 5, 6), main.readBytes().toList())
        assertTrue(dir.resolve("${DatabaseFiles.MAIN}-wal").toFile().exists())
        assertFalse(migrating.exists())
    }

    @Test
    fun aSingleOriginalIsLeftAlone() {
        val dir = tempDir()
        dir.resolve(DatabaseFiles.MAIN).toFile().writeBytes(DatabaseFiles.SQLITE_HEADER.toByteArray())
        assertEquals(DatabaseFiles.Recovery.None, DatabaseFiles.recover(dir.toFile()))
    }

    private fun tempDir() = Files.createTempDirectory("stool-sentry-db")
}
