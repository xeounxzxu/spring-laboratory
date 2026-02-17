package com.example.demo.pubapp

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * 클라이언트가 호출하는 동기형 진입점입니다.
 * 내부 처리는 Kafka 기반 비동기로 진행되지만, HTTP 응답은 한 번에 반환합니다.
 */
@RestController
class PubController(
    private val requestGateway: PubRequestGateway,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(PubController::class.java)

    @PostMapping("/kafka/publish", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun publish(@RequestBody request: PublishRequest): ResponseEntity<Any> {
        if (request.requestId.isBlank()) {
            return ResponseEntity.badRequest().body("request_id is required")
        }
        if (request.payload == null && request.message.isNullOrBlank()) {
            return ResponseEntity.badRequest().body("payload or message is required")
        }
        val envelope = toEnvelope(request)
        logger.info(
            "publish request received requestId={} type={} version={} {}",
            envelope.requestId,
            envelope.type,
            envelope.version,
            coroutineLogContext(),
        )
        return try {
            val job = withContext(CoroutineName("http-publish-${request.requestId}")) {
                requestGateway.dispatch(envelope)
            }
            val reply = job.await()
            logger.info(
                "publish request completed requestId={} {}",
                request.requestId,
                coroutineLogContext(),
            )
            ResponseEntity.ok(reply)
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body("duplicate request_id")
        } catch (ex: TimeoutCancellationException) {
            logger.warn(
                "publish request timeout requestId={} {}",
                request.requestId,
                coroutineLogContext(),
            )
            ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body("timeout")
        }
    }

    private fun toEnvelope(request: PublishRequest): MessageEnvelope {
        val payload = request.payload ?: objectMapper.createObjectNode().put("message", request.message ?: "")
        return MessageEnvelope(
            requestId = request.requestId,
            type = request.type ?: "generic",
            version = request.version ?: 1,
            payload = payload,
        )
    }
}
