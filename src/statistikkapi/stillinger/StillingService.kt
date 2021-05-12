package statistikkapi.stillinger

class StillingService(
    private val elasticSearchKlient: ElasticSearchKlient,
    private val stillingRepository: StillingRepository
) {

    fun registrerStilling(stillingUuid: String): Long {
        val stillingFraDatabase: Stilling? = stillingRepository.hentStilling(stillingUuid)
        val stillingFraElasticSearch: ElasticSearchStilling? = elasticSearchKlient.hentStilling(stillingUuid)

        if (stillingFraElasticSearch == null && stillingFraDatabase == null) {
            throw RuntimeException("Eksisterer ingen stilling med UUID: $stillingUuid")
        }

        val måLagreStillingFraElasticSearch =
            stillingFraDatabase == null ||
                    stillingFraElasticSearch != null && !erLike(stillingFraDatabase, stillingFraElasticSearch)

        return if (måLagreStillingFraElasticSearch) {
            stillingRepository.lagreStilling(stillingFraElasticSearch!!)
        } else {
            stillingFraDatabase!!.id
        }
    }

    private fun erLike(stillingFraDatabase: Stilling, stillingFraElasticSearch: ElasticSearchStilling) =
        stillingFraDatabase.uuid == stillingFraElasticSearch.uuid &&
                stillingFraDatabase.publisert == stillingFraElasticSearch.publisert &&
                stillingFraDatabase.inkluderingsmuligheter == stillingFraElasticSearch.inkluderingsmuligheter &&
                stillingFraDatabase.prioriterteMålgrupper == stillingFraElasticSearch.prioriterteMålgrupper &&
                stillingFraDatabase.tiltakEllerEllerVirkemidler == stillingFraElasticSearch.tiltakEllerEllerVirkemidler
}
