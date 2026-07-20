package com.tufin.notificationservice.exception

import org.springframework.http.HttpStatus
import java.time.Instant

data class ErrorResponse(
    val timestamp: String,
    val status: Int,
    val error: String,
    val message: String,
    val path: String
) {
    companion object {
        fun of(httpStatus: HttpStatus, message: String, path: String) = ErrorResponse(
            timestamp = Instant.now().toString(),
            status = httpStatus.value(),
            error = httpStatus.reasonPhrase,
            message = message,
            path = path
        )
    }
}
