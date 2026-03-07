package com.example.demo.coroutineapp

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/perf-io", produces = [MediaType.APPLICATION_JSON_VALUE])
class PerformanceIoController(
    private val perfMockHttpService: PerfMockHttpService,
) {
    @GetMapping("/blocking")
    fun blocking(
        @RequestParam(required = false, defaultValue = "50") sleepMs: Long,
    ): PerfProbeResponse {
        if (sleepMs < 0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "sleepMs must be >= 0")
        }

        val startNanos = System.nanoTime()
        perfMockHttpService.relayEchoBlocking(delayMs = sleepMs)
        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000

        return PerfProbeResponse(
            endpoint = "io-blocking",
            requestedSleepMs = sleepMs,
            elapsedMs = elapsedMs,
            thread = Thread.currentThread().name
        )
    }

    @GetMapping("/suspend")
    suspend fun suspendBlocking(
        @RequestParam(required = false, defaultValue = "50") sleepMs: Long,
    ): PerfProbeResponse {
        if (sleepMs < 0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "sleepMs must be >= 0")
        }

        val startNanos = System.nanoTime()
        perfMockHttpService.relayEchoSuspend(delayMs = sleepMs)
        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000

        return PerfProbeResponse(
            endpoint = "io-suspend",
            requestedSleepMs = sleepMs,
            elapsedMs = elapsedMs,
            thread = Thread.currentThread().name
        )
    }
}
