package com.taleson2wheels.app.data.location

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.taleson2wheels.app.R
import com.taleson2wheels.app.T2WApplication
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.LocationPoint
import com.taleson2wheels.app.data.repository.LiveRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Foreground service that streams the device's GPS up to `/live/location` for a
 * ride, so tracking continues while the app is backgrounded or the screen is
 * off. Started/stopped from the live screen; publishes progress through
 * [LiveShareController]. The caller must hold a location permission before
 * starting (the live screen requests it).
 */
class LiveLocationService : Service() {

    private val scope = CoroutineScope(SupervisorJob())
    private val tracker by lazy { LocationTracker(this) }
    private val repository: LiveRepository by lazy { (application as T2WApplication).container.liveRepository }

    private val buffer = LocationBuffer()
    private var flushJob: Job? = null
    private var rideId: String? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val id = intent?.getStringExtra(EXTRA_RIDE_ID)
        if (id.isNullOrBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }
        // The OS can restart a location FGS (START_REDELIVER_INTENT) after the
        // location permission was revoked; calling startForeground / starting GPS
        // then would crash. Bail cleanly instead.
        if (!hasLocationPermission()) {
            stopSelf()
            return START_NOT_STICKY
        }
        // Switching to a different ride while already running: drop the previous
        // ride's buffered fixes so they aren't uploaded to the new ride's id.
        if (rideId != null && rideId != id) {
            buffer.clear()
        }
        rideId = id

        createChannel()
        if (!startForegroundSafely()) {
            // ForegroundServiceStartNotAllowed (API 31+) / SecurityException
            // (missing type or permission, API 34+) — can't run, so stop quietly.
            stopSelf()
            return START_NOT_STICKY
        }
        LiveShareController.onStart(id)
        // Re-upload any tail persisted by a previous (torn-down) session — on this
        // live FGS's scope, so it's far more reliable than the old onDestroy
        // fire-and-forget, and to its ORIGINAL ride id.
        recoverPendingFixes()

        if (flushJob == null) {
            tracker.start { loc -> enqueue(loc) }
            flushJob = scope.launch {
                while (isActive) {
                    delay(FLUSH_MS)
                    // Read the CURRENT ride id each tick (not a captured local) so a
                    // mid-session ride change uploads to the right ride.
                    rideId?.let { flush(it) }
                }
            }
        }
        // Re-deliver the last intent (with the ride id) if the service is restarted.
        return START_REDELIVER_INTENT
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    /** Start in the foreground with the location type (required on API 34+),
     *  swallowing the platform's start-not-allowed/security exceptions. */
    private fun startForegroundSafely(): Boolean = try {
        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                0
            },
        )
        true
    } catch (e: Exception) {
        false
    }

    private fun enqueue(loc: Location) {
        buffer.add(
            LocationPoint(
                lat = loc.latitude,
                lng = loc.longitude,
                speed = if (loc.hasSpeed()) loc.speed.toDouble() else null,
                heading = if (loc.hasBearing()) loc.bearing.toDouble() else null,
                accuracy = if (loc.hasAccuracy()) loc.accuracy.toDouble() else null,
                recordedAt = Instant.ofEpochMilli(loc.time).toString(),
            ),
        )
    }

    private suspend fun flush(id: String) {
        val points = buffer.nextBatch()
        if (points.isEmpty()) return
        when (val r = repository.uploadLocations(id, points)) {
            is ApiResult.Success -> LiveShareController.onUploaded(r.data.accepted)
            // Re-queue on a transient failure so fixes aren't lost (bounded — a
            // sustained failure drops the oldest rather than growing forever).
            is ApiResult.Failure -> buffer.requeue(points)
        }
    }

    override fun onDestroy() {
        tracker.stop()
        flushJob?.cancel()
        // Persist whatever is still buffered so the tail survives process death and
        // a failed final upload; the next live-ride start re-uploads it (see
        // recoverPendingFixes). Replaces the old onDestroy fire-and-forget upload,
        // which silently dropped the tail on any failure or early process kill.
        rideId?.let { id ->
            val remaining = buffer.drainAll()
            if (remaining.isNotEmpty()) persistPending(PendingFixes(id, remaining))
        }
        scope.cancel()
        LiveShareController.onStop()
        super.onDestroy()
    }

    // ── Crash-durable tail (persisted across process death) ──────────────────

    /** Upload a tail persisted by a previous session, then delete it on success;
     *  on failure leave the file so the next start retries. Best-effort. */
    private fun recoverPendingFixes() {
        val pending = readPending() ?: return
        if (pending.points.isEmpty()) {
            deletePending()
            return
        }
        scope.launch {
            when (repository.uploadLocations(pending.rideId, pending.points)) {
                is ApiResult.Success -> deletePending()
                is ApiResult.Failure -> Unit // keep the file; retry on the next start
            }
        }
    }

    private fun persistPending(pending: PendingFixes) {
        runCatching {
            // Merge with an unrecovered tail for the same ride, keeping the most
            // recent MAX_RETAINED so the file can't grow without bound either.
            val merged = readPending()
                ?.takeIf { it.rideId == pending.rideId }
                ?.let { (it.points + pending.points).takeLast(LocationBuffer.MAX_RETAINED) }
                ?: pending.points
            val json = (application as T2WApplication).container.json
            pendingFile().writeText(json.encodeToString(PendingFixes.serializer(), PendingFixes(pending.rideId, merged)))
        }
    }

    private fun readPending(): PendingFixes? = runCatching {
        val f = pendingFile()
        if (!f.exists()) return null
        val json = (application as T2WApplication).container.json
        json.decodeFromString(PendingFixes.serializer(), f.readText())
    }.getOrNull()

    private fun deletePending() {
        runCatching { pendingFile().delete() }
    }

    private fun pendingFile() = java.io.File(filesDir, PENDING_FILE)

    override fun onBind(intent: Intent?) = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Live ride tracking", NotificationManager.IMPORTANCE_LOW).apply {
                        description = "Shows while your location is shared during a live ride."
                    },
                )
            }
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, LiveLocationService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION") Notification.Builder(this)
        }
        return builder
            .setContentTitle("Sharing your location")
            .setContentText("Tales on 2 Wheels is tracking this live ride.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Stop", stopIntent).build())
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "live_location"
        private const val NOTIF_ID = 4201
        private const val EXTRA_RIDE_ID = "ride_id"
        private const val ACTION_STOP = "com.taleson2wheels.app.STOP_LIVE_SHARE"
        private const val FLUSH_MS = 8_000L
        private const val PENDING_FILE = "pending_live_fixes.json"

        fun start(context: Context, rideId: String) {
            val intent = Intent(context, LiveLocationService::class.java).putExtra(EXTRA_RIDE_ID, rideId)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LiveLocationService::class.java))
        }
    }
}
