package statistikkapi.datakatalog

interface DatakatalogData {

    fun views(): List<View>
    fun plotlyFiler(): List<Pair<String, String>>
}
