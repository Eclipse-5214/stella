package co.stellarskys.stella.utils.skyblock.dungeons

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.*
import co.stellarskys.stella.utils.*
import co.stellarskys.stella.utils.skyblock.dungeons.map.*
import co.stellarskys.stella.utils.skyblock.dungeons.players.DungeonPlayerManager
import co.stellarskys.stella.utils.skyblock.dungeons.score.*
import co.stellarskys.stella.utils.skyblock.dungeons.utils.*
import dev.deftu.omnicore.api.client.player
import dev.deftu.omnicore.api.client.world
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.SkullBlockEntity
import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonAPI
import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonFloor
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.platform.properties
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import java.awt.Color
import kotlin.math.roundToInt

/**
 * Central dungeon state manager.
 * Basically one-stop shop for everything dungeons
 */
@Module
object Dungeon {
    // Textures
    private const val RED_SKULL_TEXTURE = "eyJ0aW1lc3RhbXAiOjE1NzA5MTUxODU0ODUsInByb2ZpbGVJZCI6IjVkZTZlMTg0YWY4ZDQ5OGFiYmRlMDU1ZTUwNjUzMzE2IiwicHJvZmlsZU5hbWUiOiJBc3Nhc2luSmlhbmVyMjUiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2EyMjNlMzZhYzEzZjBmNzFhYmNmYmYwYzk2ZmRjMjAxMGNjM2UxMWZmMmIwZDgxMTJkMGU2M2Y0YjRhYWEwZGUifX19"
    private const val WITHER_ESSENCE_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTYwMzYxMDQ0MzU4MywKICAicHJvZmlsZUlkIiA6ICIzM2ViZDMyYmIzMzk0YWQ5YWM2NzBjOTZjNTQ5YmE3ZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJEYW5ub0JhbmFubm9YRCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9lNDllYzdkODJiMTQxNWFjYWUyMDU5Zjc4Y2QxZDE3NTRiOWRlOWIxOGNhNTlmNjA5MDI0YzRhZjg0M2Q0ZDI0IgogICAgfQogIH0KfQ==ewogICJ0aW1lc3RhbXAiIDogMTYwMzYxMDQ0MzU4MywKICAicHJvZmlsZUlkIiA6ICIzM2ViZDMyYmIzMzk0YWQ5YWM2NzBjOTZjNTQ5YmE3ZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJEYW5ub0JhbmFubm9YRCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9lNDllYzdkODJiMTQxNWFjYWUyMDU5Zjc4Y2QxZDE3NTRiOWRlOWIxOGNhNTlmNjA5MDI0YzRhZjg0M2Q0ZDI0IgogICAgfQogIH0KfQ=="

    // Regex patterns for chat parsing
    private val WATCHER_PATTERN = Regex("""\[BOSS] The Watcher: That will be enough for now\.""")
    private val DUNGEON_COMPLETE_PATTERN = Regex("""^\s*(Master Mode)?\s?(?:The)? Catacombs - (Entrance|Floor .{1,3})$""")
    private val ROOM_SECRETS_PATTERN = Regex("""\b([0-9]|10)/([0-9]|10)\s+Secrets\b""")

    // Room and door data
    val rooms = Array<Room?>(36) { null }
    val doors = Array<Door?>(60) { null }
    val uniqueRooms = mutableSetOf<Room>()
    val uniqueDoors = mutableSetOf<Door>()
    val discoveredRooms = mutableMapOf<String, DiscoveredRoom>()

    // Dungeon state
    var bloodClear = false
    var bloodDone = false
    var complete = false
    var currentRoom: Room? = null
    var holdingLeaps = false

    // Floor info
    val floor: DungeonFloor? get() = DungeonAPI.dungeonFloor
    val floorNumber: Int? get() = floor?.floorNumber
    val inBoss: Boolean
        get() = floor != null && player?.let {
            val (x, z) = WorldScanUtils.realCoordToComponent(it.x.toInt(), it.z.toInt())
            6 * z + x > 35
        } == true

    // HUD lines
    var mapLine1 = ""
    var mapLine2 = ""

    // Shortcuts
    val players get() = DungeonPlayerManager.players
    val score get() = DungeonScore.score

    // Class Colors
    val healerColor by config.property<Color>("healerColor")
    val mageColor by config.property<Color>("mageColor")
    val berzColor by config.property<Color>("berzColor")
    val archerColor by config.property<Color>("archerColor")
    val tankColor by config.property<Color>("tankColor")

    // Secret Items
    private val secretItems = setOf(
        "Architect's First Draft",
        "Candycomb",
        "Decoy",
        "Defuse Kit",
        "Dungeon Chest Key",
        "Healing VIII Splash Potion",
        "Inflatable Jerry",
        "Revive Stone",
        "Secret Dye",
        "Spirit Leap",
        "Training Weights",
        "Trap",
        "Treasure Talisman"
    )

    data class DiscoveredRoom(val x: Int, val z: Int, val room: Room)

    /** Initializes all dungeon systems and event listeners */
    init {
        EventBus.on<LocationEvent.AreaChange>(SkyBlockIsland.THE_CATACOMBS) { event ->
            floor?.let { EventBus.post(DungeonEvent.Enter(it)) }
        }

        EventBus.on<LocationEvent.IslandChange> { reset() }

        EventBus.on<ChatEvent.Receive>(SkyBlockIsland.THE_CATACOMBS) { event ->
            val msg = event.message.string.clearCodes()
            if (WATCHER_PATTERN.containsMatchIn(msg)) bloodDone = true
            if (DUNGEON_COMPLETE_PATTERN.containsMatchIn(msg)) {
                DungeonPlayerManager.updateAllSecrets()
                complete = true
                floor?.let { EventBus.post(DungeonEvent.End(it)) }
            }

            if (!event.isActionBar) return@on

            val room = currentRoom ?: return@on
            val match = ROOM_SECRETS_PATTERN.find(event.message.stripped) ?: return@on
            val (found, total) = match.destructured
            val secrets = found.toInt()
            val max = total.toInt()
            if (secrets != room.secretsFound) room.secretsFound = secrets
            if (max != room.secrets) room.secrets = max
        }


        EventBus.on<TickEvent.Client>(SkyBlockIsland.THE_CATACOMBS) {
            updateHudLines()
            updateHeldItem()
        }

        EventBus.on<PacketEvent.Received>(SkyBlockIsland.THE_CATACOMBS) { event ->
            if (event.packet !is ClientboundTakeItemEntityPacket) return@on
            val world = world ?: return@on
            val entity = world.getEntity(event.packet.itemId) as? ItemEntity ?: return@on
            val name = entity.item.displayName.stripped.drop(1).dropLast(1)

            if (secretItems.contains(name)) {
                EventBus.post(DungeonEvent.Secrets.Item(event.packet.itemId, entity))

                currentRoom?.roomData?.secretCoords?.item?.find {
                    Utils.calcDistance(
                        currentRoom!!.getRealCoord(it.toBlockPos()),
                        entity.blockPosition()
                    ) < 25
                }?.collected = true
            }
        }

        EventBus.on<PacketEvent.Sent>(SkyBlockIsland.THE_CATACOMBS) { event ->
            if (event.packet !is ServerboundUseItemOnPacket) return@on
            val pos = event.packet.hitResult.blockPos ?: return@on
            val world = world ?: return@on
            val blockState = world.getBlockState(pos)

            when (blockState.block) {
                Blocks.CHEST, Blocks.TRAPPED_CHEST -> {
                    EventBus.post(DungeonEvent.Secrets.Chest(blockState, pos))

                    currentRoom?.roomData?.secretCoords?.chest?.find {
                        currentRoom?.getRealCoord(it.toBlockPos()) == pos
                    }?.collected = true
                }
                Blocks.LEVER -> {
                    EventBus.post(DungeonEvent.Secrets.Misc(DungeonEvent.Secrets.Type.LEVER, pos))
                }
                else -> {
                    val entity = world.getBlockEntity(pos) ?: return@on
                    if (entity is SkullBlockEntity) {
                        val texture = entity.ownerProfile?.properties?.get("textures")?.firstOrNull()?.value
                        when (texture) {
                            WITHER_ESSENCE_TEXTURE -> {
                                EventBus.post(DungeonEvent.Secrets.Essence(entity, pos))

                                currentRoom?.roomData?.secretCoords?.wither?.find {
                                    currentRoom?.getRealCoord(it.toBlockPos()) == pos
                                }?.collected = true
                            }
                            RED_SKULL_TEXTURE -> {
                                EventBus.post(DungeonEvent.Secrets.Misc(DungeonEvent.Secrets.Type.RED_SKULL, pos))

                                currentRoom?.roomData?.secretCoords?.redstoneKey?.find {
                                    currentRoom?.getRealCoord(it.toBlockPos()) == pos
                                }?.collected = true
                            }
                        }
                    }
                }
            }
        }

        EventBus.on<SoundEvent.Play>(SkyBlockIsland.THE_CATACOMBS) { event ->
            val sound = event.sound
            if (sound.location == SoundEvents.BAT_DEATH.location) {
                val pos = BlockPos(sound.x.roundToInt(), sound.y.roundToInt(), sound.z.roundToInt())
                EventBus.post(DungeonEvent.Secrets.Bat(pos))

                currentRoom?.roomData?.secretCoords?.bat?.find {
                    Utils.calcDistance(
                        currentRoom!!.getRealCoord(it.toBlockPos()),
                        pos
                    ) < 100
                }?.collected = true
            }
        }

        RoomRegistry.loadFromRemote()
        WorldScanner.init()
        DungeonPlayerManager.init()
        DungeonScore.init()
        MapUtils.init()
    }

    /** Clears all dungeon state */
    fun reset() {
        rooms.fill(null)
        doors.fill(null)
        uniqueRooms.clear()
        uniqueDoors.clear()
        discoveredRooms.clear()
        currentRoom = null
        bloodClear = false
        bloodDone = false
        complete = false
        holdingLeaps = false
        mapLine1 = ""
        mapLine2 = ""
        WorldScanner.reset()
        DungeonPlayerManager.reset()
        DungeonScore.reset()
        MapUtils.reset()
    }

    /** Updates HUD lines for map overlay */
    private fun updateHudLines() {
        val run = DungeonScore.data

        val dSecrets = "§7Secrets: §b${run.secretsFound}§8-§e${run.secretsRemaining}§8-§c${run.totalSecrets}"
        val dCrypts = "§7Crypts: " + when {
            run.crypts >= 5 -> "§a${run.crypts}"
            run.crypts > 0  -> "§e${run.crypts}"
            else            -> "§c0"
        }
        val dMimic = if (floorNumber in listOf(6, 7)) {
            "§7Mimic: " + if (MimicTrigger.mimicDead) "§a✔" else "§c✘"
        } else ""

        val minSecrets = "§7Min Secrets: " + when {
            run.secretsFound == 0 -> "§b?"
            run.minSecrets > run.secretsFound -> "§e${run.minSecrets}"
            else -> "§a${run.minSecrets}"
        }

        val dDeaths = "§7Deaths: " + if (run.teamDeaths < 0) "§c${run.teamDeaths}" else "§a0"
        val dScore = "§7Score: " + when {
            run.score >= 300 -> "§a${run.score}"
            run.score >= 270 -> "§e${run.score}"
            else             -> "§c${run.score}"
        } + if (DungeonScore.hasPaul) " §b★" else ""

        mapLine1 = "$dSecrets    $dCrypts    $dMimic".trim()
        mapLine2 = "$minSecrets    $dDeaths    $dScore".trim()
    }

    /** Updates leap detection based on held item */
    private fun updateHeldItem() {
        val item = player?.mainHandItem ?: return
        holdingLeaps = "leap" in item.hoverName.string.clearCodes().lowercase()
    }

    // Room accessors
    fun getRoomIdx(comp: Pair<Int, Int>) = 6 * comp.second + comp.first
    fun getRoomAtIdx(idx: Int) = rooms.getOrNull(idx)
    fun getRoomAtComp(comp: Pair<Int, Int>) = getRoomAtIdx(getRoomIdx(comp))
    fun getRoomAt(x: Int, z: Int) = getRoomAtComp(WorldScanUtils.realCoordToComponent(x, z))

    // Door accessors
    fun getDoorIdx(comp: Pair<Int, Int>): Int {
        val base = ((comp.first - 1) shr 1) + 6 * comp.second
        return base - (base / 12)
    }

    fun getDoorAtIdx(idx: Int) = doors.getOrNull(idx)
    fun getDoorAtComp(comp: Pair<Int, Int>) = getDoorAtIdx(getDoorIdx(comp))
    fun getDoorAt(x: Int, z: Int) = getDoorAtComp(WorldScanUtils.realCoordToComponent(x, z))

    /** Adds a door to the map and tracks it as unique */
    fun addDoor(door: Door) {
        val idx = getDoorIdx(door.getComp())
        if (idx in doors.indices) {
            doors[idx] = door
            uniqueDoors += door
        }
    }

    /** Merges two rooms into one unified instance */
    fun mergeRooms(room1: Room, room2: Room) {
        uniqueRooms.remove(room2)
        for (comp in room2.components) {
            if (!room1.hasComponent(comp.first, comp.second)) {
                room1.addComponent(comp, update = false)
            }
            val idx = getRoomIdx(comp)
            if (idx in rooms.indices) rooms[idx] = room1
        }
        uniqueRooms += room1
        room1.update()
    }
}