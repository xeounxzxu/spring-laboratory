package com.example.demo.pubapp

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate
import org.springframework.stereotype.Component

@Component
class PubRequestGateway(
    private val replyingKafkaTemplate: ReplyingKafkaTemplate<String, String, String>,
    private val kafkaProperties: PubKafkaProperties,
) {
    private val logger = LoggerFactory.getLogger(PubRequestGateway::class.java)

    suspend fun dispatch(requestId: String, message: String): String {
        return withContext(CoroutineName("kafka-request-$requestId")) {
            val record = ProducerRecord(kafkaProperties.requestTopic, requestId, message)
            record.headers().add(REQUEST_ID, requestId.toByteArray())
            logger.info("kafka send requestId={} topic={} {}", requestId, kafkaProperties.requestTopic, coroutineLogContext())
            val response = replyingKafkaTemplate.sendAndReceive(record).await()
            logger.info("kafka reply received requestId={} {}", requestId, coroutineLogContext())
            response.value()
        }
    }

    companion object {
        const val REQUEST_ID = "request-id"
    }
}
