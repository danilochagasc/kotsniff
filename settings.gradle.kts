plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "kotsniff"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(
    "kotsniff-core",
    "kotsniff-analyzer",
)