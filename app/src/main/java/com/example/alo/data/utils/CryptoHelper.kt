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
import com.google.crypto.tink.HybridDecrypt
import com.google.crypto.tink.HybridEncrypt
import com.google.crypto.tink.PublicKeySign
import com.google.crypto.tink.PublicKeyVerify

object CryptoHelper {

    // URI của Master Key trong phần cứng Android Keystore
    private const val MASTER_KEY_URI = "android-keystore://alo_app_master_key"

    // Tên file SharedPreferences lưu các Keys đã bị mã hóa
    private const val PREF_FILE_NAME = "alo_app_crypto_keys"

    // Tên của 2 bộ khóa (Keyset)
    private const val ENCRYPT_KEYSET_NAME = "encrypt_keyset"
    private const val SIGN_KEYSET_NAME = "sign_keyset"
    private var cachedDecryptHandle: KeysetHandle? = null//cache save key


    /**
     * Hàm này GỌI 1 LẦN DUY NHẤT khi app khởi chạy
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
        // Lấy hoặc Tạo cặp khóa Mã hóa (ECIES) bằng hàm thông minh
        val encryptKeysetHandle = getValidKeysetHandle(
            context,
            ENCRYPT_KEYSET_NAME,
            KeyTemplates.get("ECIES_P256_HKDF_HMAC_SHA256_AES128_GCM")
        )

        // Lấy hoặc Tạo cặp khóa Chữ ký (ED25519) bằng hàm thông minh
        val signKeysetHandle = getValidKeysetHandle(
            context,
            SIGN_KEYSET_NAME,
            KeyTemplates.get("ED25519")
        )

        //  Trích xuất
        val publicEncryptKeyBase64 = extractPublicKeyToBase64(encryptKeysetHandle)
        val publicSignKeyBase64 = extractPublicKeyToBase64(signKeysetHandle)

        return Pair(publicEncryptKeyBase64, publicSignKeyBase64)
    }
    /**
     * HÀM HỖ TRỢ : Khởi tạo Keyset thông minh (Chống lỗi Crash do Backup/Uninstall)
     */
    private fun getValidKeysetHandle(context: Context, keysetName: String, template: com.google.crypto.tink.KeyTemplate): KeysetHandle {
        return try {
            AndroidKeysetManager.Builder()
                .withSharedPref(context, keysetName, PREF_FILE_NAME)
                .withKeyTemplate(template)
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
                .keysetHandle
        } catch (e: Exception) {
            // NẾU VÀO ĐÂY: Có nghĩa là file XML cũ được Restore nhưng Master Key phần cứng đã mất!
            android.util.Log.e("CRYPTO_ERROR", "Phát hiện Khóa cũ bị hỏng do cài lại app. Đang dọn dẹp...", e)

            // 1. Xóa sạch file SharedPreferences bị kẹt
            context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE).edit().clear().apply()

            // 2. Chạy lại lệnh tạo khóa từ đầu (Lúc này file đã sạch, Tink sẽ tự đẻ khóa mới)
            AndroidKeysetManager.Builder()
                .withSharedPref(context, keysetName, PREF_FILE_NAME)
                .withKeyTemplate(template)
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
                .keysetHandle
        }
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

            val emptyContextInfo = ByteArray(0)

            // 1. MÃ HÓA CHO NGƯỜI NHẬN (BOB)
            val receiverKeyHandle = getPublicKeyHandleFromBase64(receiverPublicEncryptKeyBase64)
            val receiverHybridEncrypt = receiverKeyHandle.getPrimitive(HybridEncrypt::class.java)
            // null ở tham số thứ 2 là contextInfo (chúng ta không cần dùng tới)
            val receiverCiphertext = receiverHybridEncrypt.encrypt(plaintextBytes, emptyContextInfo)
            val receiverCiphertextBase64 = Base64.encodeToString(receiverCiphertext, Base64.NO_WRAP)

            // 2. MÃ HÓA CHO CHÍNH MÌNH (ALICE - ĐỂ TỰ ĐỌC LẠI LỊCH SỬ)
            val myKeyHandle = getPublicKeyHandleFromBase64(myPublicEncryptKeyBase64)
            val myHybridEncrypt = myKeyHandle.getPrimitive(HybridEncrypt::class.java)
            val myCiphertext = myHybridEncrypt.encrypt(plaintextBytes, emptyContextInfo)
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
        // Biến lưu tạm Private Key vào RAM để giải mã hàng loạt tin nhắn mà không bị lag UI

    }
    /**
     * HÀM HỖ TRỢ: Lấy Private Key Mã hóa (Có Caching)
     */
    private fun getDecryptHandle(context: Context): KeysetHandle {
        if (cachedDecryptHandle == null) {
            cachedDecryptHandle = getValidKeysetHandle(
                context,
                ENCRYPT_KEYSET_NAME,
                KeyTemplates.get("ECIES_P256_HKDF_HMAC_SHA256_AES128_GCM")
            )
        }
        return cachedDecryptHandle!!
    }

    /**
     * HÀM GIẢI MÃ TIN NHẮN (DECRYPT)
     * Cần gọi hàm này cho mỗi tin nhắn tải về từ Supabase.
     */
    fun decryptMessage(
        context: Context,
        encryptedJson: String,
        senderPublicSignKeyBase64: String?, // Public Sign Key của người gửi (có thể null nếu là tin mình gửi)
        isMyMessage: Boolean                // Biến cờ: true nếu mình là người gửi tin này
    ): String {
        try {
            // Lấy công cụ giải mã (từ Private Key của chính mình)
            val hybridDecrypt = getDecryptHandle(context).getPrimitive(HybridDecrypt::class.java)

            // Parse chuỗi JSON (Dùng org.json có sẵn của Android)
            val jsonObject = org.json.JSONObject(encryptedJson)
            val emptyContextInfo = ByteArray(0)

            if (isMyMessage) {
                // ==========================================
                // TRƯỜNG HỢP 1: TIN NHẮN DO MÌNH TỰ GỬI
                // ==========================================
                val forSenderBase64 = jsonObject.getString("for_sender")
                val ciphertextBytes = Base64.decode(forSenderBase64, Base64.NO_WRAP)

                // Giải mã bằng Private Key của mình
                val plaintextBytes = hybridDecrypt.decrypt(ciphertextBytes, emptyContextInfo)
                return String(plaintextBytes, Charsets.UTF_8)

            } else {
                // ==========================================
                // TRƯỜNG HỢP 2: TIN NHẮN DO NGƯỜI KHÁC GỬI
                // ==========================================
                val forReceiverBase64 = jsonObject.getString("for_receiver")
                val signatureBase64 = jsonObject.getString("signature")

                val ciphertextBytes = Base64.decode(forReceiverBase64, Base64.NO_WRAP)
                val signatureBytes = Base64.decode(signatureBase64, Base64.NO_WRAP)

                // BƯỚC 2.1: SOÁT XÉT CHỮ KÝ!
                if (senderPublicSignKeyBase64 == null) {
                    throw Exception("Thiếu Public Sign Key của người gửi để xác minh!")
                }

                val senderSignKeyHandle = getPublicKeyHandleFromBase64(senderPublicSignKeyBase64)
                val verifier = senderSignKeyHandle.getPrimitive(PublicKeyVerify::class.java)

                // Hàm verify sẽ văng Exception ngay lập tức nếu Hacker tráo tin nhắn / sửa chữ ký
                verifier.verify(signatureBytes, ciphertextBytes)

                // BƯỚC 2.2: CHỮ KÝ CHUẨN -> GIẢI MÃ THÔI!
                val plaintextBytes = hybridDecrypt.decrypt(ciphertextBytes, emptyContextInfo)
                return String(plaintextBytes, Charsets.UTF_8)
            }

        } catch (e: Exception) {
            // Nếu parse JSON lỗi (do tin nhắn cũ trước khi có E2EE) hoặc giải mã thất bại
            android.util.Log.e("CRYPTO_ERROR", "Lỗi giải mã tin nhắn: ${e.message}")
            // Bạn có thể trả về chính encryptedJson nếu muốn xem raw, hoặc báo lỗi:
            return "🔒 Nội dung không thể giải mã"
        }
    }
}