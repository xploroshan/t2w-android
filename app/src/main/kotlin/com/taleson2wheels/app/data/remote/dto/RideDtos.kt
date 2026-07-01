package com.taleson2wheels.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * List-view ride (`GET /rides` → `Page<RideCard>`). `status` is computed
 * server-side; `myRegistrationStatus` is null unless the bearer viewer is
 * registered.
 */
@Serializable
data class RideCard(
    val id: String,
    val title: String,
    val rideNumber: String? = null,
    val type: String? = null,
    val status: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val distanceKm: Double = 0.0,
    val difficulty: String? = null,
    val fee: Double = 0.0,
    val posterUrl: String? = null,
    val registeredRiders: Int = 0,
    val activeRegistrations: Int = 0,
    val myRegistrationStatus: String? = null,
)

/**
 * Detail-view ride (`GET /rides/{id}` → `{ "ride": RideDetail }`). A superset of
 * [RideCard] with crew, registrations and the viewer's status; the backend marks
 * it `additionalProperties: true`, so unmodeled fields are tolerated.
 */
@Serializable
data class RideDetail(
    val id: String,
    val title: String,
    val rideNumber: String? = null,
    val type: String? = null,
    val status: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val startLocation: String? = null,
    val endLocation: String? = null,
    val distanceKm: Double = 0.0,
    val difficulty: String? = null,
    val description: String? = null,
    val fee: Double = 0.0,
    val maxRiders: Int = 0,
    val posterUrl: String? = null,
    val registeredRiders: Int = 0,
    val activeRegistrations: Int = 0,
    val currentUserRegistered: Boolean = false,
    val currentUserApprovalStatus: String? = null,
    /** Server-resolved dynamic registration form config (see [RegistrationConfig]). */
    val registrationConfig: RegistrationConfig? = null,
)

/** `/rides/{id}` wraps the ride in `{ "ride": ... }`. */
@Serializable
data class RideDetailResponse(val ride: RideDetail)

/**
 * Flattened, client-ready registration form config the backend resolves per ride
 * (`RideDetail.registrationConfig`). The registration screen renders its form from
 * this: only [fields] are shown, `select` fields use their [RegField.options], and
 * the agreement + payment sections follow the flags below.
 */
@Serializable
data class RegistrationConfig(
    val fields: List<RegField> = emptyList(),
    val requireCancellationAgreement: Boolean = true,
    val requireIndemnity: Boolean = true,
    val cancellationText: String = "",
    val payment: RegPaymentConfig = RegPaymentConfig(),
)

/** One field in the dynamic registration form. `key` matches [RegisterRideRequest]. */
@Serializable
data class RegField(
    val key: String,
    val label: String,
    val type: String, // "text" | "tel" | "email" | "select"
    val required: Boolean = false,
    val options: List<String> = emptyList(),
)

@Serializable
data class RegPaymentConfig(
    val mode: String = "none", // "screenshot" | "transaction_id" | "both" | "none"
    val fee: Double = 0.0,
    val upiIds: List<RegUpiId> = emptyList(),
    val bankAccounts: List<RegBankAccount> = emptyList(),
)

@Serializable
data class RegUpiId(val label: String = "", val id: String = "")

@Serializable
data class RegBankAccount(val label: String = "", val details: String = "")

/** Body for `POST /rides/{id}/register` — all fields optional; server fills defaults. */
@Serializable
data class RegisterRideRequest(
    val riderName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val bloodGroup: String? = null,
    val foodPreference: String? = null,
    val ridingType: String? = null,
    val referredBy: String? = null,
    val vehicleModel: String? = null,
    val vehicleRegNumber: String? = null,
    val tshirtSize: String? = null,
    val accommodationType: String? = null,
    val upiTransactionId: String? = null,
    val paymentScreenshot: String? = null,
    val agreedCancellationTerms: Boolean = false,
    val agreedIndemnity: Boolean = false,
)

@Serializable
data class RideRegistrationDto(
    val id: String,
    val rideId: String,
    val approvalStatus: String,
    val accommodationType: String? = null,
    val confirmationCode: String? = null,
    val registeredAt: String? = null,
)

/** `/rides/{id}/register` wraps the result in `{ "registration": ... }`. */
@Serializable
data class RideRegistrationResponse(val registration: RideRegistrationDto)

/**
 * A "ride tale" — a community post attached to a ride
 * (`GET /rides/{id}/posts` → `Page<RidePost>`). The feed only returns approved
 * posts; a freshly created post may be `pending` until a moderator approves it.
 */
@Serializable
data class RidePost(
    val id: String,
    val rideId: String,
    val authorId: String? = null,
    val authorName: String = "",
    val authorAvatar: String? = null,
    val content: String = "",
    val images: List<String> = emptyList(),
    val approvalStatus: String? = null,
    val approvedBy: String? = null,
    val createdAt: String? = null,
)

/** Body for `POST /rides/{id}/register`'s sibling `POST /rides/{id}/posts`. */
@Serializable
data class RidePostInput(
    val content: String,
    val images: List<String> = emptyList(),
)

/** `/rides/{id}/posts` (create) wraps the new post in `{ "post": ... }`. */
@Serializable
data class RidePostResponse(val post: RidePost)
