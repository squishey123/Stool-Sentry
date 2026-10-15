package com.squishey.stoolsentry.data

internal object SqlCipher {
    @Volatile private var loaded = false

    fun load() {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            System.loadLibrary("sqlcipher")
            loaded = true
        }
    }
}
