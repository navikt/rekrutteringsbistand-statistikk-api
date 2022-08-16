package no.nav.statistikkapi.kandidater

import com.fasterxml.jackson.databind.JsonNode

interface KandidatEsKlient {
    fun hentKandidat(aktørId: String): JsonNode
}

class KandidatEsKlientImpl(): KandidatEsKlient {
    override fun hentKandidat(aktørId: String): JsonNode {
        TODO("Not yet implemented")
    }
}