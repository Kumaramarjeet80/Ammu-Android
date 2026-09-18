package com.ammu.player.crypto

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object SecuritySuite {

    private const val GCM_TAG_LENGTH = 128
    private const val PBKDF2_ITERATIONS = 100000
    private const val KEY_LENGTH = 256
    private const val PREFS_FILE = "ammu_secure_prefs"

    // Indian Standard Time Formatter
    fun getIndianStandardTime(date: Date = Date()): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy, hh:mm:ss a 'IST'", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        return sdf.format(date)
    }

    fun getIndianStandardDateOnly(date: Date = Date()): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        return sdf.format(date)
    }

    // SHA-256 Hashing for Passkeys
    fun hashPasskey(passkey: String): String {
        if (passkey.isBlank()) return ""
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(passkey.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    // Key Derivation from password and salt using PBKDF2
    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    // AES-256-GCM Encryption (Interoperable with Web Crypto JSON schema)
    fun encryptPayloadAES(plainText: String, password: String): String {
        val random = SecureRandom()
        val salt = ByteArray(16).apply { random.nextBytes(this) }
        val iv = ByteArray(12).apply { random.nextBytes(this) }

        val secretKey = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val json = JSONObject()
        json.put("encrypted", true)

        val saltArray = JSONArray()
        salt.forEach { saltArray.put(it.toInt() and 0xFF) }
        json.put("salt", saltArray)

        val ivArray = JSONArray()
        iv.forEach { ivArray.put(it.toInt() and 0xFF) }
        json.put("iv", ivArray)

        val cipherArray = JSONArray()
        cipherBytes.forEach { cipherArray.put(it.toInt() and 0xFF) }
        json.put("cipherData", cipherArray)

        return json.toString(2)
    }

    // AES-256-GCM Decryption (Decodes Web Crypto format)
    fun decryptPayloadAES(encryptedJsonStr: String, password: String): String {
        val json = JSONObject(encryptedJsonStr)
        val saltArray = json.getJSONArray("salt")
        val salt = ByteArray(saltArray.length()) { i -> saltArray.getInt(i).toByte() }

        val ivArray = json.getJSONArray("iv")
        val iv = ByteArray(ivArray.length()) { i -> ivArray.getInt(i).toByte() }

        val cipherArray = json.getJSONArray("cipherData")
        val cipherBytes = ByteArray(cipherArray.length()) { i -> cipherArray.getInt(i).toByte() }

        val secretKey = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        val decryptedBytes = cipher.doFinal(cipherBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    // EncryptedSharedPreferences for Admin Super-Key & Local App Preferences
    fun getEncryptedPrefs(context: Context) = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback to standard SharedPreferences if Keystore issue on some devices
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
    }

    fun saveAdminKey(context: Context, key: String) {
        val prefs = getEncryptedPrefs(context)
        val hash = hashPasskey(key)
        prefs.edit()
            .putString("account_admin_key_plain", key)
            .putString("account_admin_key_hash", hash)
            .apply()
    }

    fun removeAdminKey(context: Context) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit()
            .remove("account_admin_key_plain")
            .remove("account_admin_key_hash")
            .apply()
    }

    fun getAdminKeyPlain(context: Context): String? {
        return getEncryptedPrefs(context).getString("account_admin_key_plain", null)
    }

    fun getAdminKeyHash(context: Context): String? {
        return getEncryptedPrefs(context).getString("account_admin_key_hash", null)
    }

    fun verifyAdminKey(context: Context, enteredKey: String): Boolean {
        val savedHash = getAdminKeyHash(context) ?: return false
        return hashPasskey(enteredKey) == savedHash
    }
}
