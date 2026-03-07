package com.example.demo.coroutineapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

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
        requestId: String,
        sleep: Long
    ) {
        return runBlocking(Dispatchers.IO) {

            val a = async(Dispatchers.IO) {
                logger.info("pingPubApp start requestId={} thread={}", requestId, Thread.currentThread().name)
                val response = pingHttpService.ping(requestId = requestId, sleep = sleep)
                logger.info(
                    "pingPubApp end requestId={} status={} thread={}",
                    requestId,
                    response.status,
                    Thread.currentThread().name
                )
            }


            val b = async(Dispatchers.IO) {
                logger.info("pingPubApp start requestId={} thread={}", requestId, Thread.currentThread().name)
                val response = pingHttpService.ping(requestId = requestId, sleep = sleep)
                logger.info(
                    "pingPubApp end requestId={} status={} thread={}",
                    requestId,
                    response.status,
                    Thread.currentThread().name
                )
            }

            val c = async(Dispatchers.IO) {
                logger.info("pingPubApp start requestId={} thread={}", requestId, Thread.currentThread().name)
                val response = pingHttpService.ping(requestId = requestId, sleep = sleep)
                logger.info(
                    "pingPubApp end requestId={} status={} thread={}",
                    requestId,
                    response.status,
                    Thread.currentThread().name
                )
            }

            a.await()
            b.await()
            c.await()
        }
    }
}
