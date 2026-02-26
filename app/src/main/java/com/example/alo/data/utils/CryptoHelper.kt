package com.example.alo.data.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.util.Base64

object CryptoHelper {
    private const val KEY_ALIAS = "alo_chat_key_alias"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    // Hàm sinh và lấy Public Key (dạng Base64 String để lưu lên DB)
    fun getOrGeneratePublicKey(): String {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

        // Kiểm tra xem máy đã có key chưa, nếu chưa thì tạo mới
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateRsaKeyPair()
        }

        // Lấy Public key ra
        val publicKey = keyStore.getCertificate(KEY_ALIAS).publicKey

        // Encode sang Base64 chuỗi String để ném lên database
        return Base64.getEncoder().encodeToString(publicKey.encoded)
    }

    private fun generateRsaKeyPair() {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA,
            ANDROID_KEYSTORE
        )

        // Cấu hình mã hoá RSA chuẩn
        val parameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
            .setKeySize(2048)
            .build()

        keyPairGenerator.initialize(parameterSpec)
        keyPairGenerator.generateKeyPair() // Lưu thẳng vào phần cứng Android Keystore
    }
}