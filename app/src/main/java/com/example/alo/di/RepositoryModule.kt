package com.example.alo.di

import com.example.alo.data.repository.AuthRepositoryImpl
import com.example.alo.data.repository.ChatRepositoryImpl
import com.example.alo.data.repository.FriendRepositoryImpl
import com.example.alo.data.repository.UserDeviceRepositoryImpl
import com.example.alo.data.repository.UserRepositoryImpl
import com.example.alo.domain.repository.AuthRepository
import com.example.alo.domain.repository.ChatRepository
import com.example.alo.domain.repository.FriendRepository
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
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository


    @Binds
    @Singleton
    abstract fun bindUserDeviceRepository(
        userDeviceRepositoryImpl: UserDeviceRepositoryImpl
    ): UserDeviceRepository
}