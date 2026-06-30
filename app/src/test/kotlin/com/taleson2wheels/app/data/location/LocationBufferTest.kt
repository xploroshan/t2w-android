package com.taleson2wheels.app.data.location

import com.taleson2wheels.app.data.remote.dto.LocationPoint
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Bounds the live-location retry buffer so a prolonged upload failure can't grow
 * memory or the re-uploaded payload without bound.
 */
class LocationBufferTest {

    private fun pt(i: Int) = LocationPoint(lat = 0.0, lng = 0.0, recordedAt = i.toString())
    private fun List<LocationPoint>.ids() = map { it.recordedAt }

    @Test
    fun add_drops_the_oldest_once_over_cap() {
        val b = LocationBuffer(maxRetained = 3, maxBatch = 10)
        (1..5).forEach { b.add(pt(it)) }

        assertEquals("size is capped", 3, b.size())
        assertEquals("oldest (1,2) dropped, newest kept", listOf("3", "4", "5"), b.drainAll().ids())
    }

    @Test
    fun nextBatch_returns_up_to_maxBatch_oldest_in_fifo_order() {
        val b = LocationBuffer(maxRetained = 100, maxBatch = 2)
        (1..5).forEach { b.add(pt(it)) }

        assertEquals(listOf("1", "2"), b.nextBatch().ids())
        assertEquals(listOf("3", "4"), b.nextBatch().ids())
        assertEquals(listOf("5"), b.nextBatch().ids())
        assertEquals(emptyList<String>(), b.nextBatch().ids())
    }

    @Test
    fun requeue_retries_the_failed_batch_before_newer_fixes() {
        val b = LocationBuffer(maxRetained = 100, maxBatch = 2)
        (1..4).forEach { b.add(pt(it)) }
        val failed = b.nextBatch() // [1,2]
        b.add(pt(5)) // newer fix arrives while [1,2] was "in flight"

        b.requeue(failed)

        assertEquals("the failed batch is retried first, in order", listOf("1", "2"), b.nextBatch().ids())
        assertEquals(listOf("3", "4"), b.nextBatch().ids())
        assertEquals(listOf("5"), b.nextBatch().ids())
    }

    @Test
    fun requeue_still_respects_the_cap_so_memory_stays_bounded() {
        val b = LocationBuffer(maxRetained = 3, maxBatch = 2)
        (1..3).forEach { b.add(pt(it)) } // [1,2,3] — at cap

        // A failed batch comes back while the buffer is already full: the re-queued
        // (now-oldest) fixes are dropped rather than growing past the cap.
        b.requeue(listOf(pt(98), pt(99)))

        assertEquals(3, b.size())
        assertEquals(listOf("1", "2", "3"), b.drainAll().ids())
    }
}
