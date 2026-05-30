package co.stellarskys.stella.api.update

import co.stellarskys.stella.Stella
import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.api.handlers.Quasar
import co.stellarskys.stella.api.handlers.Signal
import co.stellarskys.stella.api.handlers.Signal.onHover
import co.stellarskys.stella.api.zenith.client
import co.stellarskys.stella.utils.config
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.fabricmc.loader.api.Version
import net.minecraft.SharedConstants
import net.minecraft.network.chat.*
import java.net.URI
import java.time.Instant

@Module
object UpdateChecker {
    private val check by config.property<Boolean>("update")
    private val stream by config.property<UpdateStream>("update.stream")

    private const val MODRINTH_PROJECT_ID = "ZSWJelST"
    private const val GITHUB_REPO = "Eclipse-5214/stella"
    private const val CURRENT_BRANCH = "26.1"

    data class ModrinthVersion(
        @SerializedName("id") val versionId: String,
        @SerializedName("version_number") val versionNumber: String,
        @SerializedName("date_published") val datePublished: String,
        @SerializedName("version_type") val versionType: String
    )
    data class GitHubRunsResponse(@SerializedName("workflow_runs") val workflowRuns: List<GitHubRun>)
    data class GitHubRun(@SerializedName("created_at") val createdAt: String, @SerializedName("html_url") val htmlUrl: String)

    init { if (check) check() }

    fun check(cb: (Boolean) -> Unit = {}) {
        when (stream) {
            UpdateStream.RELEASE -> checkModrinth(false, cb)
            UpdateStream.BETA -> checkModrinth(true, cb)
            UpdateStream.NIGHTLY -> checkGitHubNightly(cb)
        }
    }

    private fun checkModrinth(isBeta: Boolean, cb: (Boolean) -> Unit) {
        val mc = SharedConstants.getCurrentVersion().id()
        val url = "https://api.modrinth.com/v2/project/$MODRINTH_PROJECT_ID/version?loaders=[%22fabric%22]&game_versions=[%22$mc%22]&include_changelog=false"

        Quasar.fetch<List<ModrinthVersion>>(url) { res ->
            res.onSuccess { versions ->
                val targetType = if (isBeta) "beta" else "release"
                val target = versions.firstOrNull { it.versionType == targetType } ?: return@fetch
                runCatching {
                    if (isBeta) {
                        if (Instant.parse(target.datePublished).isAfter(Instant.parse(BuildInfo.BUILD_TIMESTAMP))) {
                            triggerNotification("https://modrinth.com/mod/$MODRINTH_PROJECT_ID/version/${target.versionId}"); cb(true)
                        } else cb(false)
                    } else if (Version.parse(target.versionNumber) > Version.parse(BuildInfo.VERSION)) {
                        triggerNotification("https://modrinth.com/mod/$MODRINTH_PROJECT_ID/version/${target.versionId}"); cb(true)
                    } else cb(false)
                }.onFailure { Stella.LOGGER.error("[Stella UpdateChecker] Modrinth check error: ${it.message}"); cb(false) }
            }
        }
    }

    private fun checkGitHubNightly(cb: (Boolean) -> Unit) {
        val url = "https://api.github.com/repos/$GITHUB_REPO/actions/runs?per_page=1&status=success&branch=$CURRENT_BRANCH"
        Quasar.fetch<GitHubRunsResponse>(url) { res ->
            res.onSuccess { resp ->
                val run = resp.workflowRuns.firstOrNull() ?: return@fetch
                runCatching {
                    if (Instant.parse(run.createdAt).isAfter(Instant.parse(BuildInfo.BUILD_TIMESTAMP))) {
                        triggerNotification(run.htmlUrl); cb(true)
                    } else cb(false)
                }.onFailure { Stella.LOGGER.error("[Stella UpdateChecker] GitHub check error: ${it.message}"); cb(false) }
            }
        }
    }

    private fun triggerNotification(url: String) = Stella.scope.launch {
        while (client.level == null) delay(2000)
        delay(1000)
        client.execute {
            Signal.fakeMessage("§7${Signal.LINE}")
            Signal.fakeMessage("${Stella.PREFIX} §bA new ${stream.friendlyName} update is available!")
            Signal.fakeMessage(
                Component.literal(" §7- §6CLICK §dTo open the update page.")
                    .withStyle(Style.EMPTY.withClickEvent(ClickEvent.OpenUrl(URI.create(url))))
                    .onHover("§b$url")
            )
            Signal.fakeMessage("§7${Signal.LINE}")
        }
    }
}