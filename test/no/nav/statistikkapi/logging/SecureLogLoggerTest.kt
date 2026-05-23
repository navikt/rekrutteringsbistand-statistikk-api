package no.nav.statistikkapi.logging

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.statistikkapi.logging.SecureLogLogger.Companion.secure
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.Marker

class SecureLogLoggerTest {

    @Test
    fun `info bruker TEAM_LOGS marker`() {
        val logger = mockk<Logger>(relaxed = true)
        val secureLog = secure(logger)
        val marker = slot<Marker>()

        secureLog.info("en melding")

        verify(exactly = 1) {
            logger.info(capture(marker), "en melding")
        }
        assertThat(marker.captured.name).isEqualTo("TEAM_LOGS")
    }

    @Test
    fun `warn bruker TEAM_LOGS marker`() {
        val logger = mockk<Logger>(relaxed = true)
        val secureLog = secure(logger)
        val marker = slot<Marker>()

        secureLog.warn("en advarsel")

        verify(exactly = 1) {
            logger.warn(capture(marker), "en advarsel")
        }
        assertThat(marker.captured.name).isEqualTo("TEAM_LOGS")
    }

    @Test
    fun `error bruker TEAM_LOGS marker`() {
        val logger = mockk<Logger>(relaxed = true)
        val secureLog = secure(logger)
        val marker = slot<Marker>()

        secureLog.error("en feil")

        verify(exactly = 1) {
            logger.error(capture(marker), "en feil")
        }
        assertThat(marker.captured.name).isEqualTo("TEAM_LOGS")
    }

    @Test
    fun `info med exception bruker TEAM_LOGS marker`() {
        val logger = mockk<Logger>(relaxed = true)
        val secureLog = secure(logger)
        val marker = slot<Marker>()
        val e = RuntimeException("boom")

        secureLog.info("en melding", e)

        verify(exactly = 1) {
            logger.info(capture(marker), "en melding", e)
        }
        assertThat(marker.captured.name).isEqualTo("TEAM_LOGS")
    }

    @Test
    fun `warn med exception bruker TEAM_LOGS marker`() {
        val logger = mockk<Logger>(relaxed = true)
        val secureLog = secure(logger)
        val marker = slot<Marker>()
        val e = RuntimeException("boom")

        secureLog.warn("en advarsel", e)

        verify(exactly = 1) {
            logger.warn(capture(marker), "en advarsel", e)
        }
        assertThat(marker.captured.name).isEqualTo("TEAM_LOGS")
    }

    @Test
    fun `error med exception bruker TEAM_LOGS marker`() {
        val logger = mockk<Logger>(relaxed = true)
        val secureLog = secure(logger)
        val marker = slot<Marker>()
        val e = RuntimeException("boom")

        secureLog.error("en feil", e)

        verify(exactly = 1) {
            logger.error(capture(marker), "en feil", e)
        }
        assertThat(marker.captured.name).isEqualTo("TEAM_LOGS")
    }
}
