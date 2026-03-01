package co.stellarskys.stella.features.stellanav

import co.stellarskys.stella.Stella
import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.GuiEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.features.stellanav.render.MapRenderer
import co.stellarskys.stella.hud.HUDManager
import co.stellarskys.stella.utils.config
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation
import java.awt.Color

@Module
object map: Feature("mapEnabled", island = SkyBlockIsland.THE_CATACOMBS) {
    private const val name = "StellaNav"

    // textures
    val SELF_MARKER = ResourceLocation.fromNamespaceAndPath(Stella.NAMESPACE, "stellanav/markerself")
    val OTHER_MARKER = ResourceLocation.fromNamespaceAndPath(Stella.NAMESPACE, "stellanav/markerother")
    val DEFAULT_MAP = ResourceLocation.fromNamespaceAndPath(Stella.NAMESPACE, "stellanav/defaultmap")

    // main map configs
    val bossMapEnabled by config.property<Boolean>("bossMapEnabled")
    val hideInBoss by config.property<Boolean>("hideInBoss")
    val scoreMapEnabled by config.property<Boolean>("scoreMapEnabled")
    val mapInfoUnder by config.property<Boolean>("mapInfoUnder")

    // display
    val mapBgColor by config.property<Color>("mapBgColor")
    val mapBorder by config.property<Boolean>("mapBorder")
    val mapBdColor by config.property<Color>("mapBdColor")
    val mapBdWidth by config.property<Int>("mapBdWidth")
    val nameScale by config.property<Float>("nameScale")
    val secretScale by config.property<Float>("secretScale")
    val checkmarkScale by config.property<Float>("checkmarkScale")
    val textShadow by config.property<Boolean>("mtextshadow")

    // behavior
    val roomCheck by config.property<Boolean>("roomCheck")
    val roomName by config.property<Boolean>("roomName")
    val roomSecrets by config.property<Boolean>("roomSecrets")

    val puzzleCheck by config.property<Boolean>("puzzleCheck")
    val puzzleName by config.property<Boolean>("puzzleName")
    val puzzleSecrets by config.property<Boolean>("puzzleSecrets")

    val checkAnchor by config.property<Int>("checkAnchor")
    val nameAnchor by config.property<Int>("nameAnchor")
    val secretsAnchor by config.property<Int>("secretsAnchor")

    val prioMiddle by config.property<Boolean>("prioMiddle")
    val replaceText by config.property<Boolean>("replaceText")

    // map colors
    val NormalColor by config.property<Color>("normalRoomColor")
    val PuzzleColor by config.property<Color>("puzzleRoomColor")
    val TrapColor by config.property<Color>("trapRoomColor")
    val MinibossColor by config.property<Color>("minibossRoomColor")
    val BloodColor by config.property<Color>("bloodRoomColor")
    val FairyColor by config.property<Color>("fairyRoomColor")
    val EntranceColor by config.property<Color>("entranceRoomColor")

    val NormalDoorColor by config.property<Color>("normalDoorColor")
    val WitherDoorColor by config.property<Color>("witherDoorColor")
    val BloodDoorColor by config.property<Color>("bloodDoorColor")
    val EntranceDoorColor by config.property<Color>("entranceDoorColor")

    // icon settings
    val iconScale by config.property<Float>("iconScale")
    val smoothMovement by config.property<Boolean>("smoothMovement")
    val showPlayerHead by config.property<Boolean>("showPlayerHeads")
    val ownDefault by config.property<Boolean>("ownDefault")
    val iconBorderWidth by config.property<Float>("iconBorderWidth")
    val iconBorderColor by config.property<Color>("iconBorderColor")
    val iconClassColors by config.property<Boolean>("iconClassColors")
    val showNames by config.property<Boolean>("showNames")
    val dontShowOwn by config.property<Boolean>("dontShowOwn")

    // other
    val hiddenRooms = false
    val tint = 0.7

    override fun initialize() {
        HUDManager.registerCustom(name, 148, 148, this::HUDEditorRender)

        on<GuiEvent.RenderHUD> { event ->
            RenderMap(event.context)
        }
    }

    fun HUDEditorRender(context: GuiGraphics){
        MapRenderer.renderPreview(context, 5f, 5f, 1f)
    }

    fun RenderMap(context: GuiGraphics) {
        val x = HUDManager.getX(name)
        val y = HUDManager.getY(name)
        val scale = HUDManager.getScale(name)

        MapRenderer.render(context, x, y, scale)
    }
}