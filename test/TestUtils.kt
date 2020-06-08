import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nimbusds.jwt.SignedJWT
import io.ktor.http.Cookie
import no.nav.security.token.support.test.JwtTokenGenerator
import kotlin.random.Random

fun lagCookie(): Cookie {
    val token: SignedJWT = JwtTokenGenerator.createSignedJWT(enNavIdent)
    return Cookie("isso-idtoken", token.serialize())
}

fun randomPort(): Int = Random.nextInt(1000, 9999)

fun tilJson(objekt: Any): String {
    val mapper = jacksonObjectMapper()
    mapper.registerModule(JavaTimeModule())
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    return mapper.writeValueAsString(objekt)
}
