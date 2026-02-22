package co.stellarskys.stella.features.dungeons

import co.stellarskys.stella.annotations.Command
import co.stellarskys.stella.utils.ChatUtils
import co.stellarskys.stella.utils.Commodore

@Command
object funnyCommand: Commodore("dn") {
    init {
        runs {
            ChatUtils.sendCommand("/warp dungeon_hub")
            ChatUtils.fakeMessage("§7Warping to...")
            ChatUtils.fakeMessage("§7Deez nuts lmao")
        }
    }
}