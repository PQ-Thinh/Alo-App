package com.example.alo.data.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.util.Base64

object CryptoHelper {
    private const val KEY_ALIAS = "alo_chat_key_alias"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    fun getOrGeneratePublicKey(): String {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateRsaKeyPair()
        }

        val publicKey = keyStore.getCertificate(KEY_ALIAS).publicKey

        return Base64.getEncoder().encodeToString(publicKey.encoded)
    }

    private fun generateRsaKeyPair() {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA,
            ANDROID_KEYSTORE
        )

        val parameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)//Optimal Asymmetric Encryption Padding) kết hợp với SHA-256
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setKeySize(2048)
            .build()

        keyPairGenerator.initialize(parameterSpec)
        keyPairGenerator.generateKeyPair()
    }
}