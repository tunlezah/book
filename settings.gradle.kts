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
    }
}

rootProject.name = "Dogear"

include(":app")
include(":core:model")
include(":core:common")
include(":core:ui")
include(":core:database")
include(":core:datastore")
include(":core:cover")
include(":core:ingest")
include(":feature:library")
include(":feature:reader")
include(":feature:upload")
include(":feature:settings")
include(":format:api")
include(":format:epub")
include(":format:pdf")
include(":format:comic")
include(":format:text")
include(":format:fb2")
include(":format:html")
