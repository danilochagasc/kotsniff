package br.com.kotsniff.cli.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class HtmlEscapeTest :
    FunSpec({
        test("escapes HTML metacharacters") {
            HtmlEscape.escape("<b>&\"'</b>") shouldBe "&lt;b&gt;&amp;&quot;&#39;&lt;/b&gt;"
        }

        test("neutralises a closing script tag") {
            HtmlEscape
                .escape("</script><script>alert(1)</script>")
                .contains("</script>") shouldBe false
        }

        test("escapes JavaScript string literals") {
            HtmlEscape.escapeJs("a\"b\\c\n<'&") shouldBe "a\\\"b\\\\c\\n\\u003c\\'\\u0026"
        }
    })
