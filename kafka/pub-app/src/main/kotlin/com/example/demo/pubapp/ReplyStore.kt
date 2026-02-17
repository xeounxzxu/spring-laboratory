package com.example.demo.pubapp

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class ReplyStore {
    private val pending = ConcurrentHashMap.newKeySet<String>()
    private val responses = ConcurrentHashMap<String, String>()

    fun register(requestId: String) {
        val added = pending.add(requestId)
        if (!added) {
            throw IllegalArgumentException("request_id already exists")
        }
    }

    fun complete(requestId: String, message: String): Boolean {
        if (!pending.contains(requestId)) {
            return false
        }
        responses[requestId] = message
        return true
    }

    fun poll(requestId: String): String? = responses.remove(requestId)

    fun clear(requestId: String) {
        pending.remove(requestId)
        responses.remove(requestId)
    }
}
