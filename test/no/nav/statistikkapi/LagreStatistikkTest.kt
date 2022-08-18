package no.nav.statistikkapi

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.db.TestRepository
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class LagreStatistikkTest {

    companion object {
        private val database = TestDatabase()
        private val rapid: TestRapid = TestRapid()
        private val testRepository = TestRepository(database.dataSource)
        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            start(database = database, rapid = rapid)
        }
    }

    @After
    fun afterEach() {
        testRepository.slettAlleUtfall()
        rapid.reset()
    }

    @Test
    fun test() {
        val json = "{}"
        rapid.sendTestMessage(json)
    }



}