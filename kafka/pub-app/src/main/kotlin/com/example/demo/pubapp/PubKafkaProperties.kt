package com.example.demo.pubapp

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "app.kafka")
data class PubKafkaProperties(
    val requestTopic: String,
    val replyTimeoutMs: Long,
)

@Configuration
@EnableConfigurationProperties(PubKafkaProperties::class)
class PubKafkaConfig
