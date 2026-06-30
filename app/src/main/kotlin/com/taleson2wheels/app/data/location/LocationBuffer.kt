package com.taleson2wheels.app.data.location

import com.taleson2wheels.app.data.remote.dto.LocationPoint

/**
 * Thread-safe bounded FIFO of pending GPS fixes for the live-location uploader.
 *
 * Two caps keep a prolonged upload failure (e.g. a long ride through a cellular
 * dead zone) from growing memory or the re-uploaded payload without bound:
 *  - [maxRetained] caps total buffered fixes; on overflow the OLDEST are dropped
 *    (the freshest position matters most for live "where is everyone now"
 *    tracking, and the oldest un-uploaded fixes are the least recoverable).
 *  - [maxBatch] caps how many fixes a single upload attempt drains, so one
 *    failing batch can't keep re-serializing an ever-growing payload every tick.
 */
class LocationBuffer(
    private val maxRetained: Int = MAX_RETAINED,
    private val maxBatch: Int = MAX_BATCH,
) {
    private val deque = ArrayDeque<LocationPoint>()

    /** Append a new fix, then drop the oldest if over [maxRetained]. */
    @Synchronized
    fun add(point: LocationPoint) {
        deque.addLast(point)
        trim()
    }

    /** Take up to [maxBatch] of the oldest fixes (FIFO) for an upload attempt. */
    @Synchronized
    fun nextBatch(): List<LocationPoint> {
        if (deque.isEmpty()) return emptyList()
        val n = minOf(deque.size, maxBatch)
        return ArrayList<LocationPoint>(n).apply { repeat(n) { add(deque.removeFirst()) } }
    }

    /** Put a failed batch back at the front (retried first), then re-cap. During a
     *  sustained failure this still drops the oldest once over [maxRetained], so
     *  memory stays bounded rather than the re-queue growing forever. */
    @Synchronized
    fun requeue(points: List<LocationPoint>) {
        points.asReversed().forEach { deque.addFirst(it) }
        trim()
    }

    /** Remove and return everything — used for the final flush / persistence. */
    @Synchronized
    fun drainAll(): List<LocationPoint> {
        if (deque.isEmpty()) return emptyList()
        val copy = deque.toList()
        deque.clear()
        return copy
    }

    @Synchronized
    fun clear() = deque.clear()

    @Synchronized
    fun size(): Int = deque.size

    private fun trim() {
        while (deque.size > maxRetained) deque.removeFirst()
    }

    companion object {
        /** ~70 min of fixes at one every ~3s; a few hundred KB serialized. */
        const val MAX_RETAINED = 5_000
        const val MAX_BATCH = 1_000
    }
}
