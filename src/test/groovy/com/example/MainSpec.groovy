package com.example

import spock.lang.Specification

class MainSpec extends Specification {

    def "test main method runs without error"() {
        when:
        Main.main()

        then:
        noExceptionThrown()
    }
}
