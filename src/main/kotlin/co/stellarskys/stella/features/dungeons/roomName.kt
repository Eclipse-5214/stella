package co.stellarskys.stella.features.dungeons

import co.stellarskys.stella.Stella
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.hud.HUDManager
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import co.stellarskys.stella.utils.CompatHelpers.UDrawContext
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.events.GuiEvent

@Stella.Module
object roomName : Feature("showRoomName", area = "catacombs") {
    override fun initialize() {
        HUDManager.register("roomname", "No Room Found")
        register<GuiEvent.HUD> { renderHUD(it.context)}
    }

    private fun renderHUD(
        context: UDrawContext
    ) {
        if (!HUDManager.isEnabled("roomname")) return
        if (Dungeon.inBoss) return

        val chroma = config["roomNameChroma"] as Boolean

        val text = "${if (chroma) "§z" else ""}${Dungeon.currentRoom?.name ?: "No Room Found"}"
        val x = HUDManager.getX("roomname") + 5f
        val y = HUDManager.getY("roomname") + 5f
        val scale = HUDManager.getScale("roomname")

        Render2D.drawString(context,text, x.toInt(), y.toInt(), scale, false)
    }
}