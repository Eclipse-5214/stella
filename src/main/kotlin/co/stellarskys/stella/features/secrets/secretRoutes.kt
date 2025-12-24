package co.stellarskys.stella.features.secrets

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.DungeonEvent
import co.stellarskys.stella.events.core.GuiEvent
import co.stellarskys.stella.events.core.RenderEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.features.secrets.utils.RoutePlayer
import co.stellarskys.stella.features.secrets.utils.RouteRecorder
import co.stellarskys.stella.features.secrets.utils.RouteRecorder.currentStep
import co.stellarskys.stella.features.secrets.utils.RouteRegistry
import co.stellarskys.stella.features.secrets.utils.StepData
import co.stellarskys.stella.features.secrets.utils.WaypointData
import co.stellarskys.stella.features.secrets.utils.WaypointType
import co.stellarskys.stella.hud.HUDManager
import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.config.RGBA
import co.stellarskys.stella.utils.config.core.Keybind
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import co.stellarskys.stella.utils.skyblock.dungeons.map.Room
import co.stellarskys.stella.utils.skyblock.dungeons.utils.Checkmark
import dev.deftu.omnicore.api.client.player
import dev.deftu.omnicore.api.client.world
import net.minecraft.core.BlockPos
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

@Module
object secretRoutes: Feature("secretRoutes", island = SkyBlockIsland.THE_CATACOMBS) {
    val onlyRenderAfterClear by config.property<Boolean>("secretRoutes.onlyRenderAfterClear")
    val stopRenderAfterGreen by config.property<Boolean>("secretRoutes.stopRenderAfterGreen")
    var nextStepBind by config.property<Keybind.Handler>("secretRoutes.nextStep")
    var lastStepBind by config.property<Keybind.Handler>("secretRoutes.lastStep")
    var routeFile by config.property<String>("secretRoutes.fileName")
    val renderText by config.property<Boolean>("secretRoutes.renderText")

    val startColor by config.property<RGBA>("secretRoutes.startColor")
    val mineColor by config.property<RGBA>("secretRoutes.mineColor")
    val superBoomColor by config.property<RGBA>("secretRoutes.superboomColor")
    val etherWarpColor by config.property<RGBA>("secretRoutes.etherwarpColor")
    val chestColor by config.property<RGBA>("secretRoutes.chestColor")
    val itemColor by config.property<RGBA>("secretRoutes.itemColor")
    val essenceColor by config.property<RGBA>("secretRoutes.essenceColor")
    val batColor by config.property<RGBA>("secretRoutes.batColor")
    val leverColor by config.property<RGBA>("secretRoutes.leverColor")

    val recordingHud by config.property<Boolean>("secretRoutes.recordingHud")
    val minimized by config.property<Boolean>("secretRoutes.recordingHud.minimized")
    val rHudName = "rhud"

    private var stepIndex = 0
    private var route: List<StepData> = emptyList()
    private var currentRoom: Room? = null
    private val currentStep: StepData? get() = route.getOrNull(stepIndex)
    private val firstStep: Boolean get() = route.indexOf(currentStep) == 0
    private val currentSecret: WaypointData? get() = currentStep?.waypoints?.firstOrNull() { it.type in WaypointType.SECRET }

    init {
        HUDManager.registerCustom(rHudName, 100, 110, RouteRecorder::hudPreview, "secretRoutes.recordingHud")
        on<GuiEvent.RenderHUD> { if (recordingHud) RouteRecorder.hud(it.context) }

        on<DungeonEvent.Secrets.Chest> { event ->
            val secPos = currentRoom?.getRealCoord(currentSecret?.pos ?: return@on) ?: return@on
            if ( secPos == event.blockPos) nextStep()
        }

        on<DungeonEvent.Secrets.Essence> { event ->
            val secPos = currentRoom?.getRealCoord(currentSecret?.pos ?: return@on) ?: return@on
            if ( secPos == event.blockPos) nextStep()
        }

        on<DungeonEvent.Secrets.Item> { event ->
            val secPos = currentRoom?.getRealCoord(currentSecret?.pos ?: return@on) ?: return@on
            val pos = world?.getEntity(event.entityId)?.blockPosition() ?: return@on
            if (Utils.calcDistance(pos, secPos) < 25) nextStep()
        }

        on<DungeonEvent.Secrets.Bat> { event ->
            val secPos = currentRoom?.getRealCoord(currentSecret?.pos ?: return@on) ?: return@on
            if (Utils.calcDistance(event.blockPos, secPos) < 100) nextStep()
        }

        on<DungeonEvent.Room.Change> { event ->
            currentRoom = event.new
            stepIndex = 0
            route = RouteRegistry.getRoute(currentRoom?.name ?: return@on) ?: emptyList()
        }

        on<RenderEvent.World.Last> { event ->
            if (currentRoom == null || route.isEmpty() || currentStep == null) return@on
            if (onlyRenderAfterClear && currentRoom?.checkmark in setOf(Checkmark.NONE, Checkmark.UNEXPLORED, Checkmark.UNDISCOVERED)) return@on
            if (stopRenderAfterGreen && currentRoom?.checkmark == Checkmark.GREEN) return@on

            RoutePlayer.renderRoute(currentStep!!, firstStep, event.context)
        }

        nextStepBind.onPress {
            if(!this@secretRoutes.isEnabled() || RouteRecorder.recording) return@onPress
            nextStep()
        }

        lastStepBind.onPress{
            if(!this@secretRoutes.isEnabled() || RouteRecorder.recording) return@onPress
            previousStep()
        }
    }

    fun nextStep() {
        if(stepIndex < route.size - 1) stepIndex++
    }

    fun previousStep() {
        if (stepIndex > 0) stepIndex--
    }
}
