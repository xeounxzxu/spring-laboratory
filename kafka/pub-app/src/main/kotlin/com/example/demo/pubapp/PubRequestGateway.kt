package com.example.demo.pubapp

import kotlinx.coroutines.withTimeout
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class PubRequestGateway(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val kafkaProperties: PubKafkaProperties,
    private val replyStore: ReplyStore,
) {
    suspend fun dispatch(requestId: String, message: String): String {
        val deferred = replyStore.register(requestId)
        return try {
            publish(requestId, message)
            withTimeout(kafkaProperties.replyTimeoutMs) { deferred.await() }
        } finally {
            replyStore.cancel(requestId)
        }
    }

    private fun publish(requestId: String, message: String) {
        val record = ProducerRecord<String, String>(kafkaProperties.requestTopic, message)
        record.headers().add(KafkaHeaders.REQUEST_ID, requestId.toByteArray())
        kafkaTemplate.send(record)
    }
}
