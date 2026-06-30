package com.taleson2wheels.app.data.push

/**
 * Supplies the current push (FCM) token for this device, or null when push isn't
 * configured. Kept as an interface so Firebase Messaging can be plugged in later
 * (add the SDK + google-services.json, then return `FirebaseMessaging.getInstance()
 * .token.await()`) without touching the auth/session wiring.
 */
interface PushTokenProvider {
    suspend fun currentToken(): String?
}

/** Default — no push backend wired yet, so device registration is a no-op. */
class NoOpPushTokenProvider : PushTokenProvider {
    override suspend fun currentToken(): String? = null
}
