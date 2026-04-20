package co.stellarskys.stella.api.zenith

import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.server.packs.resources.ResourceManager

inline val client: Minecraft get() = Zenith.client
inline val textureManager: TextureManager get() = Zenith.textureManager
inline val player: LocalPlayer? get() = Zenith.player
inline val world: ClientLevel? get() = Zenith.world
inline val resourceManager: ResourceManager get() = Zenith.resourceManager
inline val camera: Camera get() = Zenith.cam