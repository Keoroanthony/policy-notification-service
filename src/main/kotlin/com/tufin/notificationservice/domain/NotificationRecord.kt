package com.tufin.notificationservice.domain

import java.time.Instant

/**
 * Immutable domain record representing a stored DENY notification.
 *
 * Design decisions:
 *
 * - [receivedAt] uses [Instant] rather than [java.time.LocalDateTime]:
 *   Instant is timezone-agnostic and maps directly to a UTC point in time.
 *   Security audit records must be unambiguous — LocalDateTime carries no
 *   timezone context, creating risk in multi-region deployments.
 *   Consistent with policy-rule-engine which uses Instant in both
 *   Rule.createdAt and EvaluationHistoryEntry.timestamp.
 *
 * - [reason] is non-nullable [String]:
 *   Every DENY notification is an actionable security alert. A null reason
 *   produces an incomplete audit record that operators cannot act on.
 *   Callers must always provide a reason — e.g. "Rule 'Block SSH' matched"
 *   or "No matching rule — default deny". This is intentionally stricter
 *   than EvaluationResponse.matchedRuleName (nullable) because that field
 *   is engine output; reason here targets a human operator reading an alert.
 */
data class NotificationRecord(
    val id: String,
    val sourceIp: String,
    val destinationIp: String,
    val port: Int,
    val decision: String,
    val reason: String,
    val receivedAt: Instant
)
