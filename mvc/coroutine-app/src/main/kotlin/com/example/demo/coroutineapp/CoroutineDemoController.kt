package com.example.demo.coroutineapp

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/coroutines", produces = [MediaType.APPLICATION_JSON_VALUE])
class CoroutineDemoController(
    private val externalHttpService: ExternalHttpService,
) {
    @GetMapping("/external-echo")
    suspend fun externalEcho(
        @RequestParam(
            required = false,
            defaultValue = "ping"
        ) text: String
    ): ExternalEchoResponse {
        return externalHttpService.relayEcho(text)
    }
}
