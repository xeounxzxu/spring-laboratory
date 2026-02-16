package com.example.demo.pubapp

import kotlinx.coroutines.future.await
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate
import org.springframework.stereotype.Component

@Component
class PubRequestGateway(
    private val replyingKafkaTemplate: ReplyingKafkaTemplate<String, String, String>,
    private val kafkaProperties: PubKafkaProperties,
) {
    suspend fun dispatch(requestId: String, message: String): String {
        val record = ProducerRecord(kafkaProperties.requestTopic, requestId, message)
        record.headers().add(REQUEST_ID, requestId.toByteArray())
        val response = replyingKafkaTemplate.sendAndReceive(record).await()
        return response.value()
    }

    companion object {
        const val REQUEST_ID = "request-id"
    }
}
