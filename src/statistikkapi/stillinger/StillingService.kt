package statistikkapi.stillinger

import statistikkapi.log

class StillingService(
    private val elasticSearchKlient: ElasticSearchKlient,
    private val stillingRepository: StillingRepository
) {

    fun registrerStilling(stillingUuid: String) {
        val startHele = System.currentTimeMillis()

        var start = System.currentTimeMillis()
        val stillingFraDatabase: Stilling? = stillingRepository.hentNyesteStilling(stillingUuid)
        log.info("Mottok utfall, hentNyesteStilling tok ${System.currentTimeMillis() - start} ms")

        start = System.currentTimeMillis()
        val stillingFraElasticSearch: ElasticSearchStilling? = elasticSearchKlient.hentStilling(stillingUuid)
        log.info("Mottok utfall, hentStilling tok ${System.currentTimeMillis() - start} ms")

        if (stillingFraElasticSearch == null && stillingFraDatabase == null) {
            throw RuntimeException("Eksisterer ingen stilling med UUID: $stillingUuid")
        }

        val måLagreStillingFraElasticSearch =
            stillingFraDatabase == null ||
                    stillingFraElasticSearch != null && (stillingFraDatabase `er ulik` stillingFraElasticSearch)

        if (måLagreStillingFraElasticSearch) {
            start = System.currentTimeMillis()
            stillingRepository.lagreStilling(stillingFraElasticSearch!!)
            log.info("Mottok utfall, lagreStilling tok ${System.currentTimeMillis() - start} ms")
        }

        log.info("Mottok utfall, hele registrerStilling tok ${System.currentTimeMillis() - startHele} ms")
    }
}
