package no.nav

import org.slf4j.Logger
import org.slf4j.LoggerFactory

val Any.LOG: Logger
    get() = LoggerFactory.getLogger(this::class.java)