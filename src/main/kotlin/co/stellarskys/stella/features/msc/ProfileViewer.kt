package co.stellarskys.stella.features.msc

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.api.handlers.Chronos
import co.stellarskys.stella.api.handlers.Chronos.millis
import co.stellarskys.stella.api.handlers.Signal
import co.stellarskys.stella.api.hypixel.HypixelApi
import co.stellarskys.stella.api.hypixel.SkyblockResponse
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.features.msc.profileUtils.CollectionUtils
import co.stellarskys.stella.features.msc.profileUtils.PvScreen
import co.stellarskys.stella.features.msc.profileUtils.SkillUtils
import co.stellarskys.stella.utils.config
import kotlin.time.Duration.Companion.minutes

@Module
object ProfileViewer: Feature("profileViewer") {
    val pv by config.property<Boolean>("profileViewer.pv")
    val showRarity by config.property<Boolean>("profileViewer.showRarity")

    override fun initialize() {
        SkillUtils.load()
        CollectionUtils.load()
    }

    fun view(name: String) {
        Signal.modMessage("§bOpening profile for $name...")
        fetchProfile(name) { profile ->
            if (profile == null) return@fetchProfile
            displayProfile(name, profile)
        }
    }

    fun fetchProfile(name: String, callback: (SkyblockResponse.SkyblockMember?) -> Unit) {
        if (!isEnabled()) {
            Signal.modMessage("§cError: Profile viewer is disabled")
            callback(null)
            return
        }

        HypixelApi.getUuid(name) { uuid ->
            if (uuid == null) {
                Signal.modMessage("§cError: Could not find UUID for $name")
                callback(null)
                return@getUuid
            }

            HypixelApi.fetchSkyblockProfile(uuid, 5.minutes.millis) { member ->
                if (member == null) {
                    Signal.modMessage("§cError: Could not fetch profile for $name")
                    callback(null)
                    return@fetchSkyblockProfile
                }

                val profileId = member.profile?.id
                if (profileId != null) {
                    HypixelApi.fetchMuseum(profileId, uuid) { museumValue ->
                        member.museumValue = museumValue ?: 0L
                        callback(member)
                    }
                } else {
                    callback(member)
                }
            }
        }
    }

    fun displayProfile(name: String, profile: SkyblockResponse.SkyblockMember) {
        Chronos.Tick post {
            PvScreen.open(name, profile)
        }
    }
}