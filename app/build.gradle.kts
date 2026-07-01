import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.paparazzi)
}

// Base URL for the T2W API. This is the HOST ROOT — the Retrofit service paths
// already carry the `api/v1/` prefix, so the value must NOT include it or every
// request would hit a doubled `/api/v1/api/v1/...`. Override per-machine by adding
//   T2W_API_BASE_URL=https://your-host/       (host root + trailing slash, no /api/v1)
// to a local `secrets.properties` (git-ignored) without editing this file.
val secrets = Properties().apply {
    val f = rootProject.file("secrets.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun secretOr(key: String, default: String): String =
    (secrets.getProperty(key) ?: providers.gradleProperty(key).orNull ?: default)

// Release signing — driven by a git-ignored `keystore.properties` (storeFile,
// storePassword, keyAlias, keyPassword). Absent (e.g. CI without secrets) → the
// release build falls back to debug signing so it still assembles.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val hasReleaseSigning = keystoreProps.getProperty("storeFile") != null

// Fail a RELEASE build that has no real keystore, rather than silently shipping
// a "release" signed with the PUBLIC debug key (Play rejects it, and a sideloaded
// debug-signed update can be spoofed by anyone holding the public debug key).
// The fail-fast is opt-out via -PallowDebugSigningRelease=true so CI / deliberate
// unsigned local builds can still assemble (then they only get a loud warning).
// Provide keystore.properties to sign properly (see docs/SETUP_SECRETS.md).
run {
    val buildingRelease = gradle.startParameter.taskNames.any { it.contains("elease") }
    val allowDebugSigned = (providers.gradleProperty("allowDebugSigningRelease").orNull == "true")
    if (buildingRelease && !hasReleaseSigning) {
        if (allowDebugSigned) {
            logger.warn(
                "\n⚠️  RELEASE build with no keystore.properties — falling back to the PUBLIC " +
                    "debug signing key (allowDebugSigningRelease=true). This APK/AAB is NOT " +
                    "distributable. See docs/SETUP_SECRETS.md.\n",
            )
        } else {
            throw GradleException(
                "RELEASE build requested without keystore.properties. A debug-signed release is " +
                    "not distributable (Play rejects it; sideloaded updates are spoofable). Provide " +
                    "keystore.properties (see docs/SETUP_SECRETS.md), or pass " +
                    "-PallowDebugSigningRelease=true to build an explicitly non-distributable artifact.",
            )
        }
    }
}

// The live-ride Google Map silently renders blank without a Maps key. That's an
// intentional, documented optional config (docs/HARDENING.md), so a keyless
// release still builds — but warn loudly so a real release isn't shipped with a
// broken map by accident. Unlike the keystore this is NOT a hard fail: a blank
// map is graceful degradation, not an undistributable/spoofable artifact.
val mapsApiKey = secretOr("MAPS_API_KEY", "")
run {
    val buildingRelease = gradle.startParameter.taskNames.any { it.contains("elease") }
    if (buildingRelease && mapsApiKey.isBlank()) {
        logger.warn(
            "\n⚠️  RELEASE build with no MAPS_API_KEY — the live-ride map will render blank. " +
                "Provide it via secrets.properties (MAPS_API_KEY=...) or -PMAPS_API_KEY=... before " +
                "shipping. See docs/HARDENING.md.\n",
        )
    }
}

// Mapbox PUBLIC access token (pk.*) for the Relive 3D flyover — read at runtime
// (BuildConfig.MAPBOX_ACCESS_TOKEN) and set on MapboxOptions before a MapView is
// created. Like MAPS_API_KEY this is optional/graceful (the Relive screen shows a
// "set your Mapbox token" hint when blank), so a token-free build still assembles
// — but warn loudly on a release. NOTE: this is the RUNTIME token, separate from
// the build-time MAPBOX_DOWNLOADS_TOKEN (secret sk.*) that authenticates the
// Mapbox Maven repo in settings.gradle.kts. See docs/SETUP_SECRETS.md.
val mapboxToken = secretOr("MAPBOX_ACCESS_TOKEN", "")
run {
    val buildingRelease = gradle.startParameter.taskNames.any { it.contains("elease") }
    if (buildingRelease && mapboxToken.isBlank()) {
        logger.warn(
            "\n⚠️  RELEASE build with no MAPBOX_ACCESS_TOKEN — the Relive 3D flyover will show a " +
                "\"set token\" placeholder instead of the map. Provide it via secrets.properties " +
                "(MAPBOX_ACCESS_TOKEN=pk...) or -PMAPBOX_ACCESS_TOKEN=... before shipping. " +
                "See docs/SETUP_SECRETS.md.\n",
        )
    }
}

android {
    namespace = "com.taleson2wheels.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.taleson2wheels.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Google Maps key for the live-ride map. Supply via secrets.properties
        // (MAPS_API_KEY=...) or -PMAPS_API_KEY=...; empty by default so the build
        // works without it (the map just won't render — a release with it empty
        // gets a loud warning above). See docs/HARDENING.md.
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Host root — service paths carry the `api/v1/` prefix (matches the
            // OpenAPI servers). The emulator reaches the host's localhost via 10.0.2.2.
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${secretOr("T2W_API_BASE_URL_DEBUG", "http://10.0.2.2:3000/")}\"",
            )
            buildConfigField("String", "MAPBOX_ACCESS_TOKEN", "\"$mapboxToken\"")
        }
        release {
            // Real release keystore when configured; otherwise debug-signed so CI
            // and unsigned local release builds still assemble.
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${secretOr("T2W_API_BASE_URL", "https://taleson2wheels.com/")}\"",
            )
            buildConfigField("String", "MAPBOX_ACCESS_TOKEN", "\"$mapboxToken\"")
        }
        // A release-like variant for Macrobenchmark: minified/shrunk like release
        // (so startup numbers reflect production) but debug-signed and marked
        // profileable (see src/benchmark/AndroidManifest.xml) so the perf harness
        // can attach without a debuggable build. Measured by the :macrobenchmark
        // module on the perf CI; assembles locally with no device.
        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
            isDebuggable = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Paparazzi screenshot tests live in src/test but must NOT run during the normal
// unit-test task: their goldens are pixel-exact and OS/JVM-specific, which would
// make `testDebugUnitTest` fail on a dev's Mac or any host that doesn't match the
// (Linux) machine the goldens were recorded on. They run ONLY via the dedicated
// record/verifyPaparazziDebug tasks. Regenerate the gallery with:
//   ./gradlew :app:recordPaparazziDebug
val isPaparazziRun = gradle.startParameter.taskNames.any { it.contains("Paparazzi", ignoreCase = true) }
tasks.withType<Test>().configureEach {
    if (!isPaparazziRun) {
        exclude("**/paparazzi/**")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Networking → /api/v1
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Token storage + image loading
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)

    // Maps — live-ride map (renders once a Maps API key is supplied)
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)

    // Mapbox Maps SDK — the Relive 3D-terrain flyover (satellite + DEM terrain +
    // animated track playback). Resolves from Mapbox's private Maven repo, which
    // needs the secret MAPBOX_DOWNLOADS_TOKEN configured (settings.gradle.kts +
    // docs/SETUP_SECRETS.md). Runtime tiles use the public MAPBOX_ACCESS_TOKEN.
    implementation(libs.mapbox.maps)

    // Offline cache (Room + KSP processor)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
