package com.example.alo.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alo.data.utils.CryptoHelper
import com.example.alo.domain.model.Attachment
import com.example.alo.domain.model.Message
import com.example.alo.domain.repository.AttachmentRepository
import com.example.alo.domain.repository.AuthRepository
import com.example.alo.domain.repository.ConversationRepository
import com.example.alo.domain.repository.MessageRepository
import com.example.alo.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import javax.inject.Inject

@HiltViewModel
class ChatRoomViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository,
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository,
    private val attachmentRepository: AttachmentRepository
) : ViewModel() {

    val conversationId: String = checkNotNull(savedStateHandle["conversationId"])

    val _partnerId = MutableStateFlow("")
    val partnerId: StateFlow<String> = _partnerId.asStateFlow()
    val _partnerLastSeen = MutableStateFlow("")
    val partnerLastSeen: StateFlow<String> = _partnerLastSeen.asStateFlow()

    private val _currentUserId = MutableStateFlow("")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()

    private val _partnerName = MutableStateFlow("Đang tải...")
    val partnerName: StateFlow<String> = _partnerName.asStateFlow()

    private val _partnerAvatar = MutableStateFlow("")
    val partnerAvatar: StateFlow<String> = _partnerAvatar.asStateFlow()

    private val _isPartnerTyping = MutableStateFlow(false)
    val isPartnerTyping: StateFlow<Boolean> = _isPartnerTyping.asStateFlow()

    private val _isShowingRawEncryption = MutableStateFlow(false)
    val isShowingRawEncryption: StateFlow<Boolean> = _isShowingRawEncryption.asStateFlow()



    // cache save publicEncryptKey
    private var myPublicEncryptKey: String = ""
    private var partnerPublicEncryptKey: String = ""
    private var partnerPublicSignKey: String? = null

    private var typingJob: Job? = null
    private var lastTypingTime = 0L

    init {
        initializeChatRoom()
    }

    private fun initializeChatRoom() {
        viewModelScope.launch {
            val user = authRepository.getCurrentAuthUser()
            if (user != null) {
                _currentUserId.value = user.id
                try {
                    conversationRepository.resetUnreadCount(conversationId, user.id)

                    val myProfile = userRepository.getCurrentUser(user.id)
                    myPublicEncryptKey = myProfile?.publicEncryptKey ?: ""

                    val chatList = conversationRepository.getChatList(user.id)
                    val currentChatInfo = chatList.find { it.conversationId == conversationId }

                    if (currentChatInfo != null) {
                        _partnerName.value = currentChatInfo.chatName ?: "Người dùng ẩn danh"
                        _partnerAvatar.value = currentChatInfo.chatAvatar ?: ""

                        val pId = currentChatInfo.targetUserId ?: ""
                        _partnerId.value = pId
                        _partnerLastSeen.value = currentChatInfo.targetLastSeen ?: ""

                        if (pId.isNotEmpty()) {
                            val partnerProfile = userRepository.getCurrentUser(pId)
                            partnerPublicEncryptKey = partnerProfile?.publicEncryptKey ?: ""

                            partnerPublicSignKey = partnerProfile?.publicSignKey
                        }
                    }
                } catch (e: Exception) {}
            }

            val historyMessages = messageRepository.getMessages(conversationId)
            val decryptedHistory = historyMessages.map { msg ->
                decryptMessageObj(msg)
            }
            _messages.value = decryptedHistory

            historyMessages.forEach { msg ->
                if (msg.senderId != user?.id && !msg.seenBy.contains(user?.id)) {
                    viewModelScope.launch {
                        user?.id?.let { uid -> messageRepository.markMessageAsSeen(msg.id, uid) }                    }
                }
            }
            messageRepository.subscribeToNewMessages(conversationId
            , onTyping = { typingUserId ->
                    if (typingUserId != _currentUserId.value) {
                        typingJob?.cancel()
                        typingJob = viewModelScope.launch {
                            _isPartnerTyping.value = true
                            delay(3000) //
                            _isPartnerTyping.value = false
                        }
                    }
                }
                ).collect { newMessage ->
                val decryptedNewMsg = decryptMessageObj(newMessage)

                if (decryptedNewMsg.senderId != user?.id && !decryptedNewMsg.seenBy.contains(user?.id)) {
                    viewModelScope.launch {
                        messageRepository.markMessageAsSeen(decryptedNewMsg.id, user!!.id)
                    }
                }

                _messages.update { currentList ->
                    if (currentList.none { it.id == decryptedNewMsg.id }) {
                        listOf(decryptedNewMsg) + currentList
                    } else {
                        currentList.map { if (it.id == decryptedNewMsg.id) decryptedNewMsg else it }
                    }
                }
            }
        }
    }

    private fun decryptMessageObj(msg: Message): Message {
        val isMine = (msg.senderId == _currentUserId.value)

        // Nếu không phải tin mình gửi, lấy Sign Key của đối phương để dò mộc
        val senderSignKey = if (!isMine) partnerPublicSignKey else null

        val clearText = CryptoHelper.decryptMessage(
            context = context,
            encryptedJson = msg.encryptedContent,
            senderPublicSignKeyBase64 = senderSignKey,
            isMyMessage = isMine
        )

        return msg.copy(
            encryptedContent = clearText.ifEmpty { "[Lỗi giải mã/Chưa mã hóa]" },
            rawEncryptedContent = msg.encryptedContent
        )
    }



    fun sendMessage(replyToId: String? = null) {
        val content = _messageText.value.trim()
        val senderId = _currentUserId.value

        if (content.isEmpty() || senderId.isEmpty()) return

        if (myPublicEncryptKey.isEmpty() || partnerPublicEncryptKey.isEmpty()) {
            Log.e("CRYPTO_ERROR", "Chưa tải được Public Key, không thể mã hóa tin nhắn!")
            return
        }

        _messageText.value = ""

        viewModelScope.launch {

            val encryptedJsonPayload = CryptoHelper.encryptMessage(
                context = context,
                plaintext = content,
                receiverPublicEncryptKeyBase64 = partnerPublicEncryptKey,
                myPublicEncryptKeyBase64 = myPublicEncryptKey
            )

            if (encryptedJsonPayload.isNotEmpty()) {
                messageRepository.sendMessage(
                    conversationId,
                    senderId = senderId,
                    content = encryptedJsonPayload,
                    replyToId = replyToId
                )
                Log.d("CRYPTO_SUCCESS", "Đã gửi gói tin mã hóa: $encryptedJsonPayload")
            } else {
                Log.e("CRYPTO_ERROR", "Lỗi trong quá trình mã hóa. Tin nhắn bị hủy.")
            }
        }
    }
    fun onMessageTextChanged(text: String) {
        _messageText.value = text
        val currentTime = System.currentTimeMillis()
        if (text.isNotEmpty() && (currentTime - lastTypingTime > 2000)) {
            lastTypingTime = currentTime
            viewModelScope.launch {
                messageRepository.sendTypingEvent(conversationId, _currentUserId.value)
            }
        }
    }
    fun addReaction(messageId: String, reactionIcon: String) {
        viewModelScope.launch {
            val userId = _currentUserId.value
            if (userId.isNotEmpty()) {
                messageRepository.addReaction(messageId, userId, reactionIcon)
            }
        }
    }
    fun sendImageMessage(
        conversationId: String,
        byteArray: ByteArray,
        fileName: String,
        fileSize: Int = 0
    ) {
        viewModelScope.launch {
            try {
                val imageUrl = attachmentRepository.uploadImage(byteArray, fileName)

                val fallbackText = "[Hình ảnh]"
                val encryptedJsonPayload = CryptoHelper.encryptMessage(
                    context = context,
                    plaintext = fallbackText,
                    receiverPublicEncryptKeyBase64 = partnerPublicEncryptKey,
                    myPublicEncryptKeyBase64 = myPublicEncryptKey
                )

                if (encryptedJsonPayload.isEmpty()) {
                    Log.e("CRYPTO_ERROR", "Lỗi mã hóa tin nhắn ảnh")
                    return@launch
                }

                val generatedMessageId = messageRepository.sendMessage(
                    conversationId = conversationId,
                    senderId = _currentUserId.value,
                    content = encryptedJsonPayload,
                    messageType = "IMAGE",
                    replyToId = null
                )



                attachmentRepository.sendAttachment(
                    messageId = generatedMessageId,
                    fileUrl = imageUrl,
                    fileType = "IMAGE",
                    fileName = fileName,
                    fileSize = fileSize
                )
                val newAttachment = Attachment(
                    id = "temp_${System.currentTimeMillis()}",
                    messageId = generatedMessageId,
                    fileUrl = imageUrl,
                    fileType = "IMAGE",
                    fileName = fileName,
                    fileSize = fileSize,
                    createdAt = ""
                )
                _messages.update { currentList ->
                    currentList.map { msg ->
                        if (msg.id == generatedMessageId) {
                            msg.copy(attachments = listOf(newAttachment))
                        } else msg
                    }
                }

                Log.d("UPLOAD_SUCCESS", "Đã gửi ảnh thành công!")

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("UPLOAD_ERROR", "Lỗi gửi ảnh: ${e.message}")
            }
        }
    }
    fun toggleEncryptionView() {
        _isShowingRawEncryption.value = !_isShowingRawEncryption.value
    }
}