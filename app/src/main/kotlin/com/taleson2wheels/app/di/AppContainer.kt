package com.taleson2wheels.app.di

import android.content.Context
import android.provider.Settings
import com.taleson2wheels.app.BuildConfig
import com.taleson2wheels.app.data.remote.AuthInterceptor
import com.taleson2wheels.app.data.remote.TokenAuthenticator
import com.taleson2wheels.app.data.local.AppDatabase
import com.taleson2wheels.app.data.local.ResponseCache
import com.taleson2wheels.app.data.local.RoomCacheStore
import com.taleson2wheels.app.data.push.NoOpPushTokenProvider
import com.taleson2wheels.app.data.push.PushTokenProvider
import com.taleson2wheels.app.data.remote.api.AuthApi
import com.taleson2wheels.app.data.remote.api.BlogsApi
import com.taleson2wheels.app.data.remote.api.ContentApi
import com.taleson2wheels.app.data.remote.api.DevicesApi
import com.taleson2wheels.app.data.remote.api.GarageApi
import com.taleson2wheels.app.data.remote.api.LiveApi
import com.taleson2wheels.app.data.remote.api.RidersApi
import com.taleson2wheels.app.data.remote.api.RidesApi
import com.taleson2wheels.app.data.remote.api.UploadApi
import com.taleson2wheels.app.data.remote.api.AdminApi
import com.taleson2wheels.app.data.repository.AdminRepository
import com.taleson2wheels.app.data.repository.AuthRepository
import com.taleson2wheels.app.data.repository.BlogsRepository
import com.taleson2wheels.app.data.repository.CatalogRepository
import com.taleson2wheels.app.data.repository.DevicesRepository
import com.taleson2wheels.app.data.repository.GarageRepository
import com.taleson2wheels.app.data.repository.LiveRepository
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
        // Hard ceiling on the WHOLE call. connect/read only bound individual
        // socket phases (idle gaps between bytes), so a byte-trickling server or
        // a long redirect chain could otherwise hang a request indefinitely —
        // and on the refresh path that stall is held under the synchronized
        // refresh lock, blocking every queued 401 retry behind it.
        .callTimeout(45, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

    private val refreshRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(plainClient)
        .addConverterFactory(converterFactory)
        .build()

    private val refreshApi: AuthApi = refreshRetrofit.create(AuthApi::class.java)

    // Local store backing the offline response cache. Declared before the
    // authenticated client so the TokenAuthenticator can wipe it on a forced
    // logout (refresh failure), matching AuthRepository.logout().
    private val database = AppDatabase.build(appContext)
    val responseCache = ResponseCache(RoomCacheStore(database.cachedResponses()), json)

    /** Authenticated client: injects the bearer token and refreshes on 401. */
    private val authedClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        // Multipart image uploads (avatars, ride/blog media) need more than
        // OkHttp's 10s default write window on slow links, or they fail as a
        // bogus "network unavailable".
        .writeTimeout(60, TimeUnit.SECONDS)
        // Whole-call ceiling (>= the 60s upload write window so legitimate slow
        // uploads still complete) so no authenticated request can hang forever.
        .callTimeout(90, TimeUnit.SECONDS)
        .addInterceptor(AuthInterceptor(session))
        .authenticator(TokenAuthenticator(session, refreshApi, responseCache))
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
    val liveApi: LiveApi = retrofit.create(LiveApi::class.java)
    val devicesApi: DevicesApi = retrofit.create(DevicesApi::class.java)
    val blogsApi: BlogsApi = retrofit.create(BlogsApi::class.java)
    val adminApi: AdminApi = retrofit.create(AdminApi::class.java)

    // ── Repositories ─────────────────────────────────────────────────────────
    private val deviceId: String = runCatching {
        Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
    }.getOrNull().orEmpty().ifBlank { "unknown-device" }

    val devicesRepository = DevicesRepository(devicesApi, json)

    /** Push-token source. Swap for an FCM-backed impl once Firebase is wired. */
    val pushTokenProvider: PushTokenProvider = NoOpPushTokenProvider()

    val authRepository = AuthRepository(
        authApi = authApi,
        session = session,
        json = json,
        deviceId = deviceId,
        devicesRepository = devicesRepository,
        pushTokenProvider = pushTokenProvider,
        appBuild = BuildConfig.VERSION_NAME,
        responseCache = responseCache,
    )
    val ridesRepository = RidesRepository(ridesApi, json, responseCache)
    val ridersRepository = RidersRepository(ridersApi, json)
    val catalogRepository = CatalogRepository(contentApi, json)
    val uploadRepository = UploadRepository(uploadApi, json)
    val garageRepository = GarageRepository(garageApi, json)
    val liveRepository = LiveRepository(liveApi, json)
    val blogsRepository = BlogsRepository(blogsApi, json)
    val adminRepository = AdminRepository(adminApi, json)
}
