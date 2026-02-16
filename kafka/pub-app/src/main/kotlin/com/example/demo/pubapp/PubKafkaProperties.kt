package com.example.demo.pubapp

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.KafkaMessageListenerContainer
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate
import java.time.Duration

@ConfigurationProperties(prefix = "app.kafka")
data class PubKafkaProperties(
    val requestTopic: String,
    val replyTopic: String,
    val replyGroupId: String,
    val replyTimeoutMs: Long,
)

@Configuration
@EnableConfigurationProperties(PubKafkaProperties::class)
class PubKafkaConfig {
    @Bean
    fun repliesContainer(
        consumerFactory: ConsumerFactory<String, String>,
        kafkaProperties: PubKafkaProperties,
    ): KafkaMessageListenerContainer<String, String> {
        val containerProperties = ContainerProperties(kafkaProperties.replyTopic)
        containerProperties.setGroupId(kafkaProperties.replyGroupId)
        return KafkaMessageListenerContainer(consumerFactory, containerProperties)
    }

    @Bean
    fun replyingKafkaTemplate(
        producerFactory: ProducerFactory<String, String>,
        repliesContainer: KafkaMessageListenerContainer<String, String>,
        kafkaProperties: PubKafkaProperties,
    ): ReplyingKafkaTemplate<String, String, String> {
        val template = ReplyingKafkaTemplate(producerFactory, repliesContainer)
        template.setDefaultTopic(kafkaProperties.requestTopic)
        template.setDefaultReplyTimeout(Duration.ofMillis(kafkaProperties.replyTimeoutMs))
        return template
    }
}
