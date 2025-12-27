package com.example.demo

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.lang.management.ManagementFactory
import java.time.Instant
import kotlin.math.cos
import kotlin.math.sin
import kotlin.system.measureNanoTime

@RestController
@RequestMapping("/latency")
class LatencyTrainingController {

    private val classLoadingMxBean = ManagementFactory.getClassLoadingMXBean()
    private val compilationMxBean = ManagementFactory.getCompilationMXBean()

    @GetMapping("/probe")
    fun probe(
        @RequestParam(defaultValue = "60000") iterationsPerRound: Int,
        @RequestParam(defaultValue = "5") rounds: Int,
        @RequestParam(required = false) loadClasses: String?
    ): LatencySnapshot {

        val requestedClasses = loadClasses?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.onEach { Class.forName(it, true, javaClass.classLoader) }
            ?: emptyList()

        val classLoadingBefore = captureClassLoadingSnapshot()
        val jitBefore = captureJitSnapshot()

        val roundsTelemetry = mutableListOf<RoundExecution>()
        val checksum: Long
        val durationNs = measureNanoTime {
            checksum = burnCpu(iterationsPerRound, rounds) { roundExecution ->
                roundsTelemetry += roundExecution
            }
        }

        val classLoadingAfter = captureClassLoadingSnapshot()
        val jitAfter = captureJitSnapshot()

        return LatencySnapshot(
            timestamp = Instant.now(),
            iterationsPerRound = iterationsPerRound,
            rounds = rounds,
            durationMs = durationNs / 1_000_000.0,
            checksum = checksum,
            perRoundExecutions = roundsTelemetry,
            classLoading = ClassLoadingDiagnostics(
                before = classLoadingBefore,
                after = classLoadingAfter
            ),
            jit = JitDiagnostics(
                before = jitBefore,
                after = jitAfter
            ),
            loadedClassesRequested = requestedClasses
        )
    }

    private fun burnCpu(
        iterations: Int,
        rounds: Int,
        perRoundRecorder: (RoundExecution) -> Unit
    ): Long {
        var checksum = 0L
        repeat(rounds) { round ->
            var seed = (round + 1).toDouble()
            val roundDurationNs = measureNanoTime {
                repeat(iterations) { step ->
                    val angle = seed + step
                    seed += sin(angle) * cos(seed / (step + 1))
                }
            }
            checksum = checksum xor seed.toBits()
            perRoundRecorder(
                RoundExecution(
                    round = round + 1,
                    durationMs = roundDurationNs / 1_000_000.0,
                    checksum = checksum
                )
            )
        }
        return checksum
    }

    private fun captureClassLoadingSnapshot(): ClassLoadingSnapshot = ClassLoadingSnapshot(
        loadedClassCount = classLoadingMxBean.loadedClassCount,
        totalLoadedClassCount = classLoadingMxBean.totalLoadedClassCount,
        unloadedClassCount = classLoadingMxBean.unloadedClassCount,
        verbose = classLoadingMxBean.isVerbose
    )

    private fun captureJitSnapshot(): JitSnapshot {
        val monitoringSupported = compilationMxBean.isCompilationTimeMonitoringSupported
        val compilationTime = if (monitoringSupported) compilationMxBean.totalCompilationTime else null
        return JitSnapshot(
            compilerName = compilationMxBean.name,
            compilationTimeMs = compilationTime,
            monitoringSupported = monitoringSupported
        )
    }
}

data class LatencySnapshot(
    val timestamp: Instant,
    val iterationsPerRound: Int,
    val rounds: Int,
    val durationMs: Double,
    val checksum: Long,
    val perRoundExecutions: List<RoundExecution>,
    val classLoading: ClassLoadingDiagnostics,
    val jit: JitDiagnostics,
    val loadedClassesRequested: List<String>
)

data class RoundExecution(
    val round: Int,
    val durationMs: Double,
    val checksum: Long
)

data class ClassLoadingDiagnostics(
    val before: ClassLoadingSnapshot,
    val after: ClassLoadingSnapshot
) {
    val deltaLoadedClassCount: Int = after.loadedClassCount - before.loadedClassCount
    val deltaTotalLoadedClassCount: Long = after.totalLoadedClassCount - before.totalLoadedClassCount
    val deltaUnloadedClassCount: Long = after.unloadedClassCount - before.unloadedClassCount
}

data class ClassLoadingSnapshot(
    val loadedClassCount: Int,
    val totalLoadedClassCount: Long,
    val unloadedClassCount: Long,
    val verbose: Boolean
)

data class JitDiagnostics(
    val before: JitSnapshot,
    val after: JitSnapshot
) {
    val compilationTimeDeltaMs: Long? = if (
        before.compilationTimeMs != null && after.compilationTimeMs != null
    ) {
        after.compilationTimeMs - before.compilationTimeMs
    } else {
        null
    }
}

data class JitSnapshot(
    val compilerName: String?,
    val compilationTimeMs: Long?,
    val monitoringSupported: Boolean
)
