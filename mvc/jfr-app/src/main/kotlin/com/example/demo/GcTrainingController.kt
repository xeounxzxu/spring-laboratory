package com.example.demo

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.lang.management.GarbageCollectorMXBean
import java.lang.management.ManagementFactory
import java.time.Instant

@RestController
@RequestMapping("/gc")
class GcTrainingController {

    private val gcMxBeans: List<GarbageCollectorMXBean> = ManagementFactory.getGarbageCollectorMXBeans()

    @GetMapping("/young")
    fun triggerYoungGc(
        @RequestParam(defaultValue = "32") payloadSizeMb: Int,
        @RequestParam(defaultValue = "5") batches: Int,
        @RequestParam(defaultValue = "16") objectsPerBatch: Int
    ): YoungGcResponse {
        val normalizedPayloadMb = payloadSizeMb.coerceIn(1, 256)
        val normalizedBatches = batches.coerceIn(1, 200)
        val normalizedObjects = objectsPerBatch.coerceIn(1, 1024)

        val payloadBytes = normalizedPayloadMb * 1024L * 1024L

        val before = captureGcSnapshot()
        val totalAllocatedBytes = churnYoungGeneration(
            payloadBytes = payloadBytes,
            batches = normalizedBatches,
            objectsPerBatch = normalizedObjects
        )
        val after = captureGcSnapshot()

        return YoungGcResponse(
            timestamp = Instant.now(),
            payloadSizeMb = normalizedPayloadMb,
            batches = normalizedBatches,
            objectsPerBatch = normalizedObjects,
            totalAllocatedMb = totalAllocatedBytes / (1024.0 * 1024.0),
            gc = GarbageCollectionDiagnostics(before = before, after = after)
        )
    }

    private fun churnYoungGeneration(
        payloadBytes: Long,
        batches: Int,
        objectsPerBatch: Int
    ): Long {
        val survivors = ArrayList<ByteArray>(objectsPerBatch)
        var allocatedBytes = 0L
        repeat(batches) {
            repeat(objectsPerBatch) {
                val block = ByteArray(payloadBytes.toInt())
                survivors += block
                allocatedBytes += payloadBytes
            }
            survivors.clear()
        }
        return allocatedBytes
    }

    private fun captureGcSnapshot(): List<GcBeanSnapshot> = gcMxBeans.map {
        GcBeanSnapshot(
            name = it.name,
            collectionCount = it.collectionCount,
            collectionTimeMs = it.collectionTime
        )
    }
}

data class YoungGcResponse(
    val timestamp: Instant,
    val payloadSizeMb: Int,
    val batches: Int,
    val objectsPerBatch: Int,
    val totalAllocatedMb: Double,
    val gc: GarbageCollectionDiagnostics
)

data class GarbageCollectionDiagnostics(
    val before: List<GcBeanSnapshot>,
    val after: List<GcBeanSnapshot>
) {
    val deltas: List<GcBeanDelta> = after.map { afterSnapshot ->
        val beforeSnapshot = before.firstOrNull { it.name == afterSnapshot.name }
        val countDelta = (afterSnapshot.collectionCount - (beforeSnapshot?.collectionCount ?: 0)).coerceAtLeast(0)
        val timeDelta = if (afterSnapshot.collectionTimeMs != null && beforeSnapshot?.collectionTimeMs != null) {
            (afterSnapshot.collectionTimeMs - beforeSnapshot.collectionTimeMs).coerceAtLeast(0)
        } else {
            null
        }
        GcBeanDelta(
            name = afterSnapshot.name,
            collectionCountDelta = countDelta,
            collectionTimeDeltaMs = timeDelta,
            tracksYoungGeneration = YoungCollectorDetector.isYoungCollector(afterSnapshot.name)
        )
    }
}

data class GcBeanSnapshot(
    val name: String,
    val collectionCount: Long,
    val collectionTimeMs: Long?
)

data class GcBeanDelta(
    val name: String,
    val collectionCountDelta: Long,
    val collectionTimeDeltaMs: Long?,
    val tracksYoungGeneration: Boolean
)

private object YoungCollectorDetector {
    private val keywords = listOf("young", "scavenge", "nursery", "new")

    fun isYoungCollector(beanName: String): Boolean = keywords.any {
        beanName.contains(it, ignoreCase = true)
    }
}
