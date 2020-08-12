import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import no.nav.rekrutteringsbistand.statistikk.Cluster
import no.nav.rekrutteringsbistand.statistikk.unleash.UnleashConfig
import org.junit.Test

class ByClusterStrategyTest {

    @Test
    fun `ByClusterStrategy skal tolke respons fra Unleash riktig`() {
        val byClusterStrategy = UnleashConfig.Companion.ByClusterStrategy(Cluster.DEV_FSS)
        val clusterSlåttPå = mapOf(Pair("cluster", "dev-fss,prod-fss"))
        assertThat(byClusterStrategy.isEnabled(clusterSlåttPå)).isTrue()

        val clusterSlåttAv = mapOf(Pair("cluster", "prod-fss"))
        assertThat(byClusterStrategy.isEnabled(clusterSlåttAv)).isFalse()
    }
}
