package com.example.alo.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SharedTask(
    @SerialName("id") val id: String = "",
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String? = null,
    @SerialName("assignee_id") val assigneeId: String? = null,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("priority") val priority: String = "medium",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("creator_id") val creatorId: String? = null
)
