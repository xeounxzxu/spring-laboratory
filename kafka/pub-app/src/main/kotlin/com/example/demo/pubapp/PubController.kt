package com.example.demo.pubapp

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.kafka.requestreply.KafkaReplyTimeoutException
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class PubController(
    private val requestGateway: PubRequestGateway,
) {
    private val logger = LoggerFactory.getLogger(PubController::class.java)

    @PostMapping("/kafka/publish", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.TEXT_PLAIN_VALUE])
    suspend fun publish(@RequestBody request: PublishRequest): ResponseEntity<String> {
        if (request.requestId.isBlank()) {
            return ResponseEntity.badRequest().body("request_id is required")
        }
        logger.info(
            "publish request received requestId={} {}",
            request.requestId,
            coroutineLogContext(),
        )
        return try {
            val reply = withContext(CoroutineName("http-publish-${request.requestId}")) {
                requestGateway.dispatch(request.requestId, request.message)
            }
            logger.info(
                "publish request completed requestId={} {}",
                request.requestId,
                coroutineLogContext(),
            )
            ResponseEntity.ok(reply)
        } catch (ex: KafkaReplyTimeoutException) {
            logger.warn(
                "publish request timeout requestId={} {}",
                request.requestId,
                coroutineLogContext(),
            )
            ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body("timeout")
        }
    }
}
