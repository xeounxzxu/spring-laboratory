package com.example.demo.coroutineapp

import io.netty.channel.ChannelOption
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration

@ConfigurationProperties(prefix = "app.ping")
data class PingApiProperties(
    val baseUrl: String,
    val path: String,
    val connectTimeoutMs: Int,
    val readTimeoutMs: Long,
)

@Configuration
@EnableConfigurationProperties(PingApiProperties::class)
class PingApiConfig {
    @Bean
    fun pingWebClient(properties: PingApiProperties): WebClient {
        val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.connectTimeoutMs)
            .responseTimeout(Duration.ofMillis(properties.readTimeoutMs))
        return WebClient.builder()
            .baseUrl(properties.baseUrl)
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .build()
    }
}
