package com.squishey.stoolsentry.data

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.squishey.stoolsentry.domain.ExportCrypto
import java.io.File
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Convenience copy of the passphrase and export key on this phone.
 * The Keystore key is not tied to biometrics or the screen-lock credential.
 * Losing this copy means typing the passphrase again. It does not destroy the logs.
 */
class KeyWrap(context: Context) {
    private val wrapFile = File(context.applicationContext.filesDir, DatabaseFiles.WRAP)

    fun store(session: ExportSession): Boolean {
        val plain = WrapBlob.encode(
            passphrase = session.passphrase,
            key = session.exportKey.key,
            salt = session.exportKey.salt,
            iterations = session.exportKey.iterations,
        )
        if (runCatching { write(encrypt(plain)) }.isSuccess) return true
        clear()
        return runCatching { write(encrypt(plain)) }.isSuccess
    }

    fun read(): ExportSession? {
        if (!wrapFile.isFile) return null
        return try {
            val decoded = WrapBlob.decode(decrypt(wrapFile.readBytes())) ?: return null.also { clear() }
            ExportSession(
                passphrase = decoded.passphrase,
                exportKey = ExportCrypto.ExportKey(decoded.key, decoded.salt, decoded.iterations),
            )
        } catch (_: GeneralSecurityException) {
            clear()
            null
        } catch (_: Exception) {
            null
        }
    }

    fun clear() {
        wrapFile.delete()
        runCatching {
            val store = keyStore()
            if (store.containsAlias(ALIAS)) store.deleteEntry(ALIAS)
        }
    }

    private fun write(bytes: ByteArray) {
        wrapFile.parentFile?.mkdirs()
        wrapFile.writeBytes(bytes)
    }

    private fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val body = cipher.doFinal(plain)
        return byteArrayOf(iv.size.toByte()) + iv + body
    }

    private fun decrypt(container: ByteArray): ByteArray {
        if (container.isEmpty()) error("empty wrap")
        val ivLen = container[0].toInt() and 0xFF
        if (ivLen == 0 || container.size <= 1 + ivLen) error("short wrap")
        val iv = container.copyOfRange(1, 1 + ivLen)
        val body = container.copyOfRange(1 + ivLen, container.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(body)
    }

    private fun secretKey(): SecretKey {
        val store = keyStore()
        val existing = store.getKey(ALIAS, null) as? SecretKey
        if (existing != null) return existing
        return generate(preferUnlockedDevice = true)
    }

    private fun generate(preferUnlockedDevice: Boolean): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        val builder = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .setRandomizedEncryptionRequired(true)
        if (preferUnlockedDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setUnlockedDeviceRequired(true)
        }
        return try {
            generator.init(builder.build())
            generator.generateKey()
        } catch (_: Exception) {
            if (!preferUnlockedDevice) throw IllegalStateException("Could not create the convenience key")
            generate(preferUnlockedDevice = false)
        }
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

    private companion object {
        const val ALIAS = "stool_sentry_wrap"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

class ExportSession(
    val passphrase: ByteArray,
    val exportKey: ExportCrypto.ExportKey,
)
