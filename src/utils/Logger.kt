package no.nav.rekrutteringsbistand.statistikk.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

val Any.Log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
