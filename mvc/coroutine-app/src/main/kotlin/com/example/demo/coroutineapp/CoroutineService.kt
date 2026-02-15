package com.example.demo.coroutineapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service

@Service
class CoroutineService(
    private val externalHttpService: ExternalHttpService
) {


    fun externalHttpService(text: String): ExternalEchoResponse =
        runBlocking(context = Dispatchers.IO) {
            externalHttpService.relayEcho(message = text)
        }
}
