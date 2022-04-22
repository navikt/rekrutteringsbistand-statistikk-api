package no.nav.statistikkapi.stillinger

import java.time.LocalDateTime
import java.util.*

class StillingService(
    private val elasticSearchKlient: ElasticSearchKlient,
    private val stillingRepository: StillingRepository
) {

    fun registrerOgHent(stillingsId: UUID): Stilling {
        val stillingFraDatabase: Stilling? = stillingRepository.hentNyesteStilling(stillingsId)
        if(stillingFraDatabase?.tidspunkt?.yngreEnn30Sekunder() == true) // Trenger ikke kalles om stillingen er såpass ny. Unødvendig ytelsesoptimalisering
            return stillingFraDatabase

        val stillingFraElasticSearch: ElasticSearchStilling? =
            elasticSearchKlient.hentStilling(stillingsId.toString())

        if (stillingFraElasticSearch == null && stillingFraDatabase == null) {
            throw RuntimeException("Eksisterer ingen stilling med UUID: $stillingsId")
        }

        val måLagreStillingFraElasticSearch =
            stillingFraDatabase == null ||
                    stillingFraElasticSearch != null && (stillingFraDatabase `er ulik` stillingFraElasticSearch)

        if (måLagreStillingFraElasticSearch) {
            stillingRepository.lagreStilling(stillingFraElasticSearch!!)
        }
        return stillingRepository.hentNyesteStilling(stillingsId)!!
    }
}

private fun LocalDateTime.yngreEnn30Sekunder() = isAfter(LocalDateTime.now().minusSeconds(30))
