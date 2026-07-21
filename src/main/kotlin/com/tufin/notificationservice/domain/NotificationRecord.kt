package com.tufin.notificationservice.domain

import java.time.Instant

/**
 * Immutable domain record representing a stored DENY notification.
 *
 * Field model: uses policy-rule-engine's evaluation model (subject/resource/action)
 * rather than a network-firewall model (sourceIp/destinationIp/port). The
 * rule engine evaluates abstract policy requests, not raw IP packets, so the
 * notification contract must match what the engine actually produces.
 *
 * - [subject]        the entity making the request (e.g. "GUEST", "USER")
 * - [resource]       the resource being accessed (e.g. "/admin/users")
 * - [action]         the operation attempted (e.g. "READ", "WRITE")
 * - [matchedRuleId]  nullable — null when the denial is the default (no rule matched)
 * - [reason]         non-nullable human-readable explanation; every DENY must be
 *                    explainable. See reason convention in README.
 * - [receivedAt]     [Instant] (not LocalDateTime): timezone-agnostic UTC point in
 *                    time, consistent with Rule.createdAt and EvaluationHistoryEntry
 *                    in policy-rule-engine.
 */
data class NotificationRecord(
    val id: String,
    val subject: String,
    val resource: String,
    val action: String,
    val decision: String,
    val matchedRuleId: String?,
    val reason: String,
    val receivedAt: Instant
)
