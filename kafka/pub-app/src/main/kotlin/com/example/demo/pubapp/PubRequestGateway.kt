package com.example.demo.pubapp

import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withTimeout
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

/**
 * Publishes requests and keeps track of the coroutine job that waits for a reply.
 */
@Component
class PubRequestGateway(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val kafkaProperties: PubKafkaProperties,
    private val replyStore: ReplyStore,
) {
    private val logger = LoggerFactory.getLogger(PubRequestGateway::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun dispatch(requestId: String, message: String) = scope.async {
        val deferred = registerRequest(requestId)
        publish(requestId, message)
        withTimeout(kafkaProperties.replyTimeoutMs) { deferred.await() }
    }.also { job ->
        job.invokeOnCompletion { cause ->
            if (cause != null) {
                replyStore.cancel(requestId)
                logger.debug("request {} canceled by {}", requestId, cause::class.java.simpleName)
            }
        }
    }

    private fun registerRequest(requestId: String): CompletableDeferred<String> = replyStore.register(requestId)

    private fun publish(requestId: String, message: String) {
        val record = ProducerRecord<String, String>(kafkaProperties.requestTopic, message)
        record.headers().add(KafkaHeaders.REQUEST_ID, requestId.toByteArray())
        kafkaTemplate.send(record)
    }

    @PreDestroy
    fun shutdown() {
        scope.cancel()
    }
}
