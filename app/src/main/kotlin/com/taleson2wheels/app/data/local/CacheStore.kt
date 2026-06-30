package com.taleson2wheels.app.data.local

/**
 * Minimal key→JSON persistence seam. Kept as an interface (rather than calling
 * the Room DAO directly) so [ResponseCache] can be unit-tested on the JVM with a
 * trivial in-memory fake, no instrumentation required.
 */
interface CacheStore {
    suspend fun read(key: String): String?
    suspend fun write(key: String, json: String)
}

/** Room-backed [CacheStore]. Stamps each write with the current wall clock. */
class RoomCacheStore(
    private val dao: CachedResponseDao,
    private val now: () -> Long = { System.currentTimeMillis() },
) : CacheStore {

    override suspend fun read(key: String): String? = dao.get(key)?.json

    override suspend fun write(key: String, json: String) {
        dao.put(CachedResponse(key = key, json = json, updatedAt = now()))
    }
}
