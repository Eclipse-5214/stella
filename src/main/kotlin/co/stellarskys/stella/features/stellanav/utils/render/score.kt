package co.stellarskys.stella.features.stellanav.utils.render

import co.stellarskys.stella.Stella
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.MapRenderState
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.MapIdComponent
import net.minecraft.item.FilledMapItem
import net.minecraft.item.ItemStack
import net.minecraft.item.map.MapState
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.knit.api.KnitPlayer

object score {
    var cachedRenderState = MapRenderState()

    fun getCurrentMap(): ItemStack? {
        val stack = KnitPlayer.player?.inventory?.getStack(8) ?: return null
        if (stack.item !is FilledMapItem) return null
        return stack
    }

    fun getCurrentMapId(stack: ItemStack?): MapIdComponent? {
        return stack?.get(DataComponentTypes.MAP_ID)
    }

    fun getCurrentMapState(id: MapIdComponent?): MapState? {
        if (id == null) return null
        return FilledMapItem.getMapState(id, KnitClient.world!!)
    }

    fun getCurrentMapRender(): MapRenderState? {
        val renderState = MapRenderState()

        if (KnitPlayer.player == null || KnitClient.world == null) return null

        val map = getCurrentMap()
        val id = getCurrentMapId(map) ?: return null
        val state = getCurrentMapState(id) ?: return null

        KnitClient.client.mapRenderer.update(id,state, renderState)
        cachedRenderState = renderState
        return renderState
    }

    fun render(context: DrawContext){
        val matrix = context.matrices
        val renderState = getCurrentMapRender() ?: cachedRenderState

        matrix.pushMatrix()
        matrix.translate(5f, 5f,)
        context.drawMap(renderState)
        matrix.popMatrix()
    }
}