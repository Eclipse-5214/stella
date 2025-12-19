package co.stellarskys.stella.events.api

import kotlin.system.measureNanoTime

object EventBusBenchmark {

    /** Measure registration cost for N handlers */
    fun benchmarkRegistration(bus: EventBus, handlers: Int): Long {
        return measureNanoTime {
            repeat(handlers) {
                bus.on<TestEvent> { /* no-op */ }
            }
        }
    }

    /** Measure dispatch cost for posting one event through M handlers */
    fun benchmarkDispatch(bus: EventBus, handlers: Int, iterations: Int): Long {
        // Register handlers
        repeat(handlers) {
            bus.on<TestEvent> { /* no-op */ }
        }

        // Warmup
        repeat(10_000) { bus.post(TestEvent()) }

        // Measure
        return measureNanoTime {
            repeat(iterations) {
                bus.post(TestEvent())
            }
        }
    }
}

/** Example event type for benchmarking */
class TestEvent : Event()
