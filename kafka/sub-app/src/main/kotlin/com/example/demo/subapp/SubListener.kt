package com.example.demo.subapp

import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * 개발자가 직접 호출하는 수신 API를 제공한다.
 * 전달받은 요청을 Kafka reply 토픽으로 publish 하여 pub-app 응답 흐름을 완료시킨다.
 */
@RestController
class SubListener(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val kafkaProperties: SubKafkaProperties,
) {
    private val logger = LoggerFactory.getLogger(SubListener::class.java)

    @PostMapping("/sub/process", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun process(@RequestBody request: SubProcessRequest): ResponseEntity<String> {
        // pub-app 대기 작업과 매칭하기 위한 키이므로 필수값이다.
        if (request.requestId.isBlank()) {
            return ResponseEntity.badRequest().body("requestId is required")
        }
        // 실제 처리 대상 메시지 본문 검증
        if (request.message.isBlank()) {
            return ResponseEntity.badRequest().body("message is required")
        }

        // replyTopic 미지정 시 기본 reply 토픽으로 보낸다.
        val replyTopic = request.replyTopic ?: kafkaProperties.replyTopic
        logger.info("api received: {} (request_id={}) {}", request.message, request.requestId, threadLogContext())

        // pub-app이 기다리는 포맷으로 reply 이벤트를 구성한다.
        val replyRecord = ProducerRecord<String, String>(replyTopic, "processed: ${request.message}")
        replyRecord.headers().add(KafkaHeaders.CORRELATION_ID, request.requestId.toByteArray())
        replyRecord.headers().add(REQUEST_ID, request.requestId.toByteArray())
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
    val requestId: String,
    val message: String,
    val replyTopic: String? = null,
)
