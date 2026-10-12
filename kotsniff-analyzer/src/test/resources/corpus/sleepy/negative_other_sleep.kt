package corpus.sleepy

import io.kotest.core.spec.style.FunSpec

class OtherSleepSpec : FunSpec({
    test("uses an unrelated sleep") {
        myThread.sleep(100)
    }
})