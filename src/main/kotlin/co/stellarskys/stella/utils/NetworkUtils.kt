package co.stellarskys.stella.utils

import co.stellarskys.stella.Stella
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.deftu.omnicore.api.client.client
import kotlinx.coroutines.*
import java.net.URI
import java.net.http.*
import java.time.Duration
import kotlin.coroutines.resume

object NetworkUtils {
    val gson = Gson()
    private val httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()

    private fun request(url: String) = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("User-Agent", "Stella-Mod")
        .timeout(Duration.ofSeconds(10))

    suspend fun fetchString(url: String): Result<String> = suspendCancellableCoroutine { cont ->
        val future = httpClient.sendAsync(request(url).GET().build(), HttpResponse.BodyHandlers.ofString())
        cont.invokeOnCancellation { future.cancel(true) }
        future.whenComplete { res, err ->
            val result = when {
                err != null -> Result.failure(err)
                res.statusCode() in 200..299 -> Result.success(res.body())
                else -> Result.failure(Exception("HTTP ${res.statusCode()} at $url"))
            }
            cont.resume(result)
        }
    }

    inline fun <reified T> fetch(
        url: String,
        scope: CoroutineScope = Stella.scope,
        crossinline onResult: (Result<T>) -> Unit
    ) {
        val type = object : TypeToken<T>() {}.type
        scope.launch {
            val res = fetchString(url).mapCatching { gson.fromJson<T>(it, type) }
            client.execute { onResult(res) }
        }
    }
}