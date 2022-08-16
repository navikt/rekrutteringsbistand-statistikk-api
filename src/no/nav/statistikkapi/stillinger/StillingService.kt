package no.nav.statistikkapi.stillinger

import java.util.*

class StillingService(
    private val stillingEsKlient: StillingEsKlient,
    private val stillingRepository: StillingRepository
) {

    fun registrerOgHent(stillingsId: UUID): Stilling {
        val stillingFraDatabase: Stilling? = stillingRepository.hentNyesteStilling(stillingsId)
        val stillingFraElasticSearch: ElasticSearchStilling? = stillingEsKlient.hentStilling(stillingsId.toString())
        if (stillingFraElasticSearch == null && stillingFraDatabase == null) {
            throw RuntimeException("Eksisterer ingen stilling med UUID: $stillingsId")
        }
        val måLagreStillingFraElasticSearch =
            stillingFraDatabase == null ||
                    (stillingFraElasticSearch != null && (stillingFraDatabase `er ulik` stillingFraElasticSearch))
        if (måLagreStillingFraElasticSearch) {
            stillingRepository.lagreStilling(stillingFraElasticSearch!!)
        }
        return stillingRepository.hentNyesteStilling(stillingsId)!!
    }
}
