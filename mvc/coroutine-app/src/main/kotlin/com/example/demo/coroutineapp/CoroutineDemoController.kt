package com.example.demo.coroutineapp

import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private val logger = LoggerFactory.getLogger(CoroutineDemoController::class.java)

@RestController
@RequestMapping("/coroutines", produces = [MediaType.APPLICATION_JSON_VALUE])
class CoroutineDemoController(
    private val coroutineService: CoroutineService,
) {
    @GetMapping("/external-echo")
    suspend fun externalEcho(
        @RequestParam(
            required = false,
            defaultValue = "ping"
        ) text: String
    ): ExternalEchoResponse {
        return coroutineService.externalHttpService(text)
    }

    @GetMapping("/pub-ping")
    suspend fun pubPing(
//        @RequestParam(required = false) sleep: Long?,
    ): PingRelayResponse {
        logger.info("pubPing start")
        val data = coroutineService.pingPubApp(
//            sleep = sleep ?: 1L
            sleep = 0L
        )
        logger.info("pubPing end")
        return data
    }
}
