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
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "MiFleteCiudad"

// Lógica pura (precios, zona, validaciones): se puede testear sin el SDK de Android.
include(":core")

// La app Android. Con `-PcoreOnly` se omite para correr solo los tests del core
// en máquinas sin SDK de Android (por ejemplo, un servidor de CI mínimo).
if (!providers.gradleProperty("coreOnly").isPresent) {
    include(":app")
}
