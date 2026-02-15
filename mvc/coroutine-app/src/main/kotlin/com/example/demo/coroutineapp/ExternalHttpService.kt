package com.example.demo.coroutineapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Service
class ExternalHttpService(
    private val webClient: WebClient,
) {
    suspend fun relayEcho(message: String): ExternalEchoResponse = withContext(Dispatchers.IO) {
        webClient.get()
            .uri { builder -> builder.path("/echo").queryParam("text", message).build() }
            .retrieve()
            .awaitBody()
    }
}
