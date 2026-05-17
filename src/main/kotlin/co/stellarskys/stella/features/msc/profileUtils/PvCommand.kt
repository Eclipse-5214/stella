package co.stellarskys.stella.features.msc.profileUtils

import co.stellarskys.stella.annotations.Command
import co.stellarskys.stella.api.handlers.Atlas
import co.stellarskys.stella.api.zenith.player
import co.stellarskys.stella.features.msc.ProfileViewer

@Command
object PvCommand: Atlas("pv") {
    init {
        runs<Greedy?> ("name") { arg ->
            val name = arg?.string ?: player?.name?.string ?: return@runs
            ProfileViewer.view(name)
        }
    }

    override fun isEnabled(): Boolean = ProfileViewer.pv
}