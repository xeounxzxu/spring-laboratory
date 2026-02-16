package com.example.demo.pubapp

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.currentCoroutineContext

suspend fun coroutineLogContext(): String {
    val coroutineName = currentCoroutineContext()[CoroutineName]?.name ?: "unnamed"
    val threadName = Thread.currentThread().name
    return "thread=$threadName, coroutine=$coroutineName"
}
