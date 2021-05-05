package datakatalog.plot

import java.time.LocalDate

data class Data(val x: List<String>, val name: String, val y: List<Int>, val type: String)

fun testData(datoer: List<LocalDate>, names: List<String>) = names.map { name ->
    Data(
        x = datoer.map { it.toString() },
        name = name,
        y = (1..datoer.size).map { 0 },
        type = "bar"
    )
}
