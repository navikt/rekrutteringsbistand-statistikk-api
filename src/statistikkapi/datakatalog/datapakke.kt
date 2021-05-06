package statistikkapi.datakatalog

import com.fasterxml.jackson.annotation.JsonProperty


data class Datapakke (
    val title: String,
    val description: String,
    val views: List<View>,
    val resources: List<Resource>
)


data class Resource (
    val name: String,
    val description: String,
    val path: String,
    val format: String,

    @JsonProperty("dsv_separator")
    val dsvSeparator: String
)


data class View (
    val title: String,
    val description: String,
    val specType: String,
    val spec: Spec
)


data class Spec (
    val url: String
)
