package org.santayn.testing.mobile.core.storage

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager

/** Обычные настройки в SharedPreferences. */
class SharedPrefsStore(context: Context, name: String) : KeyValueStore {
    private val prefs: SharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String?) {
        prefs.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
    }

    override fun keys(): Set<String> = prefs.all.keys
}

/**
 * Защищённое хранилище: значения шифруются AES-256-GCM (Tink),
 * ключ набора ключей хранится в Android Keystore.
 */
class EncryptedPrefsStore(context: Context, name: String) : KeyValueStore {
    private val prefs: SharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    private val aead: Aead? = runCatching {
        AeadConfig.register()
        AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_PREFS)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
            .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }.getOrNull()

    override fun getString(key: String): String? {
        val encoded = prefs.getString(key, null) ?: return null
        val cipher = aead ?: return null
        return runCatching {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            cipher.decrypt(bytes, key.encodeToByteArray()).decodeToString()
        }.getOrNull()
    }

    override fun putString(key: String, value: String?) {
        val cipher = aead
        prefs.edit().apply {
            if (value == null || cipher == null) {
                remove(key)
            } else {
                val encrypted = cipher.encrypt(value.encodeToByteArray(), key.encodeToByteArray())
                putString(key, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            }
        }.apply()
    }

    override fun keys(): Set<String> = prefs.all.keys

    private companion object {
        const val KEYSET_NAME = "student_testing_keyset"
        const val KEYSET_PREFS = "student_testing_keyset_prefs"
        const val MASTER_KEY_URI = "android-keystore://student_testing_master_key"
    }
}
