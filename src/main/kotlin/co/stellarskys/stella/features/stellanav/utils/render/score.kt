package co.stellarskys.stella.features.stellanav.utils.render

import co.stellarskys.stella.Stella
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.LightmapTextureManager
import net.minecraft.client.render.MapRenderState
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.MapIdComponent
import net.minecraft.item.FilledMapItem
import net.minecraft.item.ItemStack
import net.minecraft.item.map.MapState

object score {
    var cashedRenderState = MapRenderState()

    fun getCurrentMap(): ItemStack? {
        val stack = Stella.mc.player?.inventory?.getStack(8) ?: return null
        if (stack.item !is FilledMapItem) return null
        return stack
    }

    fun getCurrentMapId(stack: ItemStack?): MapIdComponent? {
        return stack?.get(DataComponentTypes.MAP_ID)
    }

    fun getCurrentMapState(id: MapIdComponent?): MapState? {
        if (id == null) return null
        return FilledMapItem.getMapState(id, Stella.mc.world!!)
    }

    fun getCurrentMapRender(): MapRenderState? {
        val renderState = MapRenderState()

        if (Stella.mc.player == null || Stella.mc.world == null) return null

        val map = getCurrentMap()
        val id = getCurrentMapId(map) ?: return null
        val state = getCurrentMapState(id) ?: return null

        Stella.mc.mapRenderer.update(id,state, renderState)
        return renderState
    }

    fun render(context: DrawContext){
        val matrix = context.matrices

        val consumer = Stella.mc.bufferBuilders.entityVertexConsumers
        val renderState = getCurrentMapRender() ?: cashedRenderState

        matrix.push()
        matrix.translate(5f, 5f, 5f)
        Stella.mc.mapRenderer.draw(renderState, matrix, consumer,true, LightmapTextureManager.MAX_LIGHT_COORDINATE)
        matrix.pop()
    }
}