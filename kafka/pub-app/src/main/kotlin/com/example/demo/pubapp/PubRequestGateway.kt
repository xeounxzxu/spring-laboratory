package com.example.demo.pubapp

import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.stereotype.Component

@Component
class PubRequestGateway(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val kafkaProperties: PubKafkaProperties,
    private val replyStore: ReplyStore,
) {
    private val logger = LoggerFactory.getLogger(PubRequestGateway::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun dispatch(requestId: String, message: String) = scope.async(CoroutineName("kafka-request-$requestId")) {
        withContext(CoroutineName("kafka-request-$requestId")) {
            replyStore.register(requestId)
            try {
                publish(requestId, message)
                waitByPolling(requestId)
            } finally {
                replyStore.clear(requestId)
            }
        }
    }

    private fun publish(requestId: String, message: String) {
        val record = ProducerRecord(kafkaProperties.requestTopic, requestId, message)
        record.headers().add(REQUEST_ID, requestId.toByteArray())
        record.headers().add(KafkaHeaders.CORRELATION_ID, requestId.toByteArray())
        record.headers().add(KafkaHeaders.REPLY_TOPIC, kafkaProperties.replyTopic.toByteArray())
        logger.info("kafka send requestId={} topic={} {}", requestId, kafkaProperties.requestTopic, "thread=${Thread.currentThread().name}")
        kafkaTemplate.send(record)
    }

    private suspend fun waitByPolling(requestId: String): String {
        return withTimeout<String>(kafkaProperties.replyTimeoutMs) {
            var reply = replyStore.poll(requestId)
            while (reply == null) {
                delay(kafkaProperties.pollIntervalMs)
                reply = replyStore.poll(requestId)
            }
            logger.info("kafka reply received requestId={} {}", requestId, coroutineLogContext())
            reply
        }
    }

    @PreDestroy
    fun shutdown() {
        scope.cancel()
    }

    companion object {
        const val REQUEST_ID = "request-id"
    }
}
