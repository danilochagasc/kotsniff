package corpus.sleepy

import io.kotest.core.spec.style.FunSpec

class ThreadSleepSpec : FunSpec({
    test("sleeps") {
        Thread.sleep(500)
    }
})