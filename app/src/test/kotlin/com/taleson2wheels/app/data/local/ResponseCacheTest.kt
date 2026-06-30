package com.taleson2wheels.app.data.local

import com.taleson2wheels.app.data.remote.ApiError
import com.taleson2wheels.app.data.remote.ApiResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ResponseCacheTest {

    @Serializable
    private data class Sample(val value: Int)

    /** In-memory [CacheStore] so the policy is testable without Room. */
    private class FakeStore : CacheStore {
        val map = mutableMapOf<String, String>()
        override suspend fun read(key: String): String? = map[key]
        override suspend fun write(key: String, json: String) { map[key] = json }
        override suspend fun delete(key: String) { map.remove(key) }
        override suspend fun clear() { map.clear() }
    }

    private val serializer = Sample.serializer()
    private fun cache(store: CacheStore) = ResponseCache(store, Json { ignoreUnknownKeys = true })

    @Test
    fun `success refreshes the cache and returns fresh data`() = runTest {
        val store = FakeStore()
        val result = cache(store).networkWithFallback("k", serializer) {
            ApiResult.Success(Sample(1))
        }
        assertEquals(Sample(1), (result as ApiResult.Success).data)
        assertEquals("""{"value":1}""", store.map["k"])
    }

    @Test
    fun `network failure falls back to the cached value`() = runTest {
        val store = FakeStore()
        val c = cache(store)
        // Seed the cache with a previous success.
        c.networkWithFallback("k", serializer) { ApiResult.Success(Sample(7)) }

        val result = c.networkWithFallback("k", serializer) {
            ApiResult.Failure(ApiError.Network(IOException("offline")))
        }
        assertEquals(Sample(7), (result as ApiResult.Success).data)
    }

    @Test
    fun `http failure is propagated and never masked by stale data`() = runTest {
        val store = FakeStore()
        val c = cache(store)
        c.networkWithFallback("k", serializer) { ApiResult.Success(Sample(7)) }

        val failure = ApiResult.Failure(ApiError.Http(status = 500, code = "INTERNAL", serverMessage = "boom"))
        val result = c.networkWithFallback("k", serializer) { failure }
        assertSame(failure, result)
    }

    @Test
    fun `network failure with an empty cache returns the failure`() = runTest {
        val store = FakeStore()
        val result = cache(store).networkWithFallback("k", serializer) {
            ApiResult.Failure(ApiError.Network(IOException("offline")))
        }
        assertTrue(result is ApiResult.Failure)
    }

    @Test
    fun `readOrNull tolerates an undecodable blob as a miss`() = runTest {
        val store = FakeStore().apply { map["k"] = "{ not valid json" }
        assertNull(cache(store).readOrNull("k", serializer))
    }

    @Test
    fun `invalidate drops only the named entry`() = runTest {
        val store = FakeStore()
        val cache = cache(store)
        cache.save("ride:abc", Sample(1), serializer)
        cache.save("rides:first", Sample(2), serializer)

        cache.invalidate("ride:abc")

        assertNull("the invalidated entry is gone", cache.readOrNull("ride:abc", serializer))
        assertEquals("other entries are untouched", Sample(2), cache.readOrNull("rides:first", serializer))
    }

    @Test
    fun `clear empties the store so nothing leaks across the auth boundary`() = runTest {
        val store = FakeStore()
        val cache = cache(store)
        cache.save("rides:first", Sample(1), serializer)
        cache.save("ride:abc", Sample(2), serializer)
        assertTrue(store.map.isNotEmpty())

        cache.clear()

        assertTrue(store.map.isEmpty())
        assertNull(cache.readOrNull("rides:first", serializer))
    }
}
