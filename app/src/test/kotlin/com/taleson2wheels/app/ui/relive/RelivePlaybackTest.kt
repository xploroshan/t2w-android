package com.taleson2wheels.app.ui.relive

import com.taleson2wheels.app.data.remote.dto.LivePathPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RelivePlaybackTest {

    private fun pt(lat: Double, lng: Double, at: String? = null, speed: Double? = null) =
        LivePathPoint(lat, lng, recordedAt = at, speed = speed)

    @Test
    fun buildTimeline_uses_real_timestamps_when_present() {
        val path = listOf(
            pt(12.90, 77.50, "2026-06-01T09:00:00Z"),
            pt(12.91, 77.51, "2026-06-01T09:00:10Z"), // +10s
            pt(12.92, 77.52, "2026-06-01T09:00:40Z"), // +40s
        )
        val t = RelivePlayback.buildTimeline(path)

        assertEquals(3, t.size)
        assertEquals(0L, t[0].tMs)
        assertEquals(10_000L, t[1].tMs)
        assertEquals(40_000L, t[2].tMs)
        assertEquals(40_000L, RelivePlayback.durationMs(t))
    }

    @Test
    fun buildTimeline_falls_back_to_even_spacing_without_timestamps() {
        val path = listOf(pt(12.90, 77.50), pt(12.91, 77.51), pt(12.92, 77.52))
        val t = RelivePlayback.buildTimeline(path)

        assertEquals(0L, t[0].tMs)
        assertEquals(RelivePlayback.DEFAULT_SEGMENT_MS, t[1].tMs)
        assertEquals(2 * RelivePlayback.DEFAULT_SEGMENT_MS, t[2].tMs)
    }

    @Test
    fun buildTimeline_falls_back_when_a_timestamp_is_missing() {
        val path = listOf(
            pt(12.90, 77.50, "2026-06-01T09:00:00Z"),
            pt(12.91, 77.51, null), // one gap → even spacing for the whole track
            pt(12.92, 77.52, "2026-06-01T09:00:40Z"),
        )
        val t = RelivePlayback.buildTimeline(path)
        assertEquals(RelivePlayback.DEFAULT_SEGMENT_MS, t[1].tMs)
    }

    @Test
    fun buildTimeline_clamps_out_of_order_timestamps_forward() {
        val path = listOf(
            pt(12.90, 77.50, "2026-06-01T09:00:00Z"),
            pt(12.91, 77.51, "2026-06-01T09:00:30Z"),
            pt(12.92, 77.52, "2026-06-01T09:00:20Z"), // earlier than previous
            pt(12.93, 77.53, "2026-06-01T09:00:50Z"),
        )
        val t = RelivePlayback.buildTimeline(path)
        // Never rewinds: the out-of-order fix is clamped to the previous offset.
        assertTrue(t[2].tMs >= t[1].tMs)
        assertEquals(30_000L, t[2].tMs)
        assertEquals(50_000L, t[3].tMs)
    }

    @Test
    fun sample_interpolates_position_at_the_segment_midpoint() {
        val path = listOf(
            pt(10.0, 20.0, "2026-06-01T09:00:00Z"),
            pt(12.0, 24.0, "2026-06-01T09:00:10Z"),
        )
        val t = RelivePlayback.buildTimeline(path)
        val s = RelivePlayback.sample(t, emptyList(), 5_000L)!!

        assertEquals(11.0, s.lat, 1e-9)   // halfway
        assertEquals(22.0, s.lng, 1e-9)
        assertEquals(0.5f, s.progress, 1e-6f)
        assertEquals(0, s.index)
    }

    @Test
    fun sample_clamps_out_of_range_offsets_to_the_endpoints() {
        val t = RelivePlayback.buildTimeline(
            listOf(pt(10.0, 20.0), pt(11.0, 21.0), pt(12.0, 22.0)),
        )
        val before = RelivePlayback.sample(t, emptyList(), -1_000L)!!
        val after = RelivePlayback.sample(t, emptyList(), 999_999L)!!

        assertEquals(10.0, before.lat, 1e-9)
        assertEquals(0f, before.progress, 1e-6f)
        assertEquals(12.0, after.lat, 1e-9)
        assertEquals(1f, after.progress, 1e-6f)
    }

    @Test
    fun sample_converts_speed_from_mps_to_kmh() {
        val path = listOf(
            pt(10.0, 20.0, "2026-06-01T09:00:00Z", speed = 10.0), // 10 m/s
            pt(11.0, 21.0, "2026-06-01T09:00:10Z", speed = 20.0), // 20 m/s
        )
        val t = RelivePlayback.buildTimeline(path)
        val start = RelivePlayback.sample(t, emptyList(), 0L)!!
        val mid = RelivePlayback.sample(t, emptyList(), 5_000L)!!

        assertEquals(36.0, start.speedKmh!!, 1e-6)  // 10 * 3.6
        assertEquals(54.0, mid.speedKmh!!, 1e-6)    // 15 * 3.6 (interpolated)
    }

    @Test
    fun sample_reads_elevation_series_by_progress() {
        val t = RelivePlayback.buildTimeline(
            listOf(pt(10.0, 20.0), pt(11.0, 21.0), pt(12.0, 22.0)),
        )
        val elevations = listOf(100f, 200f, 300f)
        val start = RelivePlayback.sample(t, elevations, 0L)!!
        val end = RelivePlayback.sample(t, elevations, RelivePlayback.durationMs(t))!!

        assertEquals(100f, start.elevationM!!, 1e-3f)
        assertEquals(300f, end.elevationM!!, 1e-3f)
    }

    @Test
    fun sample_returns_null_for_an_empty_track() {
        assertNull(RelivePlayback.sample(emptyList(), emptyList(), 0L))
    }

    @Test
    fun sample_handles_a_single_point_track() {
        val t = RelivePlayback.buildTimeline(listOf(pt(10.0, 20.0)))
        val s = RelivePlayback.sample(t, emptyList(), 1_000L)
        assertNotNull(s)
        assertEquals(10.0, s!!.lat, 1e-9)
        assertEquals(0f, s.progress, 1e-6f)
    }

    @Test
    fun bearing_due_east_is_about_90_degrees() {
        val b = RelivePlayback.bearing(0.0, 0.0, 0.0, 1.0)
        assertEquals(90.0, b, 0.5)
    }

    @Test
    fun bearing_due_north_is_about_zero_degrees() {
        val b = RelivePlayback.bearing(0.0, 0.0, 1.0, 0.0)
        assertEquals(0.0, b, 0.5)
    }

    @Test
    fun formatClock_renders_minutes_and_seconds() {
        assertEquals("0:00", RelivePlayback.formatClock(0L))
        assertEquals("0:05", RelivePlayback.formatClock(5_000L))
        assertEquals("1:05", RelivePlayback.formatClock(65_000L))
    }

    @Test
    fun downsample_caps_point_count_and_keeps_endpoints() {
        val many = (0..99).map { pt(it.toDouble(), it.toDouble()) }
        val out = RelivePlayback.downsample(many, 10)

        assertTrue(out.size <= 11)
        assertEquals(many.first(), out.first())
        assertEquals(many.last(), out.last())
    }

    @Test
    fun downsample_returns_input_when_already_small() {
        val few = listOf(pt(1.0, 1.0), pt(2.0, 2.0))
        assertEquals(few, RelivePlayback.downsample(few, 10))
    }
}
