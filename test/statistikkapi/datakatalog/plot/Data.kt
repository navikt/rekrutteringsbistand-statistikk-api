package statistikkapi.datakatalog.plot

import java.time.LocalDate

data class Data(val x: List<String>, val name: String, val y: List<Int>, val type: String)

fun testData(datoer: List<LocalDate>, names: List<String>, yValues:List<Map<LocalDate,Int>> = names.indices.map { mapOf()}) = names.mapIndexed { index, name ->
    Data(
        x = datoer.map { it.toString() },
        name = name,
        y = datoer.map { dato -> yValues[index][dato] ?: 0 },
        type = "bar"
    )
}
