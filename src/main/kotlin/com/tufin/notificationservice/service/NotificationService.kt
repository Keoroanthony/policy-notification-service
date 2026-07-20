package com.tufin.notificationservice.service

import com.tufin.notificationservice.domain.NotificationRecord
import com.tufin.notificationservice.dto.CreateNotificationRequest
import com.tufin.notificationservice.dto.NotificationResponse
import com.tufin.notificationservice.repository.InMemoryNotificationRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class NotificationService(private val repository: InMemoryNotificationRepository) {

    fun createNotification(request: CreateNotificationRequest): NotificationResponse {
        val record = NotificationRecord(
            id = UUID.randomUUID().toString(),
            sourceIp = request.sourceIp,
            destinationIp = request.destinationIp,
            port = request.port,
            decision = request.decision,
            reason = request.reason,
            receivedAt = Instant.now()
        )
        return repository.save(record).toResponse()
    }

    fun getAllNotifications(): List<NotificationResponse> =
        repository.findAll().map { it.toResponse() }

    private fun NotificationRecord.toResponse() = NotificationResponse(
        id = id,
        sourceIp = sourceIp,
        destinationIp = destinationIp,
        port = port,
        decision = decision,
        reason = reason,
        receivedAt = receivedAt
    )
}
