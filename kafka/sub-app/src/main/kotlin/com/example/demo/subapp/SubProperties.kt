package com.example.demo.subapp

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "app.kafka")
data class SubKafkaProperties(
    val replyTopic: String,
)

@Configuration
@EnableConfigurationProperties(SubKafkaProperties::class)
class SubConfig
