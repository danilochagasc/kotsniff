package corpus.conditional

import io.kotest.core.spec.style.FunSpec

class WhenStatementSpec : FunSpec({
    test("branches") {
        when (value()) {
            1 -> println("one")
            else -> println("other")
        }
    }
})