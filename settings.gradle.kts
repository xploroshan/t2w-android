pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        // Mapbox's private Maven repo — serves the Maps SDK for the Relive 3D
        // flyover. Authenticated with a SECRET downloads token (username is the
        // literal "mapbox"; the password is a Mapbox token with the Downloads:Read
        // scope). Supply it as MAPBOX_DOWNLOADS_TOKEN in ~/.gradle/gradle.properties
        // (or the env var) — NEVER commit it. The content filter scopes this repo
        // to com.mapbox.* only, so with no mapbox dependency requested (and no
        // token) the repo is never queried and the build stays green.
        // See docs/SETUP_SECRETS.md.
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication { create<BasicAuthentication>("basic") }
            credentials {
                username = "mapbox"
                password = providers.gradleProperty("MAPBOX_DOWNLOADS_TOKEN").orNull
                    ?: System.getenv("MAPBOX_DOWNLOADS_TOKEN")
                    ?: ""
            }
            content { includeGroupByRegex("com\\.mapbox.*") }
        }
    }
}

rootProject.name = "t2w-android"
include(":app")
include(":macrobenchmark")
