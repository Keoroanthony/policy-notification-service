package com.tufin.notificationservice.service

import com.tufin.notificationservice.domain.NotificationRecord
import com.tufin.notificationservice.dto.CreateNotificationRequest
import com.tufin.notificationservice.dto.NotificationResponse
import com.tufin.notificationservice.dto.NotificationSummaryResponse
import com.tufin.notificationservice.repository.InMemoryNotificationRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class NotificationService(private val repository: InMemoryNotificationRepository) {

    fun createNotification(request: CreateNotificationRequest): NotificationResponse {
        val record = NotificationRecord(
            id = UUID.randomUUID().toString(),
            subject = request.subject,
            resource = request.resource,
            action = request.action,
            decision = request.decision,
            matchedRuleId = request.matchedRuleId,
            reason = request.reason,
            receivedAt = Instant.now()
        )
        return repository.save(record).toResponse()
    }

    fun getAllNotifications(): List<NotificationResponse> =
        repository.findAll().map { it.toResponse() }

    fun getSummary(): NotificationSummaryResponse {
        val all = repository.findAll()
        val topSubject = all
            .groupBy { it.subject }
            .maxByOrNull { it.value.size }
            ?.key
        return NotificationSummaryResponse(total = all.size, topSubject = topSubject)
    }

    private fun NotificationRecord.toResponse() = NotificationResponse(
        id = id,
        subject = subject,
        resource = resource,
        action = action,
        decision = decision,
        matchedRuleId = matchedRuleId,
        reason = reason,
        receivedAt = receivedAt
    )
}
