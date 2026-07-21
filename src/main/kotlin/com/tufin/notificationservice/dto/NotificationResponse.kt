package com.tufin.notificationservice.dto

import java.time.Instant

data class NotificationResponse(
    val id: String,
    val subject: String,
    val resource: String,
    val action: String,
    val decision: String,
    val matchedRuleId: String?,
    val reason: String,
    val receivedAt: Instant
)
