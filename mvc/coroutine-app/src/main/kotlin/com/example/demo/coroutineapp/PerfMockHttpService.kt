package com.example.demo.coroutineapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Service
class PerfMockHttpService(
    @Qualifier("perfMockRestClient")
    private val restClient: RestClient,
    @Qualifier("perfMockWebClient")
    private val webClient: WebClient,
) {
    fun relayEchoBlocking(delayMs: Long): ExternalEchoResponse {
        return restClient.get()
            .uri { builder ->
                builder.path("/echo")
                    .queryParam("text", "perf-blocking")
                    .queryParam("delay", delayMs)
                    .build()
            }
            .retrieve()
            .body(ExternalEchoResponse::class.java)
            ?: throw IllegalStateException("external echo returned empty body")
    }

    suspend fun relayEchoSuspend(delayMs: Long): ExternalEchoResponse = withContext(Dispatchers.IO) {
        webClient.get()
            .uri { builder ->
                builder.path("/echo")
                    .queryParam("text", "perf-suspend")
                    .queryParam("delay", delayMs)
                    .build()
            }
            .retrieve()
            .awaitBody()
    }
}
