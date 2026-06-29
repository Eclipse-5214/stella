package co.stellarskys.stella.api.dungeons.utils

import co.stellarskys.stella.features.stellanav.Map
import co.stellarskys.stella.api.dungeons.Dungeon
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.awt.Color

enum class DungeonClass(
    val displayName: String,
    val colorGetter: () -> Color?,
    val iconProvider: () -> ItemStack = { ItemStack.EMPTY }
) {
    UNKNOWN("Unknown", { Color(0, 0, 0, 255) }),
    HEALER("Healer", { Dungeon.healerColor }, { Items.SPLASH_POTION.defaultInstance }),
    MAGE("Mage", { Dungeon.mageColor }, { Items.BLAZE_ROD.defaultInstance }),
    BERSERK("Berserk", { Dungeon.berzColor }, { Items.DIAMOND_SWORD.defaultInstance }),
    ARCHER("Archer", { Dungeon.archerColor }, { Items.BOW.defaultInstance }),
    TANK("Tank", { Dungeon.tankColor }, { Items.DIAMOND_CHESTPLATE.defaultInstance }),
    DEAD("DEAD", { null });

    val color: Color? get() = colorGetter()

    companion object {
        val valid = listOf(HEALER, MAGE, BERSERK, ARCHER, TANK)
        private val classMap = entries.associateBy { it.displayName }
        fun from(name: String?): DungeonClass = classMap[name] ?: UNKNOWN
    }
}

enum class Checkmark(
    private val texturePath: String?, // Store the name/path as a String
    val colorCode: String
) {
    NONE(null, "§7"),
    WHITE("clear/bloommapwhitecheck", "§f"),
    GREEN("clear/bloommapgreencheck", "§a"),
    FAILED("clear/bloommapfailedroom", "§c"),
    UNEXPLORED("clear/bloommapquestionmark", "§7"),
    UNDISCOVERED(null, "§7");

    val texture: Identifier? get() = texturePath?.let { Map.getOrLoad(it) }
}

enum class RoomType(
    val displayName: String,
    val colorCode: String,
    val colorGetter: () -> Color?
) {
    NORMAL("Normal", "7", { Map.NormalColor }),
    PUZZLE("Puzzle", "d", { Map.PuzzleColor }),
    TRAP("Trap", "6", { Map.TrapColor }),
    YELLOW("Champion", "e", { Map.MinibossColor }),
    BLOOD("Blood", "c", { Map.BloodColor }),
    FAIRY("Fairy", "d", { Map.FairyColor }),
    RARE("Rare", "b", { Map.NormalColor }),
    ENTRANCE("Entrance", "a", { Map.EntranceColor }),
    UNKNOWN("Unknown", "f", { Color(100, 100, 100) });

    val color: Color? get() = colorGetter()

    companion object {
        val unguessable = setOf(NORMAL, BLOOD, FAIRY, ENTRANCE)

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
    NORMAL("Normal", { Map.NormalDoorColor }),
    WITHER("Wither", { Map.WitherDoorColor }),
    BLOOD("Blood", { Map.BloodDoorColor }),
    ENTRANCE("Entrance", { Map.EntranceDoorColor });

    val color: Color get() = colorGetter()
}

enum class DoorState { UNDISCOVERED, DISCOVERED }
