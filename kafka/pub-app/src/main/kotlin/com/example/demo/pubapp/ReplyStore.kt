package com.example.demo.pubapp

import kotlinx.coroutines.CompletableDeferred
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class ReplyStore {
    private val pending = ConcurrentHashMap<String, CompletableDeferred<String>>()

    fun register(requestId: String): CompletableDeferred<String> {
        val deferred = CompletableDeferred<String>()
        val existing = pending.putIfAbsent(requestId, deferred)
        if (existing != null) {
            throw IllegalArgumentException("request_id already exists")
        }
        return deferred
    }

    fun complete(requestId: String, message: String): Boolean {
        val deferred = pending.remove(requestId) ?: return false
        deferred.complete(message)
        return true
    }

    fun cancel(requestId: String): Boolean {
        val deferred = pending.remove(requestId) ?: return false
        deferred.cancel()
        return true
    }
}
