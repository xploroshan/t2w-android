package com.taleson2wheels.app.di

import android.content.Context
import android.provider.Settings
import com.taleson2wheels.app.BuildConfig
import com.taleson2wheels.app.data.remote.AuthInterceptor
import com.taleson2wheels.app.data.remote.TokenAuthenticator
import com.taleson2wheels.app.data.remote.api.AuthApi
import com.taleson2wheels.app.data.remote.api.ContentApi
import com.taleson2wheels.app.data.remote.api.GarageApi
import com.taleson2wheels.app.data.remote.api.RidersApi
import com.taleson2wheels.app.data.remote.api.RidesApi
import com.taleson2wheels.app.data.remote.api.UploadApi
import com.taleson2wheels.app.data.repository.AuthRepository
import com.taleson2wheels.app.data.repository.CatalogRepository
import com.taleson2wheels.app.data.repository.RidersRepository
import com.taleson2wheels.app.data.repository.RidesRepository
import com.taleson2wheels.app.data.repository.UploadRepository
import com.taleson2wheels.app.data.session.SessionStore
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Hand-rolled dependency container (no Hilt/KSP — keeps the scaffold's build
 * lean and annotation-processor-free). One instance lives on the Application
 * and owns every long-lived singleton: JSON, OkHttp, Retrofit, the typed
 * `/api/v1` services, and the repositories.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val json: Json = Json {
        ignoreUnknownKeys = true   // tolerate server fields the app doesn't model yet
        explicitNulls = false      // omit nulls when serializing request bodies
        coerceInputValues = true   // fall back to defaults for nulls on non-null fields
        encodeDefaults = true      // actually send defaulted request fields (e.g. platform="android")
        isLenient = true
    }

    val session = SessionStore(appContext)

    private val converterFactory = json.asConverterFactory("application/json".toMediaType())

    private val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    /**
     * Bare client used only for token refresh and other unauthenticated calls.
     * It carries neither the [AuthInterceptor] nor the [TokenAuthenticator], so
     * a failing refresh can't recurse.
     */
    private val plainClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

    private val refreshRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(plainClient)
        .addConverterFactory(converterFactory)
        .build()

    private val refreshApi: AuthApi = refreshRetrofit.create(AuthApi::class.java)

    /** Authenticated client: injects the bearer token and refreshes on 401. */
    private val authedClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(AuthInterceptor(session))
        .authenticator(TokenAuthenticator(session, refreshApi))
        .addInterceptor(logging)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(authedClient)
        .addConverterFactory(converterFactory)
        .build()

    // ── Typed /api/v1 services ───────────────────────────────────────────────
    val authApi: AuthApi = retrofit.create(AuthApi::class.java)
    val ridesApi: RidesApi = retrofit.create(RidesApi::class.java)
    val ridersApi: RidersApi = retrofit.create(RidersApi::class.java)
    val garageApi: GarageApi = retrofit.create(GarageApi::class.java)
    val contentApi: ContentApi = retrofit.create(ContentApi::class.java)
    val uploadApi: UploadApi = retrofit.create(UploadApi::class.java)

    // ── Repositories ─────────────────────────────────────────────────────────
    private val deviceId: String = runCatching {
        Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
    }.getOrNull().orEmpty().ifBlank { "unknown-device" }

    val authRepository = AuthRepository(authApi, session, json, deviceId)
    val ridesRepository = RidesRepository(ridesApi, json)
    val ridersRepository = RidersRepository(ridersApi, json)
    val catalogRepository = CatalogRepository(contentApi, json)
    val uploadRepository = UploadRepository(uploadApi, json)
}
