import com.nimbusds.jwt.SignedJWT
import io.ktor.http.Cookie
import no.nav.security.token.support.test.JwtTokenGenerator
import kotlin.random.Random

fun lagCookie(): Cookie {
    val token: SignedJWT = JwtTokenGenerator.createSignedJWT(enNavIdent)
    return Cookie("isso-idtoken", token.serialize())
}

fun randomPort(): Int = Random.nextInt(1000, 9999)
