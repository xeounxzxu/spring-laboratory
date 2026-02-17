package com.example.demo.pubapp

import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

/**
 * Kafka consume 스레드와 HTTP 요청 스레드를 분리하기 위해
 * 수신한 reply를 먼저 메모리에 버퍼링합니다.
 */
@Component
class PubReplyListener(
    private val replyStore: ReplyStore,
) {
    private val logger = LoggerFactory.getLogger(PubReplyListener::class.java)

    @KafkaListener(
        topics = ["\${app.kafka.reply-topic}"],
        groupId = "\${app.kafka.reply-group-id}",
    )
    fun onReply(
        message: String,
        @Header(PubRequestGateway.REQUEST_ID, required = false) requestIdBytes: ByteArray?,
        @Header(KafkaHeaders.CORRELATION_ID, required = false) correlationId: ByteArray?,
    ) {
        val requestId = requestIdBytes?.toString(Charsets.UTF_8) ?: correlationId?.toString(Charsets.UTF_8)
        if (requestId.isNullOrBlank()) {
            logger.warn("missing request-id and correlation-id on reply thread={}", Thread.currentThread().name)
            return
        }
        val delivered = replyStore.complete(requestId, message)
        if (!delivered) {
            logger.debug("reply ignored requestId={} thread={}", requestId, Thread.currentThread().name)
        } else {
            logger.info("reply buffered requestId={} thread={}", requestId, Thread.currentThread().name)
        }
    }
}
