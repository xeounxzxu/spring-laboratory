package com.example.demo.coroutineapp

import java.time.Instant

data class EchoResponse(
    val message: String,
    val processedAt: Instant,
)

data class ParallelWorkerSnapshot(
    val workerId: Int,
    val durationMs: Long,
    val startedAt: Instant,
    val completedAt: Instant,
)

data class ParallelResponse(
    val results: List<ParallelWorkerSnapshot>,
) {
    val totalDurationMs: Long = results.maxOfOrNull { it.completedAt.toEpochMilli() }?.let { end ->
        val start = results.minOfOrNull { it.startedAt.toEpochMilli() } ?: end
        end - start
    } ?: 0
}

data class ExternalEchoResponse(
    val payload: String,
    val upstreamTimestamp: Instant,
    val traceId: String,
)
