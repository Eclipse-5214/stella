package co.stellarskys.stella.features.dungeons

import co.stellarskys.stella.Stella
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.hud.HUDManager
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import co.stellarskys.stella.utils.skyblock.dungeons.DungeonScanner
import co.stellarskys.stella.utils.CompatHelpers.UDrawContext
//#if MC > 1.21.5
import co.stellarskys.stella.events.GuiEvent
//#elseif MC == 1.8.9
//$$ import co.stellarskys.stella.events.RenderEvent
//#endif

@Stella.Module
object roomName : Feature("showRoomName", area = "catacombs") {
    override fun initialize() {
        HUDManager.register("roomname", "No Room Found")

        //#if MC > 1.21.5
        register<GuiEvent.HUD> { renderHUD(it.context)}
        //#elseif MC == 1.8.9
        //$$ register<RenderEvent.Text> { renderHUD(it.context) }
        //#endif
    }

    private fun renderHUD(
        context: UDrawContext
    ) {
        if (!HUDManager.isEnabled("roomname")) return
        if (Dungeon.inBoss()) return

        val text = "§z${DungeonScanner.currentRoom?.name ?: "No Room Found"}"
        val x = HUDManager.getX("roomname") + 5f
        val y = HUDManager.getY("roomname") + 5f
        val scale = HUDManager.getScale("roomname")

        Render2D.drawString(context,text, x.toInt(), y.toInt(), scale, false)
    }
}