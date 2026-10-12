package br.com.kotsniff.analyzer.parser

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class KotlinParserSyntaxTest :
    FunSpec({
        test("valid Kotlin code has no syntax errors") {
            val code =
                """
                class SampleSpec : io.kotest.core.spec.style.StringSpec({
                    "a test" {
                        val a = 1
                    }
                })
                """.trimIndent()
            KotlinParser.hasSyntaxErrors(KotlinParser.parse(code)) shouldBe false
        }

        test("unbalanced braces are reported as syntax errors") {
            val code =
                """
                package broken

                class BrokenSpec {
                    fun x() {
                        if (true) {
                            println("hi")
                """.trimIndent()
            KotlinParser.hasSyntaxErrors(KotlinParser.parse(code)) shouldBe true
        }
    })
