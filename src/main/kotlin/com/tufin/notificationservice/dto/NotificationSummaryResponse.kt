package com.tufin.notificationservice.dto

/**
 * Response for GET /api/v1/notifications/summary.
 *
 * - [total]      total number of stored DENY notifications
 * - [topSubject] the subject that has triggered the most DENY decisions;
 *                null when there are no notifications yet.
 *                Tie-breaking: when two subjects share the same count,
 *                the one that appears first in insertion order is returned.
 */
data class NotificationSummaryResponse(
    val total: Int,
    val topSubject: String?
)
