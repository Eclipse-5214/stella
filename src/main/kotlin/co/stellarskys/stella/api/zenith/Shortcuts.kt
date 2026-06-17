package co.stellarskys.stella.api.zenith

import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.server.packs.resources.ResourceManager

//? if >= 26.2 {
/*import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
*///? }

inline val client: Minecraft get() = Zenith.client
inline val textureManager: TextureManager get() = Zenith.textureManager
inline val player: LocalPlayer? get() = Zenith.player
inline val world: ClientLevel? get() = Zenith.world
inline val resourceManager: ResourceManager get() = Zenith.resourceManager
inline val camera: Camera get() = Zenith.cam

//? if >= 26.2 {
/*fun Minecraft.setScreen(screen: Screen?) = gui.setScreen(screen)
val Minecraft.screen: Screen? get() = gui.screen()
val BlockPos.center: Vec3 get() = Vec3.atCenterOf(this)
val Gui.chat: ChatComponent get() = hud.chat
*///? }