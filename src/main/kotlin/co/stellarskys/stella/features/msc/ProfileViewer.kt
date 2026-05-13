package co.stellarskys.stella.features.msc

import co.stellarskys.stella.api.handlers.Chronos
import co.stellarskys.stella.api.handlers.Chronos.millis
import co.stellarskys.stella.api.handlers.Signal
import co.stellarskys.stella.api.hypixel.HypixelApi
import co.stellarskys.stella.api.hypixel.SkyblockResponse
import co.stellarskys.stella.api.zenith.client
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.features.msc.profileUtils.PvScreen
import co.stellarskys.stella.utils.config
import kotlin.time.Duration.Companion.minutes

object ProfileViewer: Feature("profileViewer") {
    val pv by config.property<Boolean>("profileViewer.pv")

    fun view(name: String) {
        fetchProfile(name) { profile ->
            if (profile == null) return@fetchProfile
            displayProfile(profile)
        }
    }

    fun fetchProfile(name: String, callback: (SkyblockResponse.SkyblockMember?) -> Unit) {
        HypixelApi.getUuid(name) { uuid ->
            if (uuid == null) {
                Signal.modMessage("§cError: Could not find UUID for $name")
                callback(null)
                return@getUuid
            }

            HypixelApi.fetchSkyblockProfile(uuid, 5.minutes.millis) { profile ->
                if (profile == null) {
                    Signal.modMessage("§cError: Could not fetch profile for $name")
                    callback(null)
                    return@fetchSkyblockProfile
                }

                callback(profile)
            }
        }
    }

    fun displayProfile(profile: SkyblockResponse.SkyblockMember) {
        if (!isEnabled()) {
            Signal.modMessage("§cError: Profile viewer is disabled")
            return
        }

        Chronos.Tick post {
            client.setScreen(PvScreen(profile))
        }
    }
}