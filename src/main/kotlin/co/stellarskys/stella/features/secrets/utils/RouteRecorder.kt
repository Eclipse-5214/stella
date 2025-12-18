package co.stellarskys.stella.features.secrets.utils

import co.stellarskys.stella.Stella
import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.DungeonEvent
import co.stellarskys.stella.events.core.RenderEvent
import co.stellarskys.stella.events.core.SoundEvent
import co.stellarskys.stella.events.core.TickEvent
import co.stellarskys.stella.utils.ChatUtils
import co.stellarskys.stella.utils.Utils.calcDistanceSq
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import dev.deftu.omnicore.api.client.player
import dev.deftu.omnicore.api.client.world
import net.minecraft.core.BlockPos
import net.minecraft.sounds.SoundEvents
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped

@Module
object RouteRecorder {
    var recording = false
        private set

    private val route = mutableListOf<StepData>()
    private var stepIndex = 0
    private var currentRoomName: String? = null
    private var lastPlayerPos: BlockPos? = null

    val currentStep: StepData get() = route[stepIndex]
    val lastStep: StepData? get() = if (stepIndex > 0) route[stepIndex - 1] else null

    init {
        EventBus.on<DungeonEvent.Room.Change>(SkyBlockIsland.THE_CATACOMBS) {
            if (!recording) return@on

            ChatUtils.fakeMessage("${Stella.PREFIX} §cError: left room, stopping")
            stopRecording()
        }

        EventBus.on<DungeonEvent.Secrets.Bat>(SkyBlockIsland.THE_CATACOMBS) { if (!recording) return@on; addWaypoint(WaypointType.SECRET, it.entity.blockPosition()) }
        EventBus.on<DungeonEvent.Secrets.Chest>(SkyBlockIsland.THE_CATACOMBS) { if (!recording) return@on; addWaypoint(WaypointType.SECRET, it.blockPos) }
        EventBus.on<DungeonEvent.Secrets.Essence>(SkyBlockIsland.THE_CATACOMBS) { if (!recording) return@on; addWaypoint(WaypointType.SECRET, it.blockPos)}

        EventBus.on<DungeonEvent.Secrets.Item>(SkyBlockIsland.THE_CATACOMBS) {
            if (!recording) return@on
            val pos = world?.getEntity(it.entityId)?.blockPosition() ?: return@on
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
                if(!healdItem.contains("Dungeon Breaker")) return@on
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
            if (!recording || lastStep == null) return@on

            RoutePlayer.renderRecordingRoute(currentStep, lastStep!!, event.context)
        }
    }

    fun nextStep() {
        stepIndex++
        if (stepIndex >= route.size) {
            route.add(StepData(mutableListOf(), mutableListOf()))
        }
    }

    fun previousStep() {
        if (stepIndex > 0) stepIndex--
    }

    fun getRoute(): List<StepData> = route

    fun startRecording() {
        val roomName = Dungeon.currentRoom?.name
        if (roomName == null) {
            ChatUtils.fakeMessage("${Stella.PREFIX} §cNot in a valid dungeon room")
            return
        }

        recording = true
        currentRoomName = roomName
        route.clear()
        stepIndex = 0
        route.add(StepData(mutableListOf(), mutableListOf()))

        ChatUtils.fakeMessage("${Stella.PREFIX} §aStarted route recording for $roomName")
    }

    fun addWaypoint(type: WaypointType, pos: BlockPos) {
        val room = Dungeon.currentRoom ?: return
        val relPos = room.getRoomCoord(pos)

        val waypoint = WaypointData(relPos, type)
        currentStep.waypoints += waypoint

        if (type == WaypointType.SECRET) nextStep()
    }

    fun stopRecording() {
        recording = false
    }
}