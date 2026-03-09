package com.example.alo.di

import com.example.alo.data.repository.AttachmentRepositoryImpl
import com.example.alo.data.repository.AuthRepositoryImpl
import com.example.alo.data.repository.ConversationRepositoryImpl
import com.example.alo.data.repository.FriendRepositoryImpl
import com.example.alo.data.repository.MessageRepositoryImpl
import com.example.alo.data.repository.ParticipantRepositoryImpl
import com.example.alo.data.repository.UserDeviceRepositoryImpl
import com.example.alo.data.repository.UserRepositoryImpl
import com.example.alo.domain.repository.AttachmentRepository
import com.example.alo.domain.repository.AuthRepository
import com.example.alo.domain.repository.ConversationRepository
import com.example.alo.domain.repository.FriendRepository
import com.example.alo.domain.repository.MessageRepository
import com.example.alo.domain.repository.ParticipantRepository
import com.example.alo.domain.repository.UserDeviceRepository
import com.example.alo.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindFriendRepository(
        friendRepositoryImpl: FriendRepositoryImpl
    ): FriendRepository


    @Binds
    @Singleton
    abstract fun bindUserDeviceRepository(
        userDeviceRepositoryImpl: UserDeviceRepositoryImpl
    ): UserDeviceRepository
    @Binds
    @Singleton
    abstract fun bindConversationRepository(
        conversationRepositoryImpl: ConversationRepositoryImpl
    ): ConversationRepository

    @Binds
    @Singleton
    abstract fun bindMessageRepository(
        messageRepositoryImpl: MessageRepositoryImpl
    ): MessageRepository

    @Binds
    @Singleton
    abstract fun bindParticipantRepository(
        participantRepositoryImpl: ParticipantRepositoryImpl
    ): ParticipantRepository

    @Binds
    @Singleton
    abstract fun bindAttachmentRepository(
        attachmentRepositoryImpl: AttachmentRepositoryImpl
    ): AttachmentRepository

}