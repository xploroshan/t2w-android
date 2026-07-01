package com.taleson2wheels.app.data.repository

import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.api.ContentApi
import com.taleson2wheels.app.data.remote.dto.AchievementsResponse
import com.taleson2wheels.app.data.remote.dto.BadgesResponse
import com.taleson2wheels.app.data.remote.dto.ContactRequest
import com.taleson2wheels.app.data.remote.dto.ContactResponse
import com.taleson2wheels.app.data.remote.dto.CrewResponse
import com.taleson2wheels.app.data.remote.dto.GuidelinesResponse
import com.taleson2wheels.app.data.remote.dto.HealthDto
import com.taleson2wheels.app.data.remote.dto.MarkReadRequest
import com.taleson2wheels.app.data.remote.dto.MarkReadResponse
import com.taleson2wheels.app.data.remote.dto.NotificationsResponse
import com.taleson2wheels.app.data.remote.dto.StatsDto
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class CatalogRepositoryContactTest {

    private val json = Json { ignoreUnknownKeys = true }

    private class FakeContentApi(
        val response: () -> ContactResponse,
    ) : ContentApi {
        var lastContact: ContactRequest? = null

        override suspend fun contact(body: ContactRequest): ContactResponse {
            lastContact = body
            return response()
        }

        override suspend fun health(): HealthDto = error("unused")
        override suspend fun stats(): StatsDto = error("unused")
        override suspend fun guidelines(): GuidelinesResponse = error("unused")
        override suspend fun crew(): CrewResponse = error("unused")
        override suspend fun badges(): BadgesResponse = error("unused")
        override suspend fun achievements(): AchievementsResponse = error("unused")
        override suspend fun notifications(): NotificationsResponse = error("unused")
        override suspend fun markNotificationsRead(body: MarkReadRequest): MarkReadResponse = error("unused")
    }

    @Test
    fun `submitContact posts the form and returns the server success flag`() = runTest {
        val api = FakeContentApi { ContactResponse(success = true) }
        val repo = CatalogRepository(api, json)

        val result = repo.submitContact("Aditi", "aditi@example.com", "Ride query", "When is the next one?")

        assertTrue(result is ApiResult.Success && result.data)
        assertEquals(
            ContactRequest("Aditi", "aditi@example.com", "Ride query", "When is the next one?"),
            api.lastContact,
        )
    }

    @Test
    fun `submitContact surfaces a false success flag without throwing`() = runTest {
        val repo = CatalogRepository(FakeContentApi { ContactResponse(success = false) }, json)
        val result = repo.submitContact("A", "a@b.com", "s", "m")
        assertTrue(result is ApiResult.Success && !result.data)
    }

    @Test
    fun `submitContact maps a network error to a Failure`() = runTest {
        val repo = CatalogRepository(FakeContentApi { throw IOException("offline") }, json)
        val result = repo.submitContact("A", "a@b.com", "s", "m")
        assertTrue(result is ApiResult.Failure)
    }
}
