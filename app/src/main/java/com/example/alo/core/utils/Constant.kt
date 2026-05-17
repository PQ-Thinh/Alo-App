package com.example.alo.core.utils

object Constant {
    // Intents Actions & Extras
    const val ACTION_INCOMING_CALL_ACCEPT = "com.example.alo.ACTION_INCOMING_CALL_ACCEPT"
    const val ACTION_INCOMING_CALL_DECLINE = "com.example.alo.ACTION_INCOMING_CALL_DECLINE"
    const val ACTION_START_OUTGOING = "com.example.alo.call.START_OUTGOING"
    const val ACTION_START_INCOMING = "com.example.alo.call.START_INCOMING"
    const val ACTION_CALL_CONNECTED = "com.example.alo.call.CALL_CONNECTED"
    const val ACTION_STOP = "com.example.alo.call.STOP"
    const val EXTRA_CALL_ID = "extra_call_id"
    const val EXTRA_PEER_NAME = "extra_peer_name"

    // Foreground Service & Call
    const val CALL_CHANNEL_ID = "alo_call_foreground"
    const val CALL_TIMEOUT_MS = 45_000L
    const val CALL_TYPE_DEFAULT = "default"

    // Crypto
    const val MASTER_KEY_URI = "android-keystore://alo_app_master_key"
    const val PREF_FILE_NAME = "alo_app_crypto_keys"
    const val ENCRYPT_KEYSET_NAME = "encrypt_keyset"
    const val SIGN_KEYSET_NAME = "sign_keyset"

    // Supabase Tables & Storage
    const val TABLE_USERS = "users"
    const val TABLE_USER_DEVICES = "user_devices"
    const val TABLE_SHARED_TASKS = "shared_tasks"
    const val TABLE_PARTICIPANTS = "participants"
    const val TABLE_MESSAGES = "messages"
    const val TABLE_MESSAGE_REACTIONS = "message_reactions"
    const val TABLE_FRIEND_REQUESTS = "friend_requests"
    const val TABLE_FRIENDS = "friends"
    const val TABLE_CONVERSATIONS = "conversations"
    const val TABLE_ATTACHMENTS = "attachments"
    const val TABLE_VIDEO_CALLS = "video_calls"
    const val VIEW_CHAT_LIST = "chat_list_view"
    
    const val STORAGE_AVATARS = "avatars"
}
