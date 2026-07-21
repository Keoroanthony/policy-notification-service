package com.tufin.notificationservice.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

/**
 * Request body for POST /api/v1/notifications.
 *
 * Fields mirror policy-rule-engine's EvaluationRequest so there is no
 * mapping layer between the two services. Uses @field: annotation target
 * so Jakarta Bean Validation inspects the backing field rather than the
 * constructor parameter — required for Kotlin data classes with @Valid.
 *
 * Jackson ignores unknown fields by default (Spring Boot auto-config), so
 * any extra fields sent by the rule engine (e.g. a future timestamp field)
 * are silently dropped rather than causing a 400.
 */
data class CreateNotificationRequest(

    @field:NotBlank(message = "subject must not be blank")
    val subject: String,

    @field:NotBlank(message = "resource must not be blank")
    val resource: String,

    @field:NotBlank(message = "action must not be blank")
    val action: String,

    @field:Pattern(regexp = "DENY", message = "decision must be DENY — this service only accepts DENY notifications")
    val decision: String,

    val matchedRuleId: String?,

    @field:NotBlank(message = "reason must not be blank")
    val reason: String
)
