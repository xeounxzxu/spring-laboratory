package com.example.demo.pubapp

import com.fasterxml.jackson.databind.JsonNode

data class PublishRequest(
    val requestId: String,
    val type: String? = null,
    val version: Int? = null,
    val payload: JsonNode? = null,
    val message: String? = null,
)

data class MessageEnvelope(
    val requestId: String,
    val type: String,
    val version: Int,
    val payload: JsonNode,
)
