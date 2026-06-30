package com.taleson2wheels.app.data.local

/**
 * Minimal key→JSON persistence seam. Kept as an interface (rather than calling
 * the Room DAO directly) so [ResponseCache] can be unit-tested on the JVM with a
 * trivial in-memory fake, no instrumentation required.
 */
interface CacheStore {
    suspend fun read(key: String): String?
    suspend fun write(key: String, json: String)
    suspend fun clear()
}

/**
 * Room-backed [CacheStore]. Stamps each write with the current wall clock and
 * enforces a max age so offline snapshots actually expire: reads older than
 * [maxAgeMillis] are treated as a miss, and each write opportunistically evicts
 * anything past the cutoff (otherwise stale rows would live forever).
 */
class RoomCacheStore(
    private val dao: CachedResponseDao,
    private val maxAgeMillis: Long = DEFAULT_MAX_AGE_MS,
    private val now: () -> Long = { System.currentTimeMillis() },
) : CacheStore {

    override suspend fun read(key: String): String? {
        val entry = dao.get(key) ?: return null
        if (now() - entry.updatedAt > maxAgeMillis) return null // stale → miss
        return entry.json
    }

    override suspend fun write(key: String, json: String) {
        val t = now()
        dao.put(CachedResponse(key = key, json = json, updatedAt = t))
        dao.evictOlderThan(t - maxAgeMillis)
    }

    override suspend fun clear() = dao.clearAll()

    private companion object {
        /** Offline snapshots are useful but should not be served indefinitely. */
        const val DEFAULT_MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000 // 7 days
    }
}
