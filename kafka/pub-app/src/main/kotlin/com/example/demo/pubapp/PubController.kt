package com.example.demo.pubapp

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
    @PostMapping("/kafka/publish", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.TEXT_PLAIN_VALUE])
    suspend fun publish(@RequestBody request: PublishRequest): ResponseEntity<String> {
        if (request.requestId.isBlank()) {
            return ResponseEntity.badRequest().body("request_id is required")
        }
        return try {
            val reply = requestGateway.dispatch(request.requestId, request.message)
            ResponseEntity.ok(reply)
        } catch (ex: KafkaReplyTimeoutException) {
            ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body("timeout")
        }
    }
}
