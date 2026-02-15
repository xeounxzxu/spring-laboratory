package com.example.demo.subapp

import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
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
        @Header(KafkaHeaders.REQUEST_ID) requestIdBytes: ByteArray?,
    ) {
        val requestId = requestIdBytes?.toString(Charsets.UTF_8)
        if (requestId.isNullOrBlank()) {
            logger.warn("missing request_id header")
            return
        }
        logger.info("received: {} (request_id={})", message, requestId)
        val replyRecord = ProducerRecord<String, String>(kafkaProperties.replyTopic, "processed: $message")
        replyRecord.headers().add(KafkaHeaders.REQUEST_ID, requestId.toByteArray())
        kafkaTemplate.send(replyRecord)
    }
}
