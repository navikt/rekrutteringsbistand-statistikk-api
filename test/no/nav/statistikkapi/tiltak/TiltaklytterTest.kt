package no.nav.statistikkapi.tiltak

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.statistikkapi.LagreStatistikkTest
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.start
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDateTime

class TiltaklytterTest {
    companion object {
        private val rapid = TestRapid()
        private val database = TestDatabase()

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            start(database = database, rapid = rapid)
        }
    }

    @Before
    fun beforeEach() {
        rapid.reset()
    }

    @Test
    fun testAtDenIkkeFeiler() {
        rapid.sendTestMessage("""
            {
              "tiltakstype":"noe",
              "deltakerFnr": "12345678910",
              "opprettetTidspunkt": "${LocalDateTime.now()}"
            }
            """.trimIndent())
    }
}