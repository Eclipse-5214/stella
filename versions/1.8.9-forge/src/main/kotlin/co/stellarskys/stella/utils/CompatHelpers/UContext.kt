package co.stellarskys.stella.utils.CompatHelpers

import net.minecraft.client.renderer.GlStateManager

interface UMatrix {
    fun pushMatrix()
    fun popMatrix()
    fun translate(x: Float, y: Float, z: Float)
    fun rotate(angle: Float, x: Float, y: Float, z: Float)
    fun scale(x: Float, y: Float, z: Float)
}

object matrix : UMatrix {
    override fun pushMatrix() = GlStateManager.pushMatrix()
    override fun popMatrix() = GlStateManager.popMatrix()
    override fun translate(x: Float, y: Float, z: Float) = GlStateManager.translate(x, y, z)
    override fun rotate(angle: Float, x: Float, y: Float, z: Float) {GlStateManager.rotate(angle, x, y, z)}
    override fun scale(x: Float, y: Float, z: Float) = GlStateManager.scale(x, y, z)
}

class DrawContext {
    val matrices: UMatrix = matrix
}

typealias UDrawContext = DrawContext