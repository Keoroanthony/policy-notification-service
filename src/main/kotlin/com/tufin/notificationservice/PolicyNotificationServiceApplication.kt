package com.tufin.notificationservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PolicyNotificationServiceApplication

fun main(args: Array<String>) {
    runApplication<PolicyNotificationServiceApplication>(*args)
}
