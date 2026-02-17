package com.example.demo.pubapp

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "app.kafka")
data class PubKafkaProperties(
    val requestTopic: String,
    val replyTopic: String,
    val replyGroupId: String,
    // HTTP 요청 1건이 최대 대기할 수 있는 시간(ms)
    val replyTimeoutMs: Long,
    // 코루틴 Job이 reply를 확인하는 폴링 간격(ms)
    val pollIntervalMs: Long,
)

@Configuration
@EnableConfigurationProperties(PubKafkaProperties::class)
class PubKafkaConfig
