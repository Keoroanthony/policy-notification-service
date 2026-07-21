package com.tufin.notificationservice.controller

import com.tufin.notificationservice.dto.CreateNotificationRequest
import com.tufin.notificationservice.dto.NotificationResponse
import com.tufin.notificationservice.dto.NotificationSummaryResponse
import com.tufin.notificationservice.service.NotificationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(private val notificationService: NotificationService) {

    @PostMapping
    fun createNotification(
        @Valid @RequestBody request: CreateNotificationRequest
    ): ResponseEntity<NotificationResponse> =
        ResponseEntity
            .status(HttpStatus.CREATED)
            .body(notificationService.createNotification(request))

    @GetMapping
    fun getAllNotifications(): ResponseEntity<List<NotificationResponse>> =
        ResponseEntity.ok(notificationService.getAllNotifications())

    @GetMapping("/summary")
    fun getSummary(): ResponseEntity<NotificationSummaryResponse> =
        ResponseEntity.ok(notificationService.getSummary())
}
