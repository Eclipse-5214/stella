package co.stellarskys.stella.utils.skyblock.dungeons.utils

import co.stellarskys.stella.Stella
import co.stellarskys.stella.features.stellanav.map
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import net.minecraft.resources.ResourceLocation
import java.awt.Color

enum class DungeonClass(
    val displayName: String,
    val colorGetter: () -> Color?
) {
    UNKNOWN("Unknown", { Color(0, 0, 0, 255) }),
    HEALER("Healer", { Dungeon.healerColor }),
    MAGE("Mage", { Dungeon.mageColor }),
    BERSERK("Berserk", { Dungeon.berzColor }),
    ARCHER("Archer", { Dungeon.archerColor }),
    TANK("Tank", { Dungeon.tankColor }),
    DEAD("DEAD", { null });

    val color: Color? get() = colorGetter()

    companion object {
        private val classMap = entries.associateBy { it.displayName }
        fun from(name: String?): DungeonClass = classMap[name] ?: UNKNOWN
    }
}

enum class Checkmark(
    val texture: ResourceLocation?,
    val colorCode: String
) {
    NONE(null, "§7"),
    WHITE(ResourceLocation.fromNamespaceAndPath(Stella.NAMESPACE, "stellanav/clear/bloommapwhitecheck"), "§f"),
    GREEN(ResourceLocation.fromNamespaceAndPath(Stella.NAMESPACE, "stellanav/clear/bloommapgreencheck"), "§a"),
    FAILED(ResourceLocation.fromNamespaceAndPath(Stella.NAMESPACE, "stellanav/clear/bloommapfailedroom"), "§c"),
    UNEXPLORED(ResourceLocation.fromNamespaceAndPath(Stella.NAMESPACE, "stellanav/clear/bloommapquestionmark"), "§7"),
    UNDISCOVERED(null, "§7");
}

enum class RoomType(
    val displayName: String,
    val colorCode: String,
    val colorGetter: () -> Color?
) {
    NORMAL("Normal", "7", { map.NormalColor }),
    PUZZLE("Puzzle", "d", { map.PuzzleColor }),
    TRAP("Trap", "6", { map.TrapColor }),
    YELLOW("Yellow", "e", { map.MinibossColor }),
    BLOOD("Blood", "c", { map.BloodColor }),
    FAIRY("Fairy", "d", { map.FairyColor }),
    RARE("Rare", "b", { null }),
    ENTRANCE("Entrance", "a", { map.EntranceColor }),
    UNKNOWN("Unknown", "f", { null });

    val color: Color? get() = colorGetter()

    companion object {
        fun fromByte(byte: Int): RoomType = when (byte) {
            63 -> NORMAL
            30 -> ENTRANCE
            74 -> YELLOW
            18 -> BLOOD
            66 -> PUZZLE
            62 -> TRAP
            else -> UNKNOWN
        }

        fun fromString(name: String): RoomType {
            return entries.find { it.displayName.equals(name, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

enum class DoorType(
    val displayName: String,
    val colorGetter: () -> Color
) {
    NORMAL("Normal", { map.NormalDoorColor }),
    WITHER("Wither", { map.WitherDoorColor }),
    BLOOD("Blood", { map.BloodDoorColor }),
    ENTRANCE("Entrance", { map.EntranceDoorColor });

    val color: Color get() = colorGetter()
}

enum class DoorState { UNDISCOVERED, DISCOVERED }
