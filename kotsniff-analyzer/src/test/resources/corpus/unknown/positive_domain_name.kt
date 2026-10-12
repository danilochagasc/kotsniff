package corpus.unknown

import io.kotest.core.spec.style.FunSpec

class DomainNameSpec : FunSpec({
    test("calls a domain method whose name starts with expect") {
        expectStatus(200)
    }
})