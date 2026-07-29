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

rootProject.name = "UVCCamera"
include(":lib")

// Sample apps — moved from root to samples/
val sampleModules = listOf(
    "usbCameraCommon",
    "usbCameraTest",
    "usbCameraTest0",
    "usbCameraTest2",
    "usbCameraTest3",
    "usbCameraTest4",
    "usbCameraTest5",
    "usbCameraTest6",
    "usbCameraTest7",
    "usbCameraTest8"
)
sampleModules.forEach { name ->
    include(":$name")
    project(":$name").projectDir = file("samples/$name")
}
