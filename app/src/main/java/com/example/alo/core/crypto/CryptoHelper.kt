package com.example.alo.core.crypto

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

import com.example.alo.core.utils.Constant

object CryptoHelper {

    // Cache giải mã: lưu cả userId để invalidate khi đổi tài khoản
    private var cachedDecryptHandle: KeysetHandle? = null
    private var cachedDecryptUserId: String? = null

    /**
     *  Đăng ký các thuật toán Mã hóa Lai (ECDH + AES) và Chữ ký (Ed25519)
     */
    fun initTink() {
        HybridConfig.register()
        SignatureConfig.register()
    }

    /**
     * Xóa cache giải mã. GỌI MỖI KHI LOGOUT hoặc đổi tài khoản.
     */
    fun clearCachedKeys() {
        cachedDecryptHandle = null
        cachedDecryptUserId = null
    }

    /**
     * Xóa toàn bộ local keys của 1 user cụ thể.
     * Dùng khi cần force tạo lại key (ít khi dùng).
     */
    fun clearLocalKeys(context: Context, userId: String) {
        context.getSharedPreferences(Constant.prefFileName(userId), Context.MODE_PRIVATE)
            .edit().clear().apply()
        // Invalidate cache nếu đang cache cho user này
        if (cachedDecryptUserId == userId) {
            clearCachedKeys()
        }
    }

    /**
     * Dọn dẹp key cũ (legacy) từ SharedPref dùng chung.
     * Gọi 1 lần sau khi migrate xong.
     */
    fun cleanupLegacyKeys(context: Context) {
        try {
            context.getSharedPreferences(Constant.PREF_FILE_NAME, Context.MODE_PRIVATE)
                .edit().clear().apply()
        } catch (e: Exception) {
            android.util.Log.w("CRYPTO", "Không tìm thấy legacy keys để dọn dẹp: ${e.message}")
        }
    }

    /**
     * KHỞI TẠO HOẶC LẤY 2 CẶP KHÓA TỪ KEYSTORE (PER-USER)
     * Trả về Pair<PublicEncryptKey, PublicSignKey> dạng Base64 để upload lên Supabase
     */
    fun generateAndGetPublicKeys(context: Context, userId: String): Pair<String, String> {
        // Lấy hoặc Tạo cặp khóa Mã hóa (ECIES) - riêng cho userId
        val encryptKeysetHandle = getValidKeysetHandle(
            context,
            Constant.encryptKeysetName(userId),
            Constant.prefFileName(userId),
            KeyTemplates.get("ECIES_P256_HKDF_HMAC_SHA256_AES128_GCM")
        )

        // Lấy hoặc Tạo cặp khóa Chữ ký (ED25519) - riêng cho userId
        val signKeysetHandle = getValidKeysetHandle(
            context,
            Constant.signKeysetName(userId),
            Constant.prefFileName(userId),
            KeyTemplates.get("ED25519")
        )

        //  Trích xuất
        val publicEncryptKeyBase64 = extractPublicKeyToBase64(encryptKeysetHandle)
        val publicSignKeyBase64 = extractPublicKeyToBase64(signKeysetHandle)

        return Pair(publicEncryptKeyBase64, publicSignKeyBase64)
    }

    /**
     * HÀM HỖ TRỢ: Khởi tạo Keyset (Chống lỗi Crash do Backup/Uninstall)
     * Sửa: chỉ xóa keyset bị lỗi, KHÔNG xóa cả file SharedPref.
     */
    private fun getValidKeysetHandle(
        context: Context,
        keysetName: String,
        prefFileName: String,
        template: com.google.crypto.tink.KeyTemplate
    ): KeysetHandle {
        return try {
            AndroidKeysetManager.Builder()
                .withSharedPref(context, keysetName, prefFileName)
                .withKeyTemplate(template)
                .withMasterKeyUri(Constant.MASTER_KEY_URI)
                .build()
                .keysetHandle
        } catch (e: Exception) {
            // NẾU VÀO ĐÂY: file XML cũ được Restore nhưng Master Key phần cứng đã mất!
            android.util.Log.e("CRYPTO_ERROR", "Phát hiện Khóa cũ bị hỏng do cài lại app. Đang dọn dẹp keyset: $keysetName", e)

            // Chỉ xóa keyset bị lỗi (KHÔNG xóa cả file, tránh ảnh hưởng keyset khác)
            val prefs = context.getSharedPreferences(prefFileName, Context.MODE_PRIVATE)
            prefs.edit().remove(keysetName).apply()

            // Chạy lại lệnh tạo khóa từ đầu
            AndroidKeysetManager.Builder()
                .withSharedPref(context, keysetName, prefFileName)
                .withKeyTemplate(template)
                .withMasterKeyUri(Constant.MASTER_KEY_URI)
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
        userId: String,
        plaintext: String,
        receiverPublicEncryptKeyBase64: String,
        myPublicEncryptKeyBase64: String
    ): String {
        try {
            val plaintextBytes = plaintext.toByteArray(Charsets.UTF_8)

            val emptyContextInfo = ByteArray(0)

            // 1. MÃ HÓA CHO NGƯỜI NHẬN (BOB)
            val receiverKeyHandle = getPublicKeyHandleFromBase64(receiverPublicEncryptKeyBase64)
            val receiverHybridEncrypt = receiverKeyHandle.getPrimitive(HybridEncrypt::class.java)
            val receiverCiphertext = receiverHybridEncrypt.encrypt(plaintextBytes, emptyContextInfo)
            val receiverCiphertextBase64 = Base64.encodeToString(receiverCiphertext, Base64.NO_WRAP)

            // 2. MÃ HÓA CHO CHÍNH MÌNH (ALICE - ĐỂ TỰ ĐỌC LẠI LỊCH SỬ)
            val myKeyHandle = getPublicKeyHandleFromBase64(myPublicEncryptKeyBase64)
            val myHybridEncrypt = myKeyHandle.getPrimitive(HybridEncrypt::class.java)
            val myCiphertext = myHybridEncrypt.encrypt(plaintextBytes, emptyContextInfo)
            val myCiphertextBase64 = Base64.encodeToString(myCiphertext, Base64.NO_WRAP)

            // 3. KÝ ĐIỆN TỬ (ĐÓNG MỘC BẰNG PRIVATE KEY CỦA ALICE)
            // Lấy Private Sign Key từ Keystore (per-user)
            val signKeysetHandle = getValidKeysetHandle(
                context,
                Constant.signKeysetName(userId),
                Constant.prefFileName(userId),
                KeyTemplates.get("ED25519")
            )

            val signer = signKeysetHandle.getPrimitive(PublicKeySign::class.java)
            // Ký lên bản mã hóa của người nhận để đảm bảo nó không bị tráo trên đường đi
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
            return ""
        }

    }
    /**
     * HÀM HỖ TRỢ: Lấy Private Key Mã hóa (Có Caching, per-user)
     */
    private fun getDecryptHandle(context: Context, userId: String): KeysetHandle {
        // Invalidate cache nếu userId khác
        if (cachedDecryptHandle == null || cachedDecryptUserId != userId) {
            cachedDecryptHandle = getValidKeysetHandle(
                context,
                Constant.encryptKeysetName(userId),
                Constant.prefFileName(userId),
                KeyTemplates.get("ECIES_P256_HKDF_HMAC_SHA256_AES128_GCM")
            )
            cachedDecryptUserId = userId
        }
        return cachedDecryptHandle!!
    }

    /**
     * HÀM GIẢI MÃ TIN NHẮN (DECRYPT)
     * Cần gọi hàm này cho mỗi tin nhắn tải về từ Supabase.
     */
    fun decryptMessage(
        context: Context,
        userId: String,
        encryptedJson: String,
        senderPublicSignKeyBase64: String?, // Public Sign Key của người gửi (có thể null nếu là tin mình gửi)
        isMyMessage: Boolean                // Biến cờ: true nếu mình là người gửi tin này
    ): String {
        try {
            // Lấy công cụ giải mã (từ Private Key của chính mình, per-user)
            val hybridDecrypt = getDecryptHandle(context, userId).getPrimitive(HybridDecrypt::class.java)

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
            return "🔒 Tin nhắn từ phiên bảo mật trước"
        }
    }

    // ==========================================
    // PHẦN MỚI: HỖ TRỢ TIN NHẮN NHÓM (GROUP CHAT E2EE)
    // ==========================================

    /**
     * Tạo một "Khóa nhóm" ngẫu nhiên (AES-256 GCM)
     */
    fun generateGroupKeysetHandle(): KeysetHandle {
        return KeysetHandle.generateNew(com.google.crypto.tink.aead.AeadKeyTemplates.AES256_GCM)
    }

    /**
     * Chuyển KeysetHandle thành chuỗi Base64 (Cleartext - Cần được mã hóa trước khi gửi lên server!)
     */
    fun exportKeysetToBase64(keysetHandle: KeysetHandle): String {
        val outputStream = ByteArrayOutputStream()
        CleartextKeysetHandle.write(keysetHandle, JsonKeysetWriter.withOutputStream(outputStream))
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Nhập KeysetHandle từ chuỗi Base64
     */
    fun importKeysetFromBase64(base64: String): KeysetHandle {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        return CleartextKeysetHandle.read(com.google.crypto.tink.JsonKeysetReader.withBytes(bytes))
    }

    /**
     * Mã hóa tin nhắn cho nhóm dùng khóa AES đã có sẵn
     */
    fun encryptGroupMessage(plaintext: String, groupKeysetHandle: KeysetHandle): String {
        try {
            val aead = groupKeysetHandle.getPrimitive(com.google.crypto.tink.Aead::class.java)
            val ciphertext = aead.encrypt(plaintext.toByteArray(Charsets.UTF_8), null)
            val ciphertextBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)

            val payload = org.json.JSONObject()
            payload.put("group_ciphertext", ciphertextBase64)
            return payload.toString()
        } catch (e: Exception) {
            return ""
        }
    }

    /**
     * Giải mã tin nhắn nhóm
     */
    fun decryptGroupMessage(encryptedJson: String, groupKeysetHandle: KeysetHandle): String {
        try {
            val jsonObject = org.json.JSONObject(encryptedJson)
            val ciphertextBase64 = jsonObject.getString("group_ciphertext")
            val ciphertext = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
            
            val aead = groupKeysetHandle.getPrimitive(com.google.crypto.tink.Aead::class.java)
            val plaintext = aead.decrypt(ciphertext, null)
            return String(plaintext, Charsets.UTF_8)
        } catch (e: Exception) {
            return "🔒 Tin nhắn từ phiên bảo mật trước"
        }
    }

    /**
     * Mã hóa "Khóa nhóm" cho một thành viên cụ thể (Dùng Hybrid Encryption 1-1 cũ)
     */
    fun wrapGroupKey(groupKeysetBase64: String, memberPublicEncryptKeyBase64: String): String {
        try {
            val plaintextBytes = groupKeysetBase64.toByteArray(Charsets.UTF_8)
            val keyHandle = getPublicKeyHandleFromBase64(memberPublicEncryptKeyBase64)
            val hybridEncrypt = keyHandle.getPrimitive(HybridEncrypt::class.java)
            val ciphertext = hybridEncrypt.encrypt(plaintextBytes, ByteArray(0))
            return Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        } catch (e: Exception) {
            return ""
        }
    }

    /**
     * Giải mã "Khóa nhóm" mà mình nhận được (Dùng Private Key của mình, per-user)
     */
    fun unwrapGroupKey(context: Context, userId: String, wrappedKeyBase64: String): String {
        try {
            val ciphertext = Base64.decode(wrappedKeyBase64, Base64.NO_WRAP)
            val hybridDecrypt = getDecryptHandle(context, userId).getPrimitive(HybridDecrypt::class.java)
            val plaintext = hybridDecrypt.decrypt(ciphertext, ByteArray(0))
            return String(plaintext, Charsets.UTF_8)
        } catch (e: Exception) {
            return ""
        }
    }

    fun hashPin(pin: String): String {
        val bytes = pin.toByteArray(Charsets.UTF_8)
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}