package com.squishey.stoolsentry.data

import java.io.File

/**
 * The on-device database files and the crash recovery for a swap between
 * [MAIN] and [MIGRATING]. The original file is only removed after the new
 * file has been checked, so a crash keeps one complete copy.
 */
object DatabaseFiles {
    const val MAIN = "stool_sentry.db"
    const val MIGRATING = "stool_sentry_migrating.db"
    const val WRAP = "stool_sentry.wrap"
    const val SQLITE_HEADER = "SQLite format 3"

    enum class Kind { Absent, Plaintext, Encrypted }

    sealed interface Recovery {
        data object None : Recovery
        data object DeletedMigrating : Recovery
        data object FinishedRename : Recovery
    }

    fun kind(database: File): Kind {
        if (!database.isFile || database.length() == 0L) return Kind.Absent
        val header = ByteArray(SQLITE_HEADER.length)
        val read = database.inputStream().use { it.read(header) }
        if (read == header.size && header.decodeToString() == SQLITE_HEADER) return Kind.Plaintext
        return Kind.Encrypted
    }

    fun recover(directory: File): Recovery {
        if (!directory.isDirectory) return Recovery.None
        val main = File(directory, MAIN)
        val migrating = File(directory, MIGRATING)
        val mainThere = kind(main) != Kind.Absent
        val migratingThere = kind(migrating) != Kind.Absent
        if (mainThere && migrating.exists()) {
            deleteAll(migrating)
            return if (migratingThere) Recovery.DeletedMigrating else Recovery.None
        }
        if (!mainThere && migratingThere) {
            deleteAll(main)
            rename(migrating, main)
            return Recovery.FinishedRename
        }
        return Recovery.None
    }

    fun deleteAll(database: File) {
        sidecars(database).forEach { it.delete() }
    }

    fun rename(from: File, to: File) {
        val suffixes = listOf("", "-wal", "-shm", "-journal")
        suffixes.forEach { suffix -> File(to.path + suffix).delete() }
        suffixes.forEach { suffix ->
            val source = File(from.path + suffix)
            if (!source.exists()) return@forEach
            val dest = File(to.path + suffix)
            if (!source.renameTo(dest)) error("Could not rename ${source.name}")
        }
    }

    private fun sidecars(database: File): List<File> = listOf(
        database,
        File(database.path + "-wal"),
        File(database.path + "-shm"),
        File(database.path + "-journal"),
    )
}
