package com.example.demo.subapp

import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

@Component
class SubListener(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val kafkaProperties: SubKafkaProperties,
) {
    private val logger = LoggerFactory.getLogger(SubListener::class.java)

    @KafkaListener(topics = ["\${app.kafka.request-topic}"])
    fun onMessage(
        message: String,
        @Header(REQUEST_ID, required = false) requestIdBytes: ByteArray?,
        @Header(KafkaHeaders.CORRELATION_ID, required = false) correlationId: ByteArray?,
        @Header(KafkaHeaders.REPLY_TOPIC, required = false) replyTopicBytes: ByteArray?,
    ) {
        val requestId = requestIdBytes?.toString(Charsets.UTF_8)
        if (correlationId == null) {
            logger.warn("missing correlation_id header")
            return
        }

        val replyTopic = replyTopicBytes?.toString(Charsets.UTF_8) ?: kafkaProperties.replyTopic
        logger.info("received: {} (request_id={})", message, requestId)
        val replyRecord = ProducerRecord<String, String>(replyTopic, "processed: $message")
        replyRecord.headers().add(KafkaHeaders.CORRELATION_ID, correlationId)
        if (!requestId.isNullOrBlank()) {
            replyRecord.headers().add(REQUEST_ID, requestId.toByteArray())
        }
        kafkaTemplate.send(replyRecord)
    }

    companion object {
        const val REQUEST_ID = "request-id"
    }
}
