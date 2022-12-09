package no.nav.statistikkapi.tiltak

import assertk.assertThat
import assertk.assertions.isEqualTo
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.statistikkapi.start
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDateTime
import java.util.*

class TiltakManglerAktørIdLytterTest {

    companion object {
        private val rapid = TestRapid()

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            start(rapid = rapid)
        }

    }

    @Test
    fun `Fnr blir mappet til aktørId`() {
        val avtaleInngått = LocalDateTime.of(2000,1,1,0,0)
        val melding = """
                    {
                      "tiltakstype":"En tiltakstype",
                      "deltakerFnr": "123",
                      "avtaleId":"100",
                      "enhetOppfolging":"0123",
                      "avtaleInngått": "$avtaleInngått"
                    }
        """.trimIndent()

        rapid.sendTestMessage(melding)

        val inspekør = rapid.inspektør
        assertThat(inspekør.size).isEqualTo(1)
        val meldingPåRapid = inspekør.message(0)
        assertThat ( meldingPåRapid["tiltakstype"].asText()).isEqualTo("En tiltakstype")
        assertThat ( meldingPåRapid["deltakerFnr"].asText()).isEqualTo("123")
        assertThat ( meldingPåRapid["avtaleId"].asText()).isEqualTo("100")
        assertThat ( meldingPåRapid["enhetOppfolging"].asText()).isEqualTo("0123")
        assertThat ( meldingPåRapid["avtaleInngått"].asLocalDateTime()).isEqualTo(avtaleInngått)
        assertThat ( meldingPåRapid["fnr"].asText()).isEqualTo("123")

    }


}