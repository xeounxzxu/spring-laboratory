package com.example.demo.pubapp

import com.fasterxml.jackson.databind.ObjectMapper
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

/**
 * pub-app을 non-blocking으로 유지하기 위한 핵심 게이트웨이입니다.
 * - 요청 메시지를 Kafka로 송신
 * - 코루틴 Job에서 suspend 상태로 대기
 * - 메모리 ReplyStore를 폴링하다가 응답 수신 또는 타임아웃 처리
 */
@Component
class PubRequestGateway(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val kafkaProperties: PubKafkaProperties,
    private val replyStore: ReplyStore,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(PubRequestGateway::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun dispatch(envelope: MessageEnvelope) = scope.async(CoroutineName("kafka-request-${envelope.requestId}")) {
        withContext(CoroutineName("kafka-request-${envelope.requestId}")) {
            replyStore.register(envelope.requestId)
            try {
                publish(envelope)
                waitByPolling(envelope.requestId)
            } finally {
                replyStore.clear(envelope.requestId)
            }
        }
    }

    private fun publish(envelope: MessageEnvelope) {
        val record = ProducerRecord(kafkaProperties.requestTopic, envelope.requestId, objectMapper.writeValueAsString(envelope))
        record.headers().add(REQUEST_ID, envelope.requestId.toByteArray())
        record.headers().add(KafkaHeaders.CORRELATION_ID, envelope.requestId.toByteArray())
        record.headers().add(KafkaHeaders.REPLY_TOPIC, kafkaProperties.replyTopic.toByteArray())
        logger.info("kafka send requestId={} topic={} {}", envelope.requestId, kafkaProperties.requestTopic, "thread=${Thread.currentThread().name}")
        kafkaTemplate.send(record)
    }

    private suspend fun waitByPolling(requestId: String): MessageEnvelope {
        return withTimeout<MessageEnvelope>(kafkaProperties.replyTimeoutMs) {
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
