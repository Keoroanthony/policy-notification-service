package com.tufin.notificationservice.repository

import com.tufin.notificationservice.domain.NotificationRecord
import org.springframework.stereotype.Repository
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Thread-safe in-memory store for [NotificationRecord] entries.
 *
 * Uses [CopyOnWriteArrayList] so that concurrent reads (e.g. GET /notifications)
 * never block concurrent writes (e.g. POST /notifications).
 * This mirrors the thread-safety approach used in the policy-rule-engine's
 * InMemoryRuleRepository.
 */
@Repository
class InMemoryNotificationRepository {

    private val store: MutableList<NotificationRecord> = CopyOnWriteArrayList()

    fun save(record: NotificationRecord): NotificationRecord {
        store.add(record)
        return record
    }

    fun findAll(): List<NotificationRecord> = store.toList()

    fun findById(id: String): NotificationRecord? = store.find { it.id == id }

    fun count(): Int = store.size
}
