package statistikkapi.datakatalog

import statistikkapi.Cluster

class DatakatalogUrl(cluster: Cluster) {
    private val rootUrl: String = rootUrlFra(cluster)
    private val datapakkeId: String = datapakkeIdFra(cluster)

    companion object {
        private fun rootUrlFra(cluster: Cluster) = when (cluster) {
            Cluster.PROD_FSS -> "https://datakatalog-api.intern.nav.no/v1/datapackage/"
            Cluster.DEV_FSS -> "https://datakatalog-api.dev.intern.nav.no/v1/datapackage/"
            Cluster.LOKAL -> "https://datakatalog-api.dev.intern.nav.no/v1/datapackage/"
        }

        private fun datapakkeIdFra(cluster: Cluster) = when (cluster) {
            Cluster.PROD_FSS -> "e0745dcae428b0fa4309b3c065f7706b"
            Cluster.DEV_FSS -> "e0745dcae428b0fa4309b3c065f7706b"
            Cluster.LOKAL -> "10d33ba3796b95b53ac1466015aa0ac7"
        }
    }

    fun datapakke() = "$rootUrl$datapakkeId"
    fun ressursfil() = "$rootUrl$datapakkeId/attachments"
}