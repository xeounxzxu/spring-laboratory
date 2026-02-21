package com.example.demo.coroutineapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

private val logger = LoggerFactory.getLogger(CoroutineService::class.java)

@Service
class CoroutineService(
    private val externalHttpService: ExternalHttpService,
    private val pingHttpService: PingHttpService,
) {


    fun externalHttpService(text: String): ExternalEchoResponse =
        runBlocking(context = Dispatchers.IO) {
            externalHttpService.relayEcho(message = text)
        }

    fun pingPubApp(
        sleep: Long
    ): PingRelayResponse {
        return runBlocking(context = Dispatchers.IO) {
            logger.info("pingPubApp start thread={}", Thread.currentThread().name)
            val response = pingHttpService.ping()
            logger.info("pingPubApp end status={} thread={}", response.status, Thread.currentThread().name)
            response
        }
    }
}
