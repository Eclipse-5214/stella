package co.stellarskys.stella.utils

import co.stellarskys.stella.events.AreaEvent
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.PacketEvent
//#if MC >= 1.21.5
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket
import net.minecraft.network.packet.s2c.play.TeamS2CPacket
//#elseif MC == 1.8.9
//$$ import net.minecraft.network.play.server.S38PacketPlayerListItem
//$$ import net.minecraft.network.play.server.S3EPacketTeams
//#endif

/*
 * Modified from Devonian code
 * Under GPL 3.0 License
 */
object LocationUtils {
    private val areaRegex = "^(?:Area|Dungeon): ([\\w ]+)$".toRegex()
    private val subAreaRegex = "^ ([⏣ф]) .*".toRegex()
    private val uselessRegex = "^[⏣ф] ".toRegex()
    private val lock = Any()
    private var cachedAreas = mutableMapOf<String?, Boolean>()
    private var cachedSubareas = mutableMapOf<String?, Boolean>()
    var dungeonFloor: String? = null
        private set
    var dungeonFloorNum: Int? = null
        private set
    var area: String? = null
        private set
    var subarea: String? = null
        private set

    init {
        EventBus.register<PacketEvent.Received> { event ->
            //#if MC >= 1.21.5
            when (val packet = event.packet) {
                is PlayerListS2CPacket -> {
                    val action = packet.actions.firstOrNull()
                    if (action != PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME && action != PlayerListS2CPacket.Action.ADD_PLAYER) return@register
                    packet.entries?.forEach { entry ->
                        val displayName = entry.displayName?.string ?: return@forEach

                        val line = displayName.removeEmotes()
                        val match = areaRegex.find(line) ?: return@forEach
                        val newArea = match.groupValues[1]
                        if (newArea.lowercase() != area) {
                            synchronized(lock) {
                                EventBus.post(AreaEvent.Main(newArea))
                                area = newArea.lowercase()
                            }
                        }
                    }
                }

                is TeamS2CPacket -> {
                    val prefix = packet.team.orElse(null)?.prefix?.string ?: ""
                    val suffix = packet.team.orElse(null)?.suffix?.string ?: ""
                    if (prefix.isEmpty() || suffix.isEmpty()) return@register

                    val line = prefix + suffix
                    if (!subAreaRegex.matches(line)) return@register
                    if (line.endsWith("cth") || line.endsWith("ch")) return@register
                    val cleanSubarea = line.clearCodes().replace(uselessRegex, "").trim().lowercase()
                    if (cleanSubarea != subarea) {
                        synchronized(lock) {
                            EventBus.post(AreaEvent.Sub(cleanSubarea))
                            subarea = cleanSubarea
                        }
                    }
                    if (line.contains("The Catacombs (") && !line.contains("Queue")) {
                        dungeonFloor = line.clearCodes().substringAfter("(").substringBefore(")")
                        dungeonFloorNum = dungeonFloor?.lastOrNull()?.digitToIntOrNull() ?: 0
                    }
                }
            }
            //#elseif MC == 1.8.9
            //$$ when (val packet = event.packet) {
            //$$    is S38PacketPlayerListItem -> {
            //$$        if (packet.action != S38PacketPlayerListItem.Action.UPDATE_DISPLAY_NAME && packet.action != S38PacketPlayerListItem.Action.ADD_PLAYER) return@register
            //$$        packet.entries?.forEach { entry ->
            //$$            val displayName = entry.displayName?.unformattedText ?: return@forEach
            //$$            val line = displayName.removeEmotes()
            //$$            val match = areaRegex.find(line) ?: return@forEach
            //$$           val newArea = match.groupValues[1]
            //$$           if (newArea != area) {
            //$$               synchronized(lock) {
            //$$                    EventBus.post(AreaEvent.Main(newArea))
            //$$                    area = newArea.lowercase()
            //$$                }
            //$$            }
            //$$        }
            //$$    }
            //$$    is S3EPacketTeams -> {
            //$$        val teamPrefix = packet.prefix
            //$$        val teamSuffix = packet.suffix
            //$$        if (teamPrefix.isEmpty() || teamSuffix.isEmpty()) return@register
            //$$
            //$$        val line = teamPrefix + teamSuffix
            //$$        if (!subAreaRegex.matches(line.clearCodes())) return@register
            //$$        if (line.endsWith("cth") || line.endsWith("ch")) return@register
            //$$        val cleanSubarea = line.clearCodes().replace(uselessRegex, "").trim().lowercase()
            //$$        if (cleanSubarea != subarea) {
            //$$            synchronized(lock) {
            //$$                EventBus.post(AreaEvent.Sub(cleanSubarea))
            //$$                subarea = cleanSubarea
            //$$            }
            //$$        }
            //$$        if (line.contains("The Catacombs (") && !line.contains("Queue")) {
            //$$            dungeonFloor = line.clearCodes().substringAfter("(").substringBefore(")")
            //$$            dungeonFloorNum = dungeonFloor?.lastOrNull()?.digitToIntOrNull() ?: 0
            //$$        }
            //$$    }
            //$$ }
            //#endif
        }

        EventBus.register<AreaEvent.Main> ({
            synchronized(lock) {
                cachedAreas.clear()
            }
        })

        EventBus.register<AreaEvent.Sub> ({
            synchronized(lock) {
                cachedSubareas.clear()
            }
        })
    }

    fun checkArea(areaLower: String?): Boolean {
        return synchronized(lock) {
            cachedAreas.getOrPut(areaLower) {
                areaLower?.let { area == it } ?: true
            }
        }
    }

    fun checkSubarea(subareaLower: String?): Boolean {
        return synchronized(lock) {
            cachedSubareas.getOrPut(subareaLower) {
                subareaLower?.let { subarea?.contains(it) == true } ?: true
            }
        }
    }
}