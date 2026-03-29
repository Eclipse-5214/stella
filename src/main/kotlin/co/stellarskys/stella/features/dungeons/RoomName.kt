package co.stellarskys.stella.features.dungeons

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.hud.HUDManager
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.api.dungeons.Dungeon
import co.stellarskys.stella.events.core.GuiEvent
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import net.minecraft.client.gui.GuiGraphics


@Module
object RoomName : Feature("showRoomName", island = SkyBlockIsland.THE_CATACOMBS) {
    private val chroma by config.property<Boolean>("roomNameChroma")

    override fun initialize() {
        HUDManager.register("roomname", "No Room Found", "showRoomName")
        on<GuiEvent.RenderHUD> { renderHUD(it.context) }
    }

    private fun renderHUD(
        context: GuiGraphics
    ) = HUDManager.renderHud("roomname", context) {
        if (Dungeon.inBoss) return@renderHud
        val text = "${if (chroma) "§z" else ""}${Dungeon.currentRoom?.name ?: "No Room Found"}"
        Render2D.drawString(context,text, 0, 0, shadow = false)
    }
}