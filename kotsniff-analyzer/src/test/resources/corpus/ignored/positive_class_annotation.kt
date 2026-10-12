package corpus.ignored

import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

@Ignored
class IgnoredClassSpec : FunSpec({
    test("one") {
        1 shouldBe 1
    }
    test("two") {
        2 shouldBe 2
    }
})