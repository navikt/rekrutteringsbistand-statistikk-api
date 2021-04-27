package no.nav.rekrutteringsbistand.statistikk.datakatalog

interface DatakatalogData {

    fun views(): List<View>
    fun plotlyFiler(): List<Pair<String, String>>
}
