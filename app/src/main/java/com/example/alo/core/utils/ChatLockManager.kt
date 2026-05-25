package com.example.alo.core.utils

/**
 * Singleton to manage unlocked chat rooms in memory.
 * Unlocked state persists until the app is killed.
 */
object ChatLockManager {
    private val unlockedChatIds = mutableSetOf<String>()

    fun unlockChat(conversationId: String) {
        unlockedChatIds.add(conversationId)
    }

    fun isChatUnlocked(conversationId: String): Boolean {
        return unlockedChatIds.contains(conversationId)
    }

    fun lockChat(conversationId: String) {
        unlockedChatIds.remove(conversationId)
    }

    fun clearAll() {
        unlockedChatIds.clear()
    }
}
