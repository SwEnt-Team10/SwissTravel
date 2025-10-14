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
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials {
                // used for private mapbox dependencies such as
                // "com.mapbox.navigationcore:android-ndk27:3.16.0-beta.1"
                username = "mapbox"
                password = System.getenv("MAPBOX_DOWNLOADS_TOKEN")
            }
        }
    }
}

rootProject.name = "SwissTravel"
include(":app")
 