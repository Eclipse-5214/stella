package co.stellarskys.stella.features.dungeons

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.hud.HUDManager
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import co.stellarskys.stella.events.core.GuiEvent
import co.stellarskys.stella.utils.skyblock.location.SkyBlockIsland
import net.minecraft.client.gui.GuiGraphics


@Module
object roomName : Feature("showRoomName", island = SkyBlockIsland.THE_CATACOMBS) {
    override fun initialize() {
        HUDManager.register("roomname", "No Room Found", "showRoomName")
        register<GuiEvent.RenderHUD> { renderHUD(it.context)}
    }

    private fun renderHUD(
        context: GuiGraphics
    ) {
        if (Dungeon.inBoss) return

        val chroma = config["roomNameChroma"] as Boolean

        val text = "${if (chroma) "§z" else ""}${Dungeon.currentRoom?.name ?: "No Room Found"}"
        val x = HUDManager.getX("roomname") + 5f
        val y = HUDManager.getY("roomname") + 5f
        val scale = HUDManager.getScale("roomname")

        Render2D.drawString(context,text, x.toInt(), y.toInt(), scale, false)
    }
}