package co.stellarskys.stella.features.dungeons

import co.stellarskys.stella.Stella
import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.api.dungeons.Dungeon
import co.stellarskys.stella.api.dungeons.utils.DungeonClass
import co.stellarskys.stella.api.handlers.Signal
import co.stellarskys.stella.api.handlers.Signal.color
import co.stellarskys.stella.api.handlers.Signal.onHover
import co.stellarskys.stella.api.hypixel.HypixelApi
import co.stellarskys.stella.api.hypixel.SkyblockResponse
import co.stellarskys.stella.events.core.ChatEvent
import co.stellarskys.stella.features.Feature
import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped

@Module
object JoinInfo: Feature("joinInfo", island = SkyBlockIsland.DUNGEON_HUB) {
    private val JoinRegex = "Party Finder > (\\w+) joined the dungeon group!".toRegex()

    override fun initialize() {
        on<ChatEvent.Receive> { event ->
            val person = JoinRegex.find(event.message.stripped)?.groups?.get(1)?.value ?: return@on
            fetchAndDisplayStats(person)
        }
    }

    fun fetchAndDisplayStats(name:String) {
        HypixelApi.getUuid(name) { uuid ->
            if (uuid == null) {
                Signal.fakeMessage("${Stella.PREFIX} §cNo UUID found for $name")
                return@getUuid
            }
            HypixelApi.fetchSkyblockProfile(uuid, 600_000L) { profile ->
                if (profile == null) {
                    Signal.fakeMessage("${Stella.PREFIX} §cNo Profile found for $name")
                    return@fetchSkyblockProfile
                }
                displayCataStats(profile, name)
            }
        }
    }

    fun displayCataStats(player: SkyblockResponse.SkyblockMember, name: String) {
        with(player.dungeons) {
            val normal = dungeonTypes.catacombs
            val master = dungeonTypes.mastermode

            Component.literal("§b§l${Signal.LINE}\n§dCatacomb stats for§8: §b$name\n\n")
                .append(buildCataLine(this))
                .append(buildFloorTimes(normal, master))
                .append(buildArmor(player))
                .append(buildItemLines(player))
                .append("§b§l${Signal.LINE}")
                .let { Signal.fakeMessage(it) }
        }
    }

    private fun buildCataLine(data: SkyblockResponse.DungeonsData) = Component.empty().apply {
        with(data) {
            val cata = Dungeon.calculateDungeonLevel(dungeonTypes.catacombs.experience)
            val classLevels = mutableListOf<Double>()
            val cataHover = Component.literal("§bClasses\n").apply {
                DungeonClass.entries.filter { it !in setOf(DungeonClass.DEAD, DungeonClass.UNKNOWN) }.forEach {
                    val exp = data.classes[it.name.lowercase()]?.experience ?: 0.0
                    append(Component.literal(it.displayName).color(it.color?.rgb ?: -1))
                    append("§8: §b${Dungeon.calculateDungeonLevel(exp).fmt()}\n")
                }
                append("§dAverage§8: §b${"%.1f".format(classLevels.average())}")
            }
            append(Component.literal("§6Cata ${"%.1f".format(cata)}").onHover(cataHover))
            append(" §8| §e$secrets Secrets §8(§b${"%.1f".format(averageSecrets)}§8)\n")
        }
    }

    private fun buildFloorHover(dungeonType: SkyblockResponse.DungeonTypeData, title: String, floorPrefix: String) =
        Component.literal(title).apply {
            (1..7).forEach { floor ->
                val time = dungeonType.fastestSPlus["$floor"]?.toLong()?.toMMSS() ?: "§7None"
                val comps = dungeonType.tierComps["$floor"] ?: "0"
                append("\n$floorPrefix: $time §8(§b$comps§8)")
            }
        }

    private fun buildFloorTimes(normal: SkyblockResponse.DungeonTypeData, master: SkyblockResponse.DungeonTypeData) =
        Component.literal("§dFloor times§8: ")
            .append(Component.literal("§bNormal").onHover(buildFloorHover(normal, "§bNormal Floors", "§3F")))
            .append(" §8| ")
            .append(Component.literal("§cMaster").onHover(buildFloorHover(master, "§cMaster Floors", "§cM")))
            .append("\n")

    private fun getArmor(person: SkyblockResponse.SkyblockMember) =
        person.inventory.invArmor.itemStacks.take(4).reversed().filterNotNull()

    private fun buildArmor(person: SkyblockResponse.SkyblockMember) = Component.literal("\n").apply {
        for (piece in getArmor(person)) {
            val hover = Component.literal(piece.name + "\n" + piece.lore.joinToString("\n"))
            append(Component.literal(piece.name).onHover(hover)).append("\n")
        }
    }

    private fun buildItemLines(member: SkyblockResponse.SkyblockMember) = Component.literal("\n§dItems§8: ").apply {
        val pet = { id: String -> member.petsData.pets.any { it.type.contains(id) }}

        append(item(member, "§bHype", "HYPERION", "ASTRAEA", "SCYLLA", "VALKYRIE")).append(" §8| ")
        append(item(member, "§cTerm", "TERMINATOR")).append(" §8| ")
        append("§6GDrag${pet("GOLDEN_DRAGON").check()} §8| ")
        append("§5EDrag${pet("ENDER_DRAGON").check()}\n")
    }

    private fun item(member: SkyblockResponse.SkyblockMember, label: String, vararg ids: String): Component {
        val matches = member.allItems.filter { i -> ids.any { i?.id?.contains(it) == true }}.filterNotNull()
        val hover = Component.empty().apply {
            matches.forEachIndexed { i, it ->
                append("${it.name}\n${it.lore.joinToString("\n")}")
                if (i < matches.size - 1) append("\n\n")
            }
        }
        return Component.literal("$label${matches.isNotEmpty().check()}").onHover(hover)
    }

    private fun Boolean.check() = if (this) " §a✓" else " §c✗"
    private fun Double.fmt(p: Int = 1) = "%.${p}f".format(this)
    private fun Long.toMMSS(): String = if (this <= 0) "§7None" else
        "§a%d:%02d.%03d".format(this / 60000, (this % 60000) / 1000, this % 1000)
}