package co.stellarskys.stella.features.dungeons

import co.stellarskys.stella.annotations.Command
import co.stellarskys.stella.api.handlers.Signal
import co.stellarskys.stella.api.handlers.Atlas

@Command
object FunnyCommand: Atlas("dn") {
    init {
        runs {
            Signal.sendCommand("/warp dungeon_hub")
            Signal.fakeMessage("§7Warping to...")
            Signal.fakeMessage("§7Deez nuts lmao")
        }
    }
}