package statistikkapi.stillinger

class StillingService(
    private val elasticSearchKlient: ElasticSearchKlient,
    private val stillingRepository: StillingRepository
) {

    fun registrerStilling(stillingUuid: String) {
        val stillingFraDatabase: Stilling? = stillingRepository.hentNyesteStilling(stillingUuid)
        val stillingFraElasticSearch: ElasticSearchStilling? = elasticSearchKlient.hentStilling(stillingUuid)

        if (stillingFraElasticSearch == null && stillingFraDatabase == null) {
            throw RuntimeException("Eksisterer ingen stilling med UUID: $stillingUuid")
        }

        val måLagreStillingFraElasticSearch =
            stillingFraDatabase == null ||
                    stillingFraElasticSearch != null && (stillingFraDatabase `er ulik` stillingFraElasticSearch)

        if (måLagreStillingFraElasticSearch) {
            stillingRepository.lagreStilling(stillingFraElasticSearch!!)
        }
    }
}
