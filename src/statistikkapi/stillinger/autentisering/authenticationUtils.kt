package rekrutteringsbistand.stilling.indekser.autentisering

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.extensions.authentication
import statistikkapi.stillinger.autentisering.AccessToken

fun addBearerToken(httpClient: FuelManager, getToken: () -> AccessToken) {
    httpClient.addRequestInterceptor {
        { request ->
            val token = getToken()
            request.authentication().bearer(token.access_token)
            it(request)
        }
    }
}

fun addBasicAuthentication(httpClient: FuelManager, username: String, password: String) {
    httpClient.addRequestInterceptor {
        { request ->
            request.authentication().basic(username, password)
            it(request)
        }
    }
}
