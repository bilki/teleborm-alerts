package com.lambdarat.teleborm.domain.model

import java.time.LocalDateTime

final case class UserState(
    userId: Long,
    messageId: Int,
    convState: ConversationState,
    created: LocalDateTime
)
