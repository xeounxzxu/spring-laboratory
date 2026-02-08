package com.example.demo.pubapp

import org.springframework.http.MediaType
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class PubController(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val kafkaProperties: PubKafkaProperties,
) {
    @PostMapping("/kafka/publish", consumes = [MediaType.TEXT_PLAIN_VALUE], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun publish(@RequestBody message: String): String {
        kafkaTemplate.send(kafkaProperties.topic, message)
        return "published"
    }
}
