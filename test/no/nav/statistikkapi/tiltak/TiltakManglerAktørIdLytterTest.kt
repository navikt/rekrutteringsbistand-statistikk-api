package no.nav.statistikkapi.tiltak

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import assertk.assertions.isZero
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.statistikkapi.atOslo
import no.nav.statistikkapi.randomPort
import no.nav.statistikkapi.start
import org.junit.Before
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
            start(rapid = rapid, port = randomPort())
        }

    }

    @Before
    fun before() {
        rapid.reset()
    }

    @Test
    fun `Fnr blir mappet til aktørId`() {
        val avtaleInngått = LocalDateTime.of(2000, 1, 1, 0, 0)
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
        assertThat(meldingPåRapid["tiltakstype"].asText()).isEqualTo("En tiltakstype")
        assertThat(meldingPåRapid["deltakerFnr"].asText()).isEqualTo("123")
        assertThat(meldingPåRapid["avtaleId"].asText()).isEqualTo("100")
        assertThat(meldingPåRapid["enhetOppfolging"].asText()).isEqualTo("0123")
        assertThat(meldingPåRapid["avtaleInngått"].asLocalDateTime()).isEqualTo(avtaleInngått)
        assertThat(meldingPåRapid["fnr"].asText()).isEqualTo("123")
    }

    @Test
    fun `Tiltaksmelding med aktørId skal lede til kvitteringsmelding`() {
        val avtaleInngått = LocalDateTime.of(2000, 1, 1, 0, 0)
        val melding = """
                    {
                      "tiltakstype":"En tiltakstype",
                      "deltakerFnr": "123",
                      "avtaleId":"${UUID.randomUUID()}",
                      "enhetOppfolging":"0123",
                      "avtaleInngått": "$avtaleInngått",
                      "sistEndret": "${avtaleInngått.atOslo()}",
                      "aktørId":"12344"
                    }
        """.trimIndent()

        rapid.sendTestMessage(melding)

        assertThat(rapid.inspektør.size).isEqualTo(1)
        assertThat(rapid.inspektør.message(0)["@slutt_av_hendelseskjede"].asBoolean()).isTrue()
    }

    @Test
    fun `Tiltaksmelding med slutt_av_hendelseskjede true skal ikke føre til flere meldinger`() {
        val avtaleInngått = LocalDateTime.of(2000, 1, 1, 0, 0)
        val melding = """
                    {
                      "tiltakstype":"En tiltakstype",
                      "deltakerFnr": "123",
                      "avtaleId":"${UUID.randomUUID()}",
                      "enhetOppfolging":"0123",
                      "avtaleInngått": "$avtaleInngått",
"                     "sistEndret": "${avtaleInngått.atOslo()}",
                      "aktørId":"12344",
                      "@slutt_av_hendelseskjede":true
                    }
        """.trimIndent()

        rapid.sendTestMessage(melding)

        assertThat(rapid.inspektør.size).isZero()
    }
}