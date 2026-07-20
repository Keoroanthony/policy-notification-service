package com.tufin.notificationservice.dto

import java.time.Instant

data class NotificationResponse(
    val id: String,
    val sourceIp: String,
    val destinationIp: String,
    val port: Int,
    val decision: String,
    val reason: String,
    val receivedAt: Instant
)
