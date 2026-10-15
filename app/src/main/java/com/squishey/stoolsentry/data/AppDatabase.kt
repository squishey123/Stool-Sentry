package com.squishey.stoolsentry.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [BowelLogEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bowelLogDao(): BowelLogDao

    companion object {
        fun open(context: Context, name: String, passphrase: ByteArray?): AppDatabase {
            val builder = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                name,
            )
            if (passphrase != null) {
                SqlCipher.load()
                builder.openHelperFactory(SupportOpenHelperFactory(passphrase.copyOf(), null, true))
            }
            return builder.build()
        }
    }
}
