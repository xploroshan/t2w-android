package com.taleson2wheels.app.ui.relive

import androidx.compose.runtime.Immutable
import com.taleson2wheels.app.data.remote.dto.LivePathPoint
import java.time.Instant
import java.time.OffsetDateTime
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** One point on the flyover timeline: a position tagged with its offset (ms) from track start. */
@Immutable
data class ReliveTrackPoint(
    val lat: Double,
    val lng: Double,
    /** Milliseconds since the first point of the track. Non-decreasing across the list. */
    val tMs: Long,
    /** Raw GPS speed in metres/second, if the fix carried one. */
    val speedMps: Double? = null,
)

/** The interpolated flyover state at a playback instant — what the map + HUD render. */
@Immutable
data class ReliveSample(
    val lat: Double,
    val lng: Double,
    /** Travel direction in degrees (0 = north, clockwise) for the camera/marker. */
    val bearing: Double,
    /** Speed in km/h at this instant, if known. */
    val speedKmh: Double?,
    /** Interpolated altitude in metres, if an elevation series was loaded. */
    val elevationM: Float?,
    /** Index of the last track point already passed. */
    val index: Int,
    /** Fraction through the whole track, 0..1. */
    val progress: Float,
)

/**
 * Pure, framework-free playback engine for the Relive flyover. Turns a recorded
 * [LivePathPoint] path into a time-indexed [ReliveTrackPoint] timeline, then
 * samples an interpolated position/speed/elevation/bearing at any playback
 * offset. No Android, no coroutines — fully unit-testable.
 */
object RelivePlayback {

    /**
     * Fallback segment duration when the recorded fixes carry no usable timestamps
     * (missing, or not strictly progressing) — the track is then played back at a
     * constant one-second-per-point cadence so the flyover still animates.
     */
    const val DEFAULT_SEGMENT_MS = 1_000L

    private const val MPS_TO_KMH = 3.6

    /**
     * Build the playback timeline from a recorded path.
     *
     * Uses real fix timestamps when every point has a parseable `recordedAt` and
     * the series spans a positive, non-decreasing range — so a paused/slow stretch
     * plays back slowly and a fast stretch plays back fast, exactly as it happened.
     * Any missing/degenerate timestamp falls back to even [DEFAULT_SEGMENT_MS]
     * spacing. Out-of-order fixes are clamped forward (running max) so the timeline
     * never rewinds.
     */
    fun buildTimeline(path: List<LivePathPoint>): List<ReliveTrackPoint> {
        if (path.isEmpty()) return emptyList()
        if (path.size == 1) {
            val p = path[0]
            return listOf(ReliveTrackPoint(p.lat, p.lng, 0L, p.speed))
        }

        val stamps = path.map { parseMillis(it.recordedAt) }
        val realTimed = stamps.all { it != null } &&
            (stamps.last()!! - stamps.first()!!) > 0L

        if (!realTimed) {
            return path.mapIndexed { i, p ->
                ReliveTrackPoint(p.lat, p.lng, i * DEFAULT_SEGMENT_MS, p.speed)
            }
        }

        val base = stamps.first()!!
        var last = 0L
        return path.mapIndexed { i, p ->
            // Clamp forward so a stray out-of-order fix can't rewind the timeline.
            val t = (stamps[i]!! - base).coerceAtLeast(last)
            last = t
            ReliveTrackPoint(p.lat, p.lng, t, p.speed)
        }
    }

    /** Total playback length in ms (0 for an empty/single-point track). */
    fun durationMs(track: List<ReliveTrackPoint>): Long = track.lastOrNull()?.tMs ?: 0L

    /**
     * Sample the interpolated flyover state at [playbackMs] (clamped into range).
     * Returns null only for an empty track. Positions and speed are linearly
     * interpolated within the active segment; elevation is sampled from
     * [elevations] by progress; bearing points along the active segment.
     */
    fun sample(
        track: List<ReliveTrackPoint>,
        elevations: List<Float>,
        playbackMs: Long,
    ): ReliveSample? {
        if (track.isEmpty()) return null
        val duration = durationMs(track)
        if (track.size == 1 || duration <= 0L) {
            val p = track.first()
            return ReliveSample(
                lat = p.lat,
                lng = p.lng,
                bearing = 0.0,
                speedKmh = p.speedMps?.times(MPS_TO_KMH),
                elevationM = elevationAt(elevations, 0f),
                index = 0,
                progress = 0f,
            )
        }

        val clamped = playbackMs.coerceIn(0L, duration)
        val progress = (clamped.toFloat() / duration).coerceIn(0f, 1f)
        val i = segmentIndex(track, clamped)
        val a = track[i]
        val b = track[i + 1]
        val segLen = b.tMs - a.tMs
        val f = if (segLen > 0L) (clamped - a.tMs).toDouble() / segLen else 0.0

        val lat = a.lat + (b.lat - a.lat) * f
        val lng = a.lng + (b.lng - a.lng) * f
        val speed = interpolateSpeed(a.speedMps, b.speedMps, f)

        return ReliveSample(
            lat = lat,
            lng = lng,
            bearing = bearing(a.lat, a.lng, b.lat, b.lng),
            speedKmh = speed?.times(MPS_TO_KMH),
            elevationM = elevationAt(elevations, progress),
            index = i,
            progress = progress,
        )
    }

    /** Largest segment index i (0..size-2) with track[i].tMs <= ms, via binary search. */
    private fun segmentIndex(track: List<ReliveTrackPoint>, ms: Long): Int {
        var lo = 0
        var hi = track.size - 2
        var ans = 0
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (track[mid].tMs <= ms) {
                ans = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return ans
    }

    private fun interpolateSpeed(a: Double?, b: Double?, f: Double): Double? = when {
        a != null && b != null -> a + (b - a) * f
        else -> a ?: b
    }

    /** Sample an elevation series by fractional progress, interpolating between samples. */
    private fun elevationAt(elevations: List<Float>, progress: Float): Float? {
        if (elevations.isEmpty()) return null
        if (elevations.size == 1) return elevations[0]
        val pos = (progress.coerceIn(0f, 1f)) * (elevations.size - 1)
        val i = pos.toInt().coerceIn(0, elevations.size - 2)
        val f = pos - i
        return elevations[i] + (elevations[i + 1] - elevations[i]) * f
    }

    /** Initial bearing from (lat1,lng1) to (lat2,lng2), degrees clockwise from north. */
    fun bearing(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLon = Math.toRadians(lng2 - lng1)
        val la1 = Math.toRadians(lat1)
        val la2 = Math.toRadians(lat2)
        val y = sin(dLon) * cos(la2)
        val x = cos(la1) * sin(la2) - sin(la1) * cos(la2) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    /** Format a playback offset as m:ss for the scrubber label. */
    fun formatClock(ms: Long): String {
        val totalSec = (ms / 1000L).coerceAtLeast(0L)
        val m = totalSec / 60
        val s = totalSec % 60
        return "%d:%02d".format(m, s)
    }

    /** ISO-8601 → epoch millis, tolerating both `Instant` (Z) and offset forms; null if unparseable. */
    private fun parseMillis(iso: String?): Long? {
        if (iso.isNullOrBlank()) return null
        return runCatching { Instant.parse(iso).toEpochMilli() }
            .recoverCatching { OffsetDateTime.parse(iso).toInstant().toEpochMilli() }
            .getOrNull()
    }
}
