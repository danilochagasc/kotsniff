package br.com.kotsniff.cli.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class GlobMatcherTest :
    FunSpec({
        test("matches ** across directories") {
            val matcher = GlobMatcher(listOf("**/build/**"))
            matcher.matches("module/build/generated/File.kt") shouldBe true
            matcher.matches("build/generated/File.kt") shouldBe true
            matcher.matches("src/main/File.kt") shouldBe false
        }

        test("**/ matches a top-level file") {
            val matcher = GlobMatcher(listOf("**/Probe.kt"))
            matcher.matches("Probe.kt") shouldBe true
            matcher.matches("a/b/Probe.kt") shouldBe true
        }

        test("single star does not cross separators") {
            val matcher = GlobMatcher(listOf("src/*.kt"))
            matcher.matches("src/File.kt") shouldBe true
            matcher.matches("src/nested/File.kt") shouldBe false
        }

        test("no patterns matches nothing") {
            GlobMatcher(emptyList()).matches("anything.kt") shouldBe false
        }
    })
