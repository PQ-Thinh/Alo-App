package com.example.alo.data.utils

import android.content.Context
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.JsonKeysetWriter
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.hybrid.HybridConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.signature.SignatureConfig
import java.io.ByteArrayOutputStream
import android.util.Base64
import com.google.crypto.tink.HybridEncrypt
import com.google.crypto.tink.PublicKeySign

object CryptoHelper {

    // URI của Master Key trong phần cứng Android Keystore
    private const val MASTER_KEY_URI = "android-keystore://alo_app_master_key"

    // Tên file SharedPreferences lưu các Keys đã bị mã hóa
    private const val PREF_FILE_NAME = "alo_app_crypto_keys"

    // Tên của 2 bộ khóa (Keyset)
    private const val ENCRYPT_KEYSET_NAME = "encrypt_keyset"
    private const val SIGN_KEYSET_NAME = "sign_keyset"

    /**
     * Hàm này GỌI 1 LẦN DUY NHẤT khi app khởi chạy (ví dụ trong Application hoặc MainActivity)
     */
    fun initTink() {
        // Đăng ký các thuật toán Mã hóa Lai (ECDH + AES) và Chữ ký (Ed25519)
        HybridConfig.register()
        SignatureConfig.register()
    }

    /**
     * KHỞI TẠO HOẶC LẤY 2 CẶP KHÓA TỪ KEYSTORE
     * Trả về Pair<PublicEncryptKey, PublicSignKey> dạng Base64 để bạn upload lên Supabase
     */
    fun generateAndGetPublicKeys(context: Context): Pair<String, String> {
        // 1. Lấy hoặc Tạo cặp khóa Mã hóa (ECIES)
        val encryptKeysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, ENCRYPT_KEYSET_NAME, PREF_FILE_NAME)
            .withKeyTemplate(KeyTemplates.get("ECIES_P256_HKDF_HMAC_SHA256_AES128_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle

        // 2. Lấy hoặc Tạo cặp khóa Chữ ký (ED25519)
        val signKeysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, SIGN_KEYSET_NAME, PREF_FILE_NAME)
            .withKeyTemplate(KeyTemplates.get("ED25519"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle

        // 3. Trích xuất Public Keys từ 2 Handle này ra thành Base64 để gửi lên DB
        val publicEncryptKeyBase64 = extractPublicKeyToBase64(encryptKeysetHandle)
        val publicSignKeyBase64 = extractPublicKeyToBase64(signKeysetHandle)

        return Pair(publicEncryptKeyBase64, publicSignKeyBase64)
    }

    /**
     * Hàm hỗ trợ: Tách Public Key ra khỏi KeysetHandle và chuyển thành chuỗi Base64
     */
    private fun extractPublicKeyToBase64(privateKeysetHandle: KeysetHandle): String {
        val publicKeysetHandle = privateKeysetHandle.publicKeysetHandle
        val outputStream = ByteArrayOutputStream()

        // Vì đây chỉ là Public Key, việc lưu dạng Cleartext (không mã hóa) để gửi qua mạng là bình thường
        CleartextKeysetHandle.write(
            publicKeysetHandle,
            JsonKeysetWriter.withOutputStream(outputStream)
        )
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * HÀM HỖ TRỢ: Biến chuỗi Base64 Public Key (tải từ Supabase) ngược lại thành KeysetHandle của Tink
     */
    private fun getPublicKeyHandleFromBase64(publicKeyBase64: String): KeysetHandle {
        val publicKeyBytes = Base64.decode(publicKeyBase64, Base64.DEFAULT)
        // Đọc chuỗi byte rõ (Cleartext) thành Object KeysetHandle để Tink có thể xài
        return CleartextKeysetHandle.read(
            com.google.crypto.tink.JsonKeysetReader.withBytes(publicKeyBytes)
        )
    }

    /**
     * HÀM MÃ HÓA TIN NHẮN CHÍNH
     * Trả về một chuỗi JSON chứa đầy đủ thông tin để lưu thẳng vào cột `encrypted_content`
     */
    fun encryptMessage(
        context: Context,
        plaintext: String,
        receiverPublicEncryptKeyBase64: String, // Public Key của người nhận (Bob)
        myPublicEncryptKeyBase64: String        // Public Key của chính mình (Alice)
    ): String {
        try {
            val plaintextBytes = plaintext.toByteArray(Charsets.UTF_8)

            // 1. MÃ HÓA CHO NGƯỜI NHẬN (BOB)
            val receiverKeyHandle = getPublicKeyHandleFromBase64(receiverPublicEncryptKeyBase64)
            val receiverHybridEncrypt = receiverKeyHandle.getPrimitive(HybridEncrypt::class.java)
            // null ở tham số thứ 2 là contextInfo (chúng ta không cần dùng tới)
            val receiverCiphertext = receiverHybridEncrypt.encrypt(plaintextBytes, null)
            val receiverCiphertextBase64 = Base64.encodeToString(receiverCiphertext, Base64.NO_WRAP)

            // 2. MÃ HÓA CHO CHÍNH MÌNH (ALICE - ĐỂ TỰ ĐỌC LẠI LỊCH SỬ)
            val myKeyHandle = getPublicKeyHandleFromBase64(myPublicEncryptKeyBase64)
            val myHybridEncrypt = myKeyHandle.getPrimitive(HybridEncrypt::class.java)
            val myCiphertext = myHybridEncrypt.encrypt(plaintextBytes, null)
            val myCiphertextBase64 = Base64.encodeToString(myCiphertext, Base64.NO_WRAP)

            // 3. KÝ ĐIỆN TỬ (ĐÓNG MỘC BẰNG PRIVATE KEY CỦA ALICE)
            // Lấy lén Private Sign Key từ Keystore lên (Giống y chang lúc tạo)
            val signKeysetHandle = AndroidKeysetManager.Builder()
                .withSharedPref(context, SIGN_KEYSET_NAME, PREF_FILE_NAME)
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
                .keysetHandle

            val signer = signKeysetHandle.getPrimitive(PublicKeySign::class.java)
            // Chúng ta sẽ ký lên bản mã hóa của người nhận để đảm bảo nó không bị tráo trên đường đi
            val signature = signer.sign(receiverCiphertext)
            val signatureBase64 = Base64.encodeToString(signature, Base64.NO_WRAP)

            // 4. ĐÓNG GÓI THÀNH CHUỖI JSON
            val payload = org.json.JSONObject()
            payload.put("for_receiver", receiverCiphertextBase64)
            payload.put("for_sender", myCiphertextBase64)
            payload.put("signature", signatureBase64)

            // Chuỗi String này chính là thứ sẽ được INSERT vào cột encrypted_content trên Supabase!
            return payload.toString()

        } catch (e: Exception) {
            android.util.Log.e("CRYPTO_ERROR", "Lỗi mã hóa tin nhắn: ${e.message}", e)
            return "" // Trả về rỗng nếu có lỗi để chặn việc gửi tin nhắn lỗi lên Server
        }
    }
}