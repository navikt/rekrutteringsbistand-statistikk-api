package no.nav.statistikkapi.stillinger

import java.util.*

class StillingService(
    private val elasticSearchKlient: ElasticSearchKlient,
    private val stillingRepository: StillingRepository
) {

    fun registrerStilling(stillingsId: UUID) {
        val stillingFraDatabase: Stilling? = stillingRepository.hentNyesteStilling(stillingsId)
        val stillingFraElasticSearch: ElasticSearchStilling? = elasticSearchKlient.hentStilling(stillingsId.toString()) // TODO Are: UUID?

        if (stillingFraElasticSearch == null && stillingFraDatabase == null) {
            throw RuntimeException("Eksisterer ingen stilling med UUID: $stillingsId")
        }

        val måLagreStillingFraElasticSearch =
            stillingFraDatabase == null ||
                    stillingFraElasticSearch != null && (stillingFraDatabase `er ulik` stillingFraElasticSearch)

        if (måLagreStillingFraElasticSearch) {
            stillingRepository.lagreStilling(stillingFraElasticSearch!!)
        }
    }
}
