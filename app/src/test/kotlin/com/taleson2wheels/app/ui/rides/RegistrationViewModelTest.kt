package com.taleson2wheels.app.ui.rides

import com.taleson2wheels.app.data.local.CacheStore
import com.taleson2wheels.app.data.local.ResponseCache
import com.taleson2wheels.app.data.remote.api.RidesApi
import com.taleson2wheels.app.data.remote.api.UploadApi
import com.taleson2wheels.app.data.remote.dto.Page
import com.taleson2wheels.app.data.remote.dto.RegField
import com.taleson2wheels.app.data.remote.dto.RegPaymentConfig
import com.taleson2wheels.app.data.remote.dto.RegisterRideRequest
import com.taleson2wheels.app.data.remote.dto.RegistrationConfig
import com.taleson2wheels.app.data.remote.dto.RideCard
import com.taleson2wheels.app.data.remote.dto.RideDetail
import com.taleson2wheels.app.data.remote.dto.RideDetailResponse
import com.taleson2wheels.app.data.remote.dto.RidePost
import com.taleson2wheels.app.data.remote.dto.RidePostInput
import com.taleson2wheels.app.data.remote.dto.RidePostResponse
import com.taleson2wheels.app.data.remote.dto.RideRegistrationDto
import com.taleson2wheels.app.data.remote.dto.RideRegistrationResponse
import com.taleson2wheels.app.data.remote.dto.UploadResponse
import com.taleson2wheels.app.data.repository.RidesRepository
import com.taleson2wheels.app.data.repository.UploadRepository
import com.taleson2wheels.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Drives [RegistrationViewModel] against fakes to verify the PR4d dynamic form:
 * the config is loaded from the ride detail, required fields + required agreements
 * gate submission, and the field values map back onto the register request.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegistrationViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val json = Json { ignoreUnknownKeys = true }

    private class FakeStore : CacheStore {
        val map = mutableMapOf<String, String>()
        override suspend fun read(key: String): String? = map[key]
        override suspend fun write(key: String, json: String) { map[key] = json }
        override suspend fun delete(key: String) { map.remove(key) }
        override suspend fun clear() { map.clear() }
    }

    private class FakeRidesApi(private val config: RegistrationConfig?) : RidesApi {
        var lastBody: RegisterRideRequest? = null

        override suspend fun detail(id: String): RideDetailResponse =
            RideDetailResponse(RideDetail(id = id, title = "Coorg Run", fee = 2500.0, registrationConfig = config))

        override suspend fun register(id: String, body: RegisterRideRequest): RideRegistrationResponse {
            lastBody = body
            return RideRegistrationResponse(
                RideRegistrationDto(id = "reg1", rideId = id, approvalStatus = "pending", confirmationCode = "ABC123"),
            )
        }

        override suspend fun list(cursor: String?, limit: Int, status: String?): Page<RideCard> = error("unused")
        override suspend fun posts(id: String, cursor: String?, limit: Int): Page<RidePost> = error("unused")
        override suspend fun createPost(id: String, body: RidePostInput): RidePostResponse = error("unused")
    }

    private class FakeUploadApi : UploadApi {
        override suspend fun upload(file: MultipartBody.Part, type: RequestBody?): UploadResponse = error("unused")
    }

    private fun vm(config: RegistrationConfig?): Pair<RegistrationViewModel, FakeRidesApi> {
        val api = FakeRidesApi(config)
        val rides = RidesRepository(api, json, ResponseCache(FakeStore(), json))
        val model = RegistrationViewModel(rides, UploadRepository(FakeUploadApi(), json))
        return model to api
    }

    private val config = RegistrationConfig(
        fields = listOf(
            RegField("phone", "Phone", "tel", required = true),
            RegField("foodPreference", "Food", "select", options = listOf("vegetarian", "non-vegetarian")),
        ),
        requireCancellationAgreement = true,
        requireIndemnity = true,
        payment = RegPaymentConfig(mode = "screenshot", fee = 2500.0),
    )

    @Test
    fun loads_the_config_from_the_ride_detail() = runTest(mainDispatcher.dispatcher) {
        val (model, _) = vm(config)
        model.load("r1"); advanceUntilIdle()
        assertFalse(model.uiState.isLoadingConfig)
        assertEquals(listOf("phone", "foodPreference"), model.uiState.config?.fields?.map { it.key })
    }

    @Test
    fun cannot_submit_until_required_fields_and_agreements_are_satisfied() = runTest(mainDispatcher.dispatcher) {
        val (model, _) = vm(config)
        model.load("r1"); advanceUntilIdle()
        assertFalse("nothing filled", model.uiState.canSubmit)

        model.onFieldChange("phone", "9999999999")
        assertFalse("required agreements still unchecked", model.uiState.canSubmit)

        model.onAgreeCancellation(true)
        model.onAgreeIndemnity(true)
        assertTrue("phone + both agreements → submittable", model.uiState.canSubmit)
    }

    @Test
    fun submit_maps_field_values_onto_the_register_request() = runTest(mainDispatcher.dispatcher) {
        val (model, api) = vm(config)
        model.load("r1"); advanceUntilIdle()
        model.onFieldChange("phone", "9999999999")
        model.onFieldChange("foodPreference", "vegetarian")
        model.onAgreeCancellation(true)
        model.onAgreeIndemnity(true)

        model.submit("r1"); advanceUntilIdle()

        val body = api.lastBody!!
        assertEquals("9999999999", body.phone)
        assertEquals("vegetarian", body.foodPreference)
        assertTrue(body.agreedCancellationTerms)
        assertTrue(body.agreedIndemnity)
        // Accommodation is assigned server-side — never sent from the form.
        assertNull(body.accommodationType)
        assertEquals("ABC123", model.uiState.confirmationCode)
    }

    @Test
    fun a_config_less_ride_falls_back_to_a_default_form() = runTest(mainDispatcher.dispatcher) {
        val (model, _) = vm(null) // backend without registrationConfig
        model.load("r1"); advanceUntilIdle()
        val cfg = model.uiState.config
        assertTrue("fallback still collects phone", cfg?.fields?.any { it.key == "phone" } == true)
        assertTrue("fallback keeps the agreements", cfg?.requireIndemnity == true)
    }
}
