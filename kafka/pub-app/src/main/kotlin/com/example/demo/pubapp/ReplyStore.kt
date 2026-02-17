package com.example.demo.pubapp

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * 단일 pub-app 인스턴스에서 사용하는 로컬 request-reply 저장소입니다.
 * requestId 유니크를 강제해 대기 중 응답 매칭 충돌을 방지합니다.
 */
@Component
class ReplyStore {
    private val pending = ConcurrentHashMap.newKeySet<String>()
    private val responses = ConcurrentHashMap<String, MessageEnvelope>()

    fun register(requestId: String) {
        val added = pending.add(requestId)
        if (!added) {
            throw IllegalArgumentException("request_id already exists")
        }
    }

    fun complete(requestId: String, message: MessageEnvelope): Boolean {
        if (!pending.contains(requestId)) {
            return false
        }
        responses[requestId] = message
        return true
    }

    fun poll(requestId: String): MessageEnvelope? = responses.remove(requestId)

    fun clear(requestId: String) {
        pending.remove(requestId)
        responses.remove(requestId)
    }
}
