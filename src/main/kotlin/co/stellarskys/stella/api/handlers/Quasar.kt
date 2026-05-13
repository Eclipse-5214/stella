package co.stellarskys.stella.api.handlers

import co.stellarskys.stella.Stella
import co.stellarskys.stella.api.zenith.client
import co.stellarskys.stella.generated.ModuleList
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.*
import kotlinx.coroutines.*
import java.net.URI
import java.net.http.*
import java.nio.file.Path
import java.time.Duration
import kotlin.coroutines.resume

object Quasar {
    val gson: Gson = GsonBuilder().registerTypeAdapterFactory(SkipFactory).create()
    private val httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()

    private fun request(url: String) = HttpRequest.newBuilder().uri(URI.create(url))
        .header("User-Agent", "Stella-Mod").timeout(Duration.ofSeconds(10))

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

    suspend fun downloadFile(url: String, targetPath: Path): Result<Path> = suspendCancellableCoroutine { cont ->
        val request = request(url).GET().build()
        val future = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofFile(targetPath))
        cont.invokeOnCancellation { future.cancel(true) }

        future.whenComplete { res, err ->
            val result = when {
                err != null -> Result.failure(err)
                res.statusCode() in 200..299 -> Result.success(res.body())
                else -> Result.failure(Exception("HTTP ${res.statusCode()} during download of $url"))
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
            val res = fetchString(url).mapCatching { json ->
                try { gson.fromJson<T>(json, type) } catch (e: Exception) {
                    Stella.LOGGER.error("Quasar GSON Error: ${e.message}"); throw e
                }
            }

            client.execute { onResult(res) }
        }
    }

    private object SkipFactory : TypeAdapterFactory {
        override fun <T : Any> create(gson: Gson, type: TypeToken<T>) = if (type.rawType in ModuleList.skippedTypes)
            object : TypeAdapter<T>() {
                override fun write(out: JsonWriter, value: T?) { out.nullValue() }
                override fun read(reader: JsonReader) = reader.skipValue().let { null }
            } else null
    }
}