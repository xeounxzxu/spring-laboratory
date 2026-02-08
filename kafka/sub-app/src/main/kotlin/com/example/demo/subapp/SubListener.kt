package com.example.demo.subapp

import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class SubListener {
    private val logger = LoggerFactory.getLogger(SubListener::class.java)

    @KafkaListener(topics = ["\${app.kafka.topic}"])
    fun onMessage(message: String) {
        logger.info("received: {}", message)
    }
}
