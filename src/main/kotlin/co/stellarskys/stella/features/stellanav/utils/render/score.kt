package co.stellarskys.stella.features.stellanav.utils.render

import dev.deftu.omnicore.api.client.client
import dev.deftu.omnicore.api.client.player
import dev.deftu.omnicore.api.client.world
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.state.MapRenderState
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.MapItem
import net.minecraft.world.level.saveddata.maps.MapId
import net.minecraft.world.level.saveddata.maps.MapItemSavedData

object score {
    var cachedRenderState = MapRenderState()

    fun getCurrentMap(): ItemStack? {
        val stack = player?.inventory?.getItem(8) ?: return null
        if (stack.item !is MapItem) return null
        return stack
    }

    fun getCurrentMapId(stack: ItemStack?): MapId? {
        return stack?.get(DataComponents.MAP_ID)
    }

    fun getCurrentMapState(id: MapId?): MapItemSavedData? {
        if (id == null) return null
        return MapItem.getSavedData(id, world!!)
    }

    fun getCurrentMapRender(): MapRenderState? {
        val renderState = MapRenderState()

        if (player == null || world == null) return null

        val map = getCurrentMap()
        val id = getCurrentMapId(map) ?: return null
        val state = getCurrentMapState(id) ?: return null

        client.mapRenderer.extractRenderState(id,state, renderState)
        cachedRenderState = renderState
        return renderState
    }

    fun render(context: GuiGraphics){
        val matrix = context.pose()
        val renderState = getCurrentMapRender() ?: cachedRenderState

        matrix.pushMatrix()
        matrix.translate(5f, 5f,)
        context.submitMapRenderState(renderState)
        matrix.popMatrix()
    }
}