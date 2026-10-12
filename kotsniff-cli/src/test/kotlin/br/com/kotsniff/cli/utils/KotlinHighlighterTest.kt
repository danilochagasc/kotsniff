package br.com.kotsniff.cli.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class KotlinHighlighterTest :
    FunSpec({
        fun hl(vararg lines: String): List<String> = KotlinHighlighter.highlight(lines.toList())

        test("highlights keywords, numbers and types") {
            val out = hl("val x: Int = 42").single()
            out shouldContain "tok-keyword\">val<"
            out shouldContain "tok-number\">42<"
            out shouldContain "tok-type\">Int<"
        }

        test("wraps and escapes string literals") {
            val out = hl("val s = \"a<b>&c\"").single()
            out shouldContain "tok-string"
            out shouldContain "&quot;a&lt;b&gt;&amp;c&quot;"
            out shouldNotContain "<b>"
        }

        test("highlights line comments and escapes them") {
            val out = hl("// note <img src=x>").single()
            out shouldContain "tok-comment\">// note &lt;img src=x&gt;<"
            out shouldNotContain "<img"
        }

        test("carries block-comment state across lines") {
            val out = hl("/* start", "still */ val x = 1")
            out[0] shouldContain "tok-comment\">/* start<"
            out[1] shouldContain "tok-comment\">still */<"
            out[1] shouldContain "tok-keyword\">val<"
        }

        test("carries raw-string state across lines") {
            val out = hl("val s = \"\"\"abc", "def\"\"\" + 1")
            out[0] shouldContain "tok-string"
            out[1] shouldContain "tok-string\">def&quot;&quot;&quot;<"
        }

        test("annotations are highlighted") {
            val out = hl("@Ignored class Foo").single()
            out shouldContain "tok-annotation\">@Ignored<"
            out shouldContain "tok-keyword\">class<"
        }

        test("never emits raw angle brackets from source") {
            val out = hl("val s = \"</script><script>alert(1)</script>\"").single()
            out shouldNotContain "</script><script>"
            out shouldContain "&lt;/script&gt;"
        }
    })
