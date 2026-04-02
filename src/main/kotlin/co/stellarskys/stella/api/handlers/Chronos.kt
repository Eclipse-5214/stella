package co.stellarskys.stella.api.handlers

import kotlinx.atomicfu.atomic
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/*
 * Modified from SkyHanni code
 * Under LGPL 2.1 License
 * https://github.com/hannibal002/SkyHanni/blob/beta/src/main/java/at/hannibal2/skyhanni/utils/SimpleTimeMark.kt
 */
object Chronos {
    // Time marks
    inline val now get() = SimpleTimeMark(System.currentTimeMillis())
    inline val zero get() = SimpleTimeMark(0)
    inline val max get() = SimpleTimeMark(Long.MAX_VALUE)
    inline val Duration.fromNow get() = now + this
    inline val Long.asTimeMark get() = SimpleTimeMark(this)
    inline val Duration.millis get() = inWholeMilliseconds

    // Schedulers
    object Tick: Scheduler("Tick")
    object Server: Scheduler("Server")
    object Async: Scheduler("Async") {
        init {
            Executors.newSingleThreadScheduledExecutor {
                Thread(it, "Stella-Chronos-Async").apply { isDaemon = true }
            }.scheduleAtFixedRate({ pulse() }, 0, 1, TimeUnit.MILLISECONDS)
        }
    }

    abstract class Scheduler(val label: String) {
        protected val tasks = ConcurrentHashMap<Long, MutableList<() -> Unit>>()
        protected val current = atomic(0L)

        fun pulse() {
            val now = current.incrementAndGet()
            tasks.remove(now)?.forEach { it() }
        }

        internal fun add(delay: Long, task: () -> Unit) {
            tasks.computeIfAbsent(current.value + delay) { CopyOnWriteArrayList() }.add(task)
        }

        infix fun after(delay: Int) = Builder(delay.toLong(), this)
        infix fun after(duration: Duration) = Builder(duration.inWholeMilliseconds, this)
        infix fun every(interval: Int) = Builder(interval.toLong(), this, repeat = true)
        infix fun every(duration: Duration) = Builder(duration.inWholeMilliseconds, this, repeat = true)
        infix fun post(action: () -> Unit): Task = after(1) run action
    }

    class Builder(
        private val value: Long,
        private val sched: Scheduler,
        private val repeat: Boolean = false,
        private var condition: (() -> Boolean)? = null
    ) {
        infix fun given(condition: () -> Boolean) = apply { this.condition = condition }

        infix fun run(action: () -> Unit): Task {
            val task = object : Task, () -> Unit {
                private val cancelled = atomic(false)
                override val isCancelled get() = cancelled.value
                override fun cancel() { cancelled.value = true }
                override fun invoke() {
                    if (isCancelled) return
                    val shouldRun = condition?.invoke() ?: true
                    if (!shouldRun) return
                    action()
                    if (repeat) sched.add(value, this)
                }
            }
            sched.add(value, task)
            return task
        }
    }

    interface Task { fun cancel(); val isCancelled: Boolean }

    @JvmInline
    value class SimpleTimeMark(val millis: Long) : Comparable<SimpleTimeMark> {
        operator fun minus(other: SimpleTimeMark) = (millis - other.millis).milliseconds
        operator fun plus(other: Duration) = SimpleTimeMark(millis + other.inWholeMilliseconds)
        operator fun minus(other: Duration) = plus(-other)

        inline val since get() = now - this
        inline val until get() = -since
        inline val isInPast get() = until.isNegative()
        inline val isInFuture get() = until.isPositive()
        inline val isZero get() = millis == 0L
        inline val isMax get() = millis == Long.MAX_VALUE
        fun takeIfInitialized() = if (isZero || isMax) null else this
        fun absoluteDifference(other: SimpleTimeMark) = abs(millis - other.millis).milliseconds

        override fun compareTo(other: SimpleTimeMark): Int = millis.compareTo(other.millis)

        override fun toString(): String = when (this) {
            zero -> "The Far Past"
            max -> "The Far Future"
            else -> Instant.ofEpochMilli(millis).toString()
        }

        fun formattedDate(pattern: String, use24h: Boolean = true): String {
            val newPattern = if (use24h) {
                pattern.replace("h", "H").replace("a", "")
            } else {
                pattern
            }
            val instant = Instant.ofEpochMilli(millis)
            val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
            val formatter = DateTimeFormatter.ofPattern(newPattern.trim())
            return localDateTime.format(formatter)
        }

        inline val toLocalDateTime get(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
        inline val toMillis get() = millis
        inline val toLocalDate get(): LocalDate = toLocalDateTime.toLocalDate()
    }
}