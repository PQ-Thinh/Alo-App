package com.example.alo.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alo.data.utils.CryptoHelper
import com.example.alo.domain.model.Message
import com.example.alo.domain.repository.AttachmentRepository
import com.example.alo.domain.repository.AuthRepository
import com.example.alo.domain.repository.ConversationRepository
import com.example.alo.domain.repository.FriendRepository
import com.example.alo.domain.repository.MessageRepository
import com.example.alo.domain.repository.ParticipantRepository
import com.example.alo.domain.repository.UserRepository
import com.google.crypto.tink.KeysetHandle
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
    private val attachmentRepository: AttachmentRepository,
    private val friendRepository: FriendRepository,
    private val participantRepository: ParticipantRepository
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

    private val _uploadingMessageIds = MutableStateFlow<Set<String>>(emptySet())
    val uploadingMessageIds: StateFlow<Set<String>> = _uploadingMessageIds.asStateFlow()

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

    private val _isFriend = MutableStateFlow(true)
    val isFriend: StateFlow<Boolean> = _isFriend.asStateFlow()

    private val _isGroup = MutableStateFlow(false)
    val isGroup: StateFlow<Boolean> = _isGroup.asStateFlow()

    private val _memberNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val memberNames: StateFlow<Map<String, String>> = _memberNames.asStateFlow()


    // cache save publicEncryptKey
    private var myPublicEncryptKey: String = ""
    private var partnerPublicEncryptKey: String = ""
    private var partnerPublicSignKey: String? = null
    
    private var groupKeysetHandle: KeysetHandle? = null

    private var typingJob: Job? = null
    private var lastTypingTime = 0L
    private var partnerStatusJob: Job? = null

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
                        _isGroup.value = currentChatInfo.isGroup
                        _partnerName.value = currentChatInfo.chatName ?: "Hội thoại"
                        _partnerAvatar.value = currentChatInfo.chatAvatar ?: ""

                        if (currentChatInfo.isGroup) {
                            // Logic Group: Lấy Group Key và danh sách thành viên
                            val myParticipant = participantRepository.getParticipant(conversationId, user.id)
                            myParticipant?.encryptedGroupKey?.let { wrappedKey ->
                                Log.d("ChatRoomVM", "Đang giải mã Group Key cho conversation: $conversationId")
                                val groupKeysetBase64 = CryptoHelper.unwrapGroupKey(context, wrappedKey)
                                if (groupKeysetBase64.isNotEmpty()) {
                                    Log.d("ChatRoomVM", "Giải mã Group Key THÀNH CÔNG")
                                    groupKeysetHandle = CryptoHelper.importKeysetFromBase64(groupKeysetBase64)
                                } else {
                                    Log.e("ChatRoomVM", "Giải mã Group Key THẤT BẠI (chuỗi rỗng)")
                                }
                            }
                            
                            val participants = participantRepository.getParticipants(conversationId)
                            val nameMap = mutableMapOf<String, String>()
                            participants.forEach { p ->
                                val profile = userRepository.getCurrentUser(p.userId)
                                nameMap[p.userId] = profile?.displayName ?: "Thành viên"
                            }
                            _memberNames.value = nameMap
                            
                        } else {
                            // Logic 1-1
                            val pId = currentChatInfo.targetUserId ?: ""
                            _partnerId.value = pId
                            _partnerLastSeen.value = currentChatInfo.targetLastSeen ?: ""

                            if (pId.isNotEmpty()) {
                                val partnerProfile = userRepository.getCurrentUser(pId)
                                partnerPublicEncryptKey = partnerProfile?.publicEncryptKey ?: ""

                                val status = friendRepository.checkFriendStatus(user.id, pId)
                                _isFriend.value = (status == "friends")
                                partnerPublicSignKey = partnerProfile?.publicSignKey
                                observePartnerStatus(pId)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ChatRoomVM", "Error init: ${e.message}")
                }
            }

            val historyMessages = messageRepository.getMessages(conversationId)
            val decryptedHistory = historyMessages.map { msg ->
                decryptMessageObj(msg)
            }
            _messages.value = decryptedHistory

            historyMessages.forEach { msg ->
                if (msg.senderId != user?.id && !msg.seenBy.contains(user?.id)) {
                    viewModelScope.launch {
                        user?.id?.let { uid -> messageRepository.markMessageAsSeen(msg.id, uid) }
                    }
                }
            }
            messageRepository.subscribeToNewMessages(conversationId, onTyping = { typingUserId ->
                    if (typingUserId != _currentUserId.value) {
                        typingJob?.cancel()
                        typingJob = viewModelScope.launch {
                            _isPartnerTyping.value = true
                            delay(3000) 
                            _isPartnerTyping.value = false
                        }
                    }
                }
                ).collect { newMessage ->
                val decryptedNewMsg = decryptMessageObj(newMessage)

                if (decryptedNewMsg.senderId != user?.id && !decryptedNewMsg.seenBy.contains(user?.id)) {
                    viewModelScope.launch {
                        messageRepository.markMessageAsSeen(decryptedNewMsg.id, user!!.id)
                        conversationRepository.resetUnreadCount(conversationId, user.id)
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
    private fun observePartnerStatus(targetUserId: String) {
        partnerStatusJob?.cancel()
        partnerStatusJob = viewModelScope.launch {
            userRepository.observeUserStatus(targetUserId).collect { newLastSeen ->
                if (newLastSeen != null) {
                    _partnerLastSeen.value = newLastSeen
                }
            }
        }
    }

    private fun decryptMessageObj(msg: Message): Message {
        if (_isGroup.value) {
            val handle = groupKeysetHandle
            return if (handle != null) {
                val clearText = CryptoHelper.decryptGroupMessage(msg.encryptedContent, handle)
                msg.copy(encryptedContent = clearText, rawEncryptedContent = msg.encryptedContent)
            } else {
                msg.copy(encryptedContent = "🔒 Lỗi giải mã nhóm")
            }
        }

        val isMine = (msg.senderId == _currentUserId.value)
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

        _messageText.value = ""

        viewModelScope.launch {
            val encryptedJsonPayload = if (_isGroup.value) {
                val handle = groupKeysetHandle
                if (handle != null) {
                    CryptoHelper.encryptGroupMessage(content, handle)
                } else {
                    Log.e("CRYPTO_ERROR", "Chưa tải được Group Key!")
                    return@launch
                }
            } else {
                if (myPublicEncryptKey.isEmpty() || partnerPublicEncryptKey.isEmpty()) {
                    Log.e("CRYPTO_ERROR", "Chưa tải được Public Key!")
                    return@launch
                }
                CryptoHelper.encryptMessage(
                    context = context,
                    plaintext = content,
                    receiverPublicEncryptKeyBase64 = partnerPublicEncryptKey,
                    myPublicEncryptKeyBase64 = myPublicEncryptKey
                )
            }

            if (encryptedJsonPayload.isNotEmpty()) {
                messageRepository.sendMessage(
                    conversationId,
                    senderId = senderId,
                    content = encryptedJsonPayload,
                    replyToId = replyToId
                )
            } else {
                Log.e("CRYPTO_ERROR", "Lỗi mã hóa tin nhắn.")
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
    fun sendMediaMessage(
        conversationId: String,
        byteArray: ByteArray,
        fileName: String,
        fileSize: Int = 0,
        isImage: Boolean
    ) {
        viewModelScope.launch {
            try {
                val fileUrl = if (isImage) {
                    attachmentRepository.uploadImage(byteArray, fileName)
                } else {
                    attachmentRepository.uploadDocument(byteArray, fileName)
                }

                val messageType = if (isImage) "IMAGE" else "FILE"
                val fallbackText = if (isImage) "[Hình ảnh]" else "[Tệp đính kèm] $fileName"

                val encryptedJsonPayload = if (_isGroup.value) {
                    val handle = groupKeysetHandle
                    if (handle != null) {
                        CryptoHelper.encryptGroupMessage(fallbackText, handle)
                    } else ""
                } else {
                    CryptoHelper.encryptMessage(
                        context = context,
                        plaintext = fallbackText,
                        receiverPublicEncryptKeyBase64 = partnerPublicEncryptKey,
                        myPublicEncryptKeyBase64 = myPublicEncryptKey
                    )
                }

                if (encryptedJsonPayload.isEmpty()) return@launch

                val generatedMessageId = messageRepository.sendMessage(
                    conversationId = conversationId,
                    senderId = _currentUserId.value,
                    content = encryptedJsonPayload,
                    messageType = messageType,
                    replyToId = null
                )

                attachmentRepository.sendAttachment(
                    messageId = generatedMessageId,
                    fileUrl = fileUrl,
                    fileType = messageType,
                    fileName = fileName,
                    fileSize = fileSize
                )

                Log.d("UPLOAD_SUCCESS", "Đã gửi $messageType thành công!")

            } catch (e: Exception) {
                Log.e("UPLOAD_ERROR", "Lỗi gửi Media: ${e.message}")
            }
        }
    }

    fun toggleEncryptionView() {
        _isShowingRawEncryption.value = !_isShowingRawEncryption.value
    }
}