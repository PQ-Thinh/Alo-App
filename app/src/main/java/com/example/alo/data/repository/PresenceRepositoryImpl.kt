package com.example.alo.data.repository

import com.example.alo.data.utils.UserPresence
import com.example.alo.domain.repository.PresenceRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.presenceDataFlow
import io.github.jan.supabase.realtime.track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject


class PresenceRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : PresenceRepository {

    private val channel = supabaseClient.channel("online-users")

    private val _onlineUsers = MutableStateFlow<Set<String>>(emptySet())
    override val onlineUsers: StateFlow<Set<String>> = _onlineUsers.asStateFlow()

    private var presenceJob: Job? = null

    override suspend fun subscribeAndTrack(currentUserId: String) {
        presenceJob?.cancel()

        channel.subscribe(blockUntilSubscribed = true)


        channel.track(UserPresence(userId = currentUserId, status = "online"))

        presenceJob = CoroutineScope(Dispatchers.IO).launch {

            channel.presenceDataFlow<UserPresence>().collect { presences ->

                val usersOnline = presences.map { it.userId }.toSet()
                _onlineUsers.value = usersOnline
            }
        }
    }

    override suspend fun unsubscribe() {
        presenceJob?.cancel()
        channel.unsubscribe()
        _onlineUsers.value = emptySet()
    }
}