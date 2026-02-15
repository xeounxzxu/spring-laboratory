package com.example.demo.coroutineapp

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/mock", produces = [MediaType.APPLICATION_JSON_VALUE])
class ExternalMockController {

    @GetMapping("/echo")
    fun externalEcho(
        @RequestParam text: String,
        @RequestParam(required = false) delay: Int?
    ): ExternalEchoResponse {

        if (delay != null) {
            Thread.sleep(delay.toLong())
        }

        return ExternalEchoResponse(
            payload = "external:$text",
            upstreamTimestamp = Instant.now(),
            traceId = UUID.randomUUID().toString(),
        )
    }
}
