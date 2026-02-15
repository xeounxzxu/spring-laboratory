package com.example.demo.coroutineapp

import io.netty.channel.ChannelOption
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import java.time.Duration

@ConfigurationProperties(prefix = "app.external")
data class ExternalApiProperties(
    val baseUrl: String,
    val connectTimeoutMs: Int,
    val readTimeoutMs: Long,
)

@Configuration
@EnableConfigurationProperties(ExternalApiProperties::class)
class ExternalApiConfig {
    @Bean
    fun coroutineWebClient(properties: ExternalApiProperties): WebClient {
        val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.connectTimeoutMs)
            .responseTimeout(Duration.ofMillis(properties.readTimeoutMs))
        return WebClient.builder()
            .baseUrl(properties.baseUrl)
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .build()
    }
}
