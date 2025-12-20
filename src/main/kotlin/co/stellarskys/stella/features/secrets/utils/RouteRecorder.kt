package co.stellarskys.stella.features.secrets.utils

import co.stellarskys.stella.Stella
import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.DungeonEvent
import co.stellarskys.stella.events.core.RenderEvent
import co.stellarskys.stella.events.core.SoundEvent
import co.stellarskys.stella.events.core.TickEvent
import co.stellarskys.stella.features.secrets.secretRoutes
import co.stellarskys.stella.hud.HUDManager
import co.stellarskys.stella.utils.ChatUtils
import co.stellarskys.stella.utils.Utils.calcDistanceSq
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import co.stellarskys.stella.utils.skyblock.dungeons.map.Room
import dev.deftu.omnicore.api.client.player
import dev.deftu.omnicore.api.client.world
import net.fabricmc.loader.impl.lib.sat4j.pb.constraints.pb.WatchPb
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.BlockPos
import net.minecraft.sounds.SoundEvents
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import kotlin.collections.setOf

@Module
object RouteRecorder {
    var recording = false
        private set

    private val route = mutableListOf<StepData>()
    private var stepIndex = 0
    private var currentRoom: Room? = null
    private var lastPlayerPos: BlockPos? = null

    val currentStep: StepData get() = route[stepIndex]
    val lastStep: StepData? get() = route.getOrNull(stepIndex - 1)

    init {
        EventBus.on<DungeonEvent.Room.Change>(SkyBlockIsland.THE_CATACOMBS) {
            if (!recording) return@on

            ChatUtils.fakeMessage("${Stella.PREFIX} §cError: left room, stopping")
            stopRecording()
        }

        EventBus.on<DungeonEvent.Secrets.Bat>(SkyBlockIsland.THE_CATACOMBS) { if (!recording) return@on; addWaypoint(WaypointType.BAT, it.blockPos) }
        EventBus.on<DungeonEvent.Secrets.Chest>(SkyBlockIsland.THE_CATACOMBS) { if (!recording) return@on; addWaypoint(WaypointType.CHEST, it.blockPos) }
        EventBus.on<DungeonEvent.Secrets.Essence>(SkyBlockIsland.THE_CATACOMBS) { if (!recording) return@on; addWaypoint(WaypointType.ESSENCE, it.blockPos)}

        EventBus.on<DungeonEvent.Secrets.Item>(SkyBlockIsland.THE_CATACOMBS) { event ->
            if (!recording) return@on
            val pos = world?.getEntity(event.entityId)?.blockPosition() ?: return@on
            val lastSecretPos = lastStep?.waypoints?.firstOrNull { it.type == WaypointType.CHEST }?.pos
            if (lastSecretPos != null && calcDistanceSq(lastSecretPos, pos) < 2) return@on
            addWaypoint(WaypointType.ITEM, pos)
        }


        EventBus.on<DungeonEvent.Secrets.Misc>(SkyBlockIsland.THE_CATACOMBS) {
            if (!recording) return@on

            when (it.secretType) {
                DungeonEvent.Secrets.Type.RED_SKULL -> { /*TODO*/ }
                DungeonEvent.Secrets.Type.LEVER -> { addWaypoint(WaypointType.LEVER, it.blockPos) }
            }
        }

        EventBus.on<SoundEvent.Play>(SkyBlockIsland.THE_CATACOMBS) { event ->
            if (!recording) return@on

            val healdItem = player?.mainHandItem?.hoverName?.stripped ?: ""
            val sound = event.sound

            if (sound.location == SoundEvents.ENDER_DRAGON_HURT.location) {
                val pos = BlockPos((sound.x - 0.5).toInt(), (sound.y - 1).toInt(), (sound.z - 0.5).toInt())
                addWaypoint(WaypointType.ETHERWARP, pos)
            }

            if (sound.location == SoundEvents.GENERIC_EXPLODE.value().location) {
                if (setOf("boom TNT", "Explosive Bow").none { healdItem.contains(it) }) return@on
                val pos = BlockPos((sound.x - 0.5).toInt(), (sound.y - 0.5).toInt(), (sound.z - 0.5).toInt())
                addWaypoint(WaypointType.SUPERBOOM, pos)
            }

            if (sound.location.toString().contains("break")) {
                if(!healdItem.contains("Dungeonbreaker")) return@on
                val pos = BlockPos((sound.x - 0.5).toInt(), (sound.y - 0.5).toInt(), (sound.z - 0.5).toInt())
                addWaypoint(WaypointType.MINE, pos)
            }
        }


        EventBus.on<TickEvent.Client>(SkyBlockIsland.THE_CATACOMBS) {
            if (!recording) return@on

            val room = Dungeon.currentRoom ?: return@on
            val loc = player?.onPos ?: return@on
            val pos = room.getRoomCoord(BlockPos(loc.x, loc.y + 1, loc.z))

            if (lastPlayerPos == null || calcDistanceSq(pos, lastPlayerPos!!) > 4) {
                currentStep.line += pos
                lastPlayerPos = pos
            }
        }

        EventBus.on<RenderEvent.World.Last>(SkyBlockIsland.THE_CATACOMBS) { event ->
            if (!recording) return@on

            RoutePlayer.renderRecordingRoute(currentStep, lastStep, event.context)
        }
    }

    fun nextStep() {
        stepIndex++
        if (stepIndex >= route.size) {
            route.add(StepData(mutableListOf(), mutableListOf()))
            val loc = player?.onPos ?: return
            val pos = currentRoom?.getRoomCoord(BlockPos(loc.x, loc.y + 1, loc.z)) ?: return
            currentStep.line += pos
        }
    }

    fun previousStep() {
        if (stepIndex > 0) stepIndex--
    }

    fun getRoute(): List<StepData> = route

    fun startRecording() {
        val room = Dungeon.currentRoom
        if (room?.name == null) {
            ChatUtils.fakeMessage("${Stella.PREFIX} §cNot in a valid dungeon room")
            return
        }

        currentRoom = room
        route.clear()
        stepIndex = 0
        route.add(StepData(mutableListOf(), mutableListOf()))
        recording = true

        ChatUtils.fakeMessage("${Stella.PREFIX} §aStarted route recording for ${room.name}")
    }

    fun stopRecording() {
        recording = false
        ChatUtils.fakeMessage("${Stella.PREFIX} §cStopped Recording")
    }

    fun saveRoute() {
        if (currentRoom?.name == null || !recording || route.isEmpty()) {
            ChatUtils.fakeMessage("${Stella.PREFIX} §cNo route to save")
            return
        }

        RouteRegistry.saveRoute(currentRoom?.name ?: return, getRoute())
        RouteRegistry.reload()
        ChatUtils.fakeMessage("${Stella.PREFIX} §aSaved route for ${currentRoom?.name}")
        stopRecording()
    }

    fun reloadRoutes() {
        RouteRegistry.reload()
        ChatUtils.fakeMessage("${Stella.PREFIX} §aReloaded routes")
    }

    fun addWaypoint(type: WaypointType, pos: BlockPos) {
        val room = Dungeon.currentRoom ?: return
        val relPos = room.getRoomCoord(pos)

        val waypoint = WaypointData(relPos, type)
        currentStep.waypoints += waypoint

        if (type in WaypointType.SECRET) nextStep()
    }

    fun hudPreview(context: GuiGraphics) {
        val matirix = context.pose()

        matirix.pushMatrix()
        matirix.translate(5f, 5f)

        if(secretRoutes.minimized) {
            Render2D.drawString(context, "§a▶ Recording", 0, 0)
        } else {
            Render2D.drawString(context, "§bRecording Room §dSupertall", 0, 0)
            Render2D.drawString(context, "§7On Step§8: §b2", 0, 10)
            Render2D.drawString(context, "§7Line Nodes§8: §b2", 0, 20)
            Render2D.drawString(context, "§7Etherwarps§8: §b5", 0, 30)
            Render2D.drawString(context, "§7Superbooms§8: §b1", 0, 40)
            Render2D.drawString(context, "§7Levers§8: §b1", 0, 50)
            Render2D.drawString(context, "§7Stonks§8: §b6", 0, 60)
            Render2D.drawString(context, "§7Custom Waypoints§8: §b0", 0, 70)
            Render2D.drawString(context, "§7Secrets§8: §b1§7/§66", 0, 80)
            Render2D.drawString(context, "§7Last Secret§8: §7(§610§7, §660§7, §620§7)", 0, 90)
        }

        matirix.popMatrix()
    }

    fun hud(context: GuiGraphics) {
        val matrix = context.pose()

        val x = HUDManager.getX(secretRoutes.rHudName)
        val y = HUDManager.getY(secretRoutes.rHudName)
        val scale = HUDManager.getScale(secretRoutes.rHudName)

        matrix.pushMatrix()
        matrix.translate(x,y)
        matrix.scale(scale)
        matrix.translate(5f, 5f)

        if(!recording) {
            if(secretRoutes.minimized) Render2D.drawString(context, "§c■ Not Recording", 0, 0)
            else Render2D.drawString(context, "§cNot Recording", 0, 0)
        } else {
            val etherwarps = currentStep.waypoints.filter { it.type == WaypointType.ETHERWARP }.size
            val superbooms = currentStep.waypoints.filter { it.type == WaypointType.SUPERBOOM }.size
            val levers = currentStep.waypoints.filter { it.type == WaypointType.LEVER }.size
            val stonks = currentStep.waypoints.filter { it.type == WaypointType.MINE }.size
            val customs = currentStep.waypoints.filter { it.type == WaypointType.CUSTOM }.size

            val lastSecretType = lastStep?.waypoints?.firstOrNull { it.type in WaypointType.SECRET}?.type ?: "§cNone"

            if(secretRoutes.minimized) {
                Render2D.drawString(context, "§a▶ Recording", 0, 0)
            } else {
                Render2D.drawString(context, "§bRecording Room §d${currentRoom?.name}", 0, 0)
                Render2D.drawString(context, "§7On Step§8: §b$stepIndex", 0, 10)
                Render2D.drawString(context, "§7Line Nodes§8: §b${currentStep.line.size}", 0, 20)
                Render2D.drawString(context, "§7Etherwarps§8: §b$etherwarps", 0, 30)
                Render2D.drawString(context, "§7Superbooms§8: §b$superbooms", 0, 40)
                Render2D.drawString(context, "§7Levers§8: §b$levers", 0, 50)
                Render2D.drawString(context, "§7Stonks§8: §b$stonks", 0, 60)
                Render2D.drawString(context, "§7Custom Waypoints§8: §b$customs", 0, 70)
                Render2D.drawString(context, "§7Secrets§8: §b${Dungeon.currentRoom?.secretsFound}§8/§6${Dungeon.currentRoom?.secrets}", 0, 80)
                Render2D.drawString(context, "§7Last Secret§8: $lastSecretType", 0, 90)
            }
        }

        matrix.popMatrix()
    }
}