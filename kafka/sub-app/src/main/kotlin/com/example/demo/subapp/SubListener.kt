package com.example.demo.subapp

import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.Base64

@RestController
class SubListener(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val kafkaProperties: SubKafkaProperties,
) {
    private val logger = LoggerFactory.getLogger(SubListener::class.java)

    @PostMapping("/sub/process", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun process(@RequestBody request: SubProcessRequest): ResponseEntity<String> {
        if (request.message.isBlank()) {
            return ResponseEntity.badRequest().body("message is required")
        }
        if (request.correlationIdBase64.isBlank()) {
            return ResponseEntity.badRequest().body("correlationIdBase64 is required")
        }
        val correlationId = try {
            Base64.getDecoder().decode(request.correlationIdBase64)
        } catch (ex: IllegalArgumentException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("invalid correlationIdBase64")
        }

        val replyTopic = request.replyTopic ?: kafkaProperties.replyTopic
        logger.info("api received: {} (request_id={}) {}", request.message, request.requestId, threadLogContext())
        val replyRecord = ProducerRecord<String, String>(replyTopic, "processed: ${request.message}")
        replyRecord.headers().add(KafkaHeaders.CORRELATION_ID, correlationId)
        if (!request.requestId.isNullOrBlank()) {
            replyRecord.headers().add(REQUEST_ID, request.requestId.toByteArray())
        }
        logger.info("reply send request_id={} topic={} {}", request.requestId, replyTopic, threadLogContext())
        kafkaTemplate.send(replyRecord)
        return ResponseEntity.ok("accepted")
    }

    companion object {
        const val REQUEST_ID = "request-id"
    }

    private fun threadLogContext(): String = "thread=${Thread.currentThread().name}"
}

data class SubProcessRequest(
    val message: String,
    val correlationIdBase64: String,
    val requestId: String? = null,
    val replyTopic: String? = null,
)
