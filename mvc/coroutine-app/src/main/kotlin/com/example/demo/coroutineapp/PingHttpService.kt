package com.example.demo.coroutineapp

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange

@Service
class PingHttpService(
    @Qualifier("pingWebClient")
    private val webClient: WebClient,
    private val pingApiProperties: PingApiProperties,
) {
    private val logger = LoggerFactory.getLogger(PingHttpService::class.java)

    suspend fun ping(
        sleep: Long = 0
    ): PingRelayResponse = withContext(Dispatchers.IO + CoroutineName("webclient-ping-8081")) {
        val coroutineName = currentCoroutineContext()[CoroutineName]?.name ?: "unnamed"
        logger.info(
            "ping request start path={} coroutine={} thread={}",
            pingApiProperties.path,
            coroutineName,
            Thread.currentThread().name
        )

        val response = webClient.get()
            .uri(pingApiProperties.path)
            .awaitExchange { response ->
                val body = response.awaitBody<String>()
                PingRelayResponse(
                    status = response.statusCode().value(),
                    body = body,
                )
            }
        logger.info(
            "ping request end status={} coroutine={} thread={}",
            response.status,
            coroutineName,
            Thread.currentThread().name
        )
        response
    }
}
