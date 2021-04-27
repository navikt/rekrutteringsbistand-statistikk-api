package datakatalog.plot

data class Data(val x: List<String>, val name: String, val y: List<Int>, val type: String)

fun testData(names: List<String>) = names.map { name ->
    Data(
        x = listOf(
            "2021-04-08",
            "2021-04-09",
            "2021-04-10",
            "2021-04-11",
            "2021-04-12",
            "2021-04-13",
            "2021-04-14",
            "2021-04-15",
            "2021-04-16",
            "2021-04-17",
            "2021-04-18",
            "2021-04-19",
            "2021-04-20"
        ),
        name = name,
        y = (1..13).map { 0 },
        type = "bar"
    )
}