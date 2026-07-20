package com.tufin.notificationservice.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

/**
 * Request body for POST /api/v1/notifications.
 *
 * Uses @field: annotation target so that Jakarta Bean Validation inspects
 * the backing field rather than the constructor parameter — required for
 * Kotlin data classes to work correctly with Spring's @Valid processing.
 */
data class CreateNotificationRequest(

    @field:NotBlank(message = "sourceIp must not be blank")
    @field:Pattern(
        regexp = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$",
        message = "sourceIp must be a valid IPv4 address"
    )
    val sourceIp: String,

    @field:NotBlank(message = "destinationIp must not be blank")
    @field:Pattern(
        regexp = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$",
        message = "destinationIp must be a valid IPv4 address"
    )
    val destinationIp: String,

    @field:Min(value = 1, message = "port must be between 1 and 65535")
    @field:Max(value = 65535, message = "port must be between 1 and 65535")
    val port: Int,

    @field:Pattern(regexp = "DENY", message = "decision must be DENY — this service only accepts DENY notifications")
    val decision: String,

    @field:NotBlank(message = "reason must not be blank")
    val reason: String
)
