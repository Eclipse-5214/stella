package co.stellarskys.stella.features.stellanav.render

import co.stellarskys.stella.Stella
import co.stellarskys.stella.features.stellanav.map
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import co.stellarskys.stella.utils.skyblock.dungeons.players.DungeonPlayerManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.deftu.omnicore.api.client.client
import dev.deftu.omnicore.api.client.player
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import tech.thatgravyboat.skyblockapi.platform.pushPop
import java.io.InputStreamReader
import java.nio.file.Files

object Boss {
    private const val SIZE = 128
    private const val HALF_SIZE = 64f

    fun renderMap(context: GuiGraphics) {
        val p = player ?: return
        val floor = Dungeon.floorNumber ?: return
        val bMap = BossMapRegistry.getBossMap(floor, p.x, p.y, p.z) ?: return
        val tex = map.getOrLoad("boss/${bMap.image}") ?: return
        val sizeInWorld = minOf(bMap.widthInWorld, bMap.heightInWorld, bMap.renderSize ?: Int.MAX_VALUE).toDouble()
        val texScale = SIZE / minOf((bMap.width / bMap.widthInWorld.toDouble()) * (bMap.renderSize ?: bMap.widthInWorld), (bMap.height / bMap.heightInWorld.toDouble()) * (bMap.renderSize ?: bMap.heightInWorld))
        val w = (bMap.width * texScale).toInt()
        val h = (bMap.height * texScale).toInt()
        val viewX = (((p.x - bMap.topLeftLocation[0]) / sizeInWorld) * SIZE - HALF_SIZE).coerceIn(0.0, maxOf(0.0, bMap.width * texScale - SIZE))
        val viewZ = (((p.z - bMap.topLeftLocation[1]) / sizeInWorld) * SIZE - HALF_SIZE).coerceIn(0.0, maxOf(0.0, bMap.height * texScale - SIZE))

        context.pushPop {
            context.pose().translate(5f, 5f)
            context.enableScissor(0, 0, SIZE, SIZE)
            context.blit(RenderPipelines.GUI_TEXTURED, tex, (-viewX).toInt(), (-viewZ).toInt(), 0f, 0f, w, h, w, h, w, h)

            val you = p.name.string
            for (dp in DungeonPlayerManager.players) {
                if (dp == null || (!dp.alive && dp.name != you)) continue

                val pos = if (map.smoothMovement) dp.pos.getLerped() else dp.pos.raw
                val rx = pos?.realX ?: continue
                val rz = pos.realZ ?: continue
                val hudX = toHud(rx, bMap.topLeftLocation[0], sizeInWorld, viewX)
                val hudY = toHud(rz, bMap.topLeftLocation[1], sizeInWorld, viewZ)

                MapRenderer.renderPlayerIcon(context, dp, hudX, hudY, pos.yaw?.toFloat() ?: 0f)
            }

            context.disableScissor()
        }
    }

    data class BossMapData(
        val image: String, val bounds: List<List<Double>>,
        val width: Int, val height: Int,
        val widthInWorld: Int, val heightInWorld: Int,
        val topLeftLocation: List<Int>, val renderSize: Int? = null
    )

    object BossMapRegistry {
        private val gson = Gson()
        private val bossMaps = mutableMapOf<String, List<BossMapData>>()
        private val configFile = FabricLoader.getInstance().configDir.resolve("stella/assets/imagedata.json")
        init { load() }

        fun load() {
            if (!Files.exists(configFile)) {
                Stella.LOGGER.warn("Boss map data not found in config: $configFile")
                return
            }

            try {
                Files.newBufferedReader(configFile).use { reader ->
                    val type = object : TypeToken<Map<String, List<BossMapData>>>() {}.type
                    val data: Map<String, List<BossMapData>> = gson.fromJson(reader, type)
                    bossMaps.clear()
                    bossMaps.putAll(data)
                    Stella.LOGGER.info("Successfully loaded ${bossMaps.size} boss map entries from config.")
                }
            } catch (e: Exception) {
                Stella.LOGGER.error("Failed to load BossMap data from config", e)
            }
        }

        fun getBossMap(floor: Int, px: Double, py: Double, pz: Double): BossMapData? = bossMaps[floor.toString()]?.firstOrNull { m ->
            val b = m.bounds
            px in b[0][0]..b[1][0] && py in b[0][1]..b[1][1] && pz in b[0][2]..b[1][2]
        }
    }

    private fun toHud(v: Double, top: Int, sizeInWorld: Double, view: Double) = ((v - top) / sizeInWorld) * SIZE - view
}