package com.example.demo.pubapp

import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

@Component
class PubReplyListener(
    private val replyStore: ReplyStore,
) {
    private val logger = LoggerFactory.getLogger(PubReplyListener::class.java)

    @KafkaListener(topics = ["\${app.kafka.reply-topic}"])
    fun onReply(
        message: String,
        @Header(KafkaHeaders.REQUEST_ID) requestIdBytes: ByteArray?,
    ) {
        val requestId = requestIdBytes?.toString(Charsets.UTF_8)
        if (requestId.isNullOrBlank()) {
            logger.warn("missing request_id header on reply")
            return
        }
        val delivered = replyStore.complete(requestId, message)
        if (!delivered) {
            logger.debug("no pending request for request_id={}", requestId)
        }
    }
}
