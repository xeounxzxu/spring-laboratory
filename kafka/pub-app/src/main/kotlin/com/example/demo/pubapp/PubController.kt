package com.example.demo.pubapp

import kotlinx.coroutines.TimeoutCancellationException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
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
        val job = try {
            requestGateway.dispatch(request.requestId, request.message)
        } catch (ex: IllegalArgumentException) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("duplicate request_id")
        }
        return try {
            val reply = job.await()
            ResponseEntity.ok(reply)
        } catch (ex: TimeoutCancellationException) {
            ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body("timeout")
        }
    }
}
