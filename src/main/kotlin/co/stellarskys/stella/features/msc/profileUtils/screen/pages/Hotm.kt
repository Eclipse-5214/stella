package co.stellarskys.stella.features.msc.profileUtils.screen.pages

import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.horizon.animation.AnimType
import co.stellarskys.stella.api.hypixel.SkyblockResponse
import co.stellarskys.stella.api.zenith.client
import co.stellarskys.stella.features.msc.profileUtils.HotmUtils
import co.stellarskys.stella.features.msc.profileUtils.SkillUtils
import co.stellarskys.stella.features.msc.profileUtils.NodeType
import co.stellarskys.stella.features.msc.profileUtils.NodeInfo
import co.stellarskys.stella.features.msc.profileUtils.screen.Page
import co.stellarskys.stella.utils.Utils
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class Hotm(
    name: String,
    val member: SkyblockResponse.SkyblockMember,
    navigate: (Page) -> Unit
) : Page("heart of the mountain", name, navigate) {

    override val icon: ItemStack = Items.DIAMOND_PICKAXE.defaultInstance

    private val hotmXp = member.skillTree.experience["mining"] ?: 0.0
    private val hotmSkill = HotmUtils.getHotmLevel(hotmXp)
    private val hotmLevel = hotmSkill.level.toInt()


    private var leftScrollOffset by Utils.animate<Float>(0.2, AnimType.EASE_OUT)
    private var targetLeftOffset = 0f
    private var rightScrollOffset by Utils.animate<Float>(0.2, AnimType.EASE_OUT)
    private var targetRightOffset = 0f

    init {
        leftScrollOffset = 0f
        rightScrollOffset = 0f
    }

    private var hoveredItem: ItemStack? = null


    private val perkGrid = Array<ItemStack>(90) { ItemStack.EMPTY }

    init {
        HotmUtils.nodes.forEach { (slot, node) ->
            if (slot in 0..89) {
                val levelVal = member.skillTree.nodes["mining"]?.get(node.apiKey)
                val level = (levelVal as? Number)?.toInt() ?: 0
                val isEnabledVal = member.skillTree.nodes["mining"]?.get("toggle_${node.apiKey}")
                val isEnabled = isEnabledVal as? Boolean ?: true

                val item =
                    HotmUtils.getNodeItem(node, level, isEnabled, hotmLevel, member.skillTree.selectedAbility["mining"])
                        .copy().apply {
                        if (level > 1 && node.type != NodeType.CORE && node.type != NodeType.ABILITY) {
                            count = level
                        }
                    }
                perkGrid[slot] = item
            }
        }
    }

    override fun onRender(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float, delta: Float) {

        ren2d.drawHollowRect(context, 10, 25, 135, 185, 1, Palette.Purple)
        val leftPanelHeight = 240
        ren2d.renderScrolled(context, 11, 26, 133, 183, leftScrollOffset) {
            drawLeftPanel(context, mouseX, mouseY)
        }
        ren2d.drawScrollbar(context, 146, 25, 185, leftScrollOffset, leftPanelHeight, Palette.Purple)


        ren2d.drawHollowRect(context, 150, 25, 190, 185, 1, Palette.Purple)
        val rightPanelHeight = 265
        ren2d.renderScrolled(context, 151, 26, 188, 183, rightScrollOffset) {
            drawRightPanel(context, mouseX, mouseY)
        }
        ren2d.drawScrollbar(context, 341, 25, 185, rightScrollOffset, rightPanelHeight, Palette.Purple)


        hoveredItem?.let {
            if (!it.isEmpty) context.setTooltipForNextFrame(
                client.font,
                it,
                mouseX.toInt(),
                mouseY.toInt()
            )
        }
        hoveredItem = null
    }

    private fun drawLeftPanel(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float) {
        val inScissor = isAreaHovered(10f, 25f, 135f, 185f, mouseX, mouseY)
        var cy = 6

        fun drawStatLine(label: String, value: String, isMaxed: Boolean = false, tooltip: List<Component>? = null) {
            val valColor = if (isMaxed) "§6" else "§e"
            ren2d.drawString(context, "§7$label: $valColor$value", 5, cy)

            if (inScissor && tooltip != null) {
                val absoluteY = 25 + cy + leftScrollOffset
                if (isAreaHovered(15f, absoluteY, 110f, 9f, mouseX, mouseY)) {
                    context.setTooltipForNextFrame(
                        client.font,
                        tooltip.map { it.visualOrderText },
                        mouseX.toInt(),
                        mouseY.toInt()
                    )
                }
            }
            cy += 11
        }


        ren2d.drawString(context, "§d§nHeart of the Mountain", 5, cy)
        cy += 13

        drawStatLine("Tier", "$hotmLevel", hotmLevel >= 10)

        val potmLevel = (member.skillTree.nodes["mining"]?.get("core_of_the_mountain") as? Number)?.toInt() ?: 0
        val totalTokens = HotmUtils.calcHotmTokens(hotmLevel, potmLevel)
        val tokensSpent = member.skillTree.tokensSpent["mountain"] ?: 0
        val tokens = (totalTokens - tokensSpent).coerceAtLeast(0)

        val tokensTooltip = listOf(
            Component.literal("§dTokens of the Mountain"),
            Component.literal("§7Spent: §a$tokensSpent"),
            Component.literal("§7Unspent: §a$tokens"),
            Component.literal("§7Total: §a$totalTokens")
        )
        drawStatLine("Tokens", "$tokensSpent / $totalTokens", totalTokens >= 25, tokensTooltip)

        val potmTooltip = listOf(
            Component.literal("§6Core of the Mountain"),
            Component.literal("§7Level: §e$potmLevel / 10")
        )
        drawStatLine("Core", "$potmLevel / 10", potmLevel >= 10, potmTooltip)

        val activeAbility = member.skillTree.selectedAbility["mining"]
        val abilityName = HotmUtils.getActiveAbilityName(activeAbility)
        drawStatLine("Ability", abilityName)

        cy += 4


        ren2d.drawString(context, "§d§nPowders", 5, cy)
        cy += 13

        val mithrilTotal = member.miningCore.powderSpentMithril + member.miningCore.powderMithril
        val mithrilTooltip = listOf(
            Component.literal("§2Mithril Powder"),
            Component.literal("§7Spent: §a" + "%,d".format(member.miningCore.powderSpentMithril.toLong())),
            Component.literal("§7Current: §a" + "%,d".format(member.miningCore.powderMithril.toLong())),
            Component.literal("§7Total: §a" + "%,d".format(mithrilTotal.toLong()))
        )
        drawStatLine(
            "Mithril",
            "%,d".format(member.miningCore.powderMithril.toLong()),
            mithrilTotal >= 12500000,
            mithrilTooltip
        )

        val gemstoneTotal = member.miningCore.powderSpentGemstone + member.miningCore.powderGemstone
        val gemstoneTooltip = listOf(
            Component.literal("§dGemstone Powder"),
            Component.literal("§7Spent: §a" + "%,d".format(member.miningCore.powderSpentGemstone.toLong())),
            Component.literal("§7Current: §a" + "%,d".format(member.miningCore.powderGemstone.toLong())),
            Component.literal("§7Total: §a" + "%,d".format(gemstoneTotal.toLong()))
        )
        drawStatLine(
            "Gemstone",
            "%,d".format(member.miningCore.powderGemstone.toLong()),
            gemstoneTotal >= 20000000,
            gemstoneTooltip
        )

        val glaciteTotal = member.miningCore.powderSpentGlacite + member.miningCore.powderGlacite
        val glaciteTooltip = listOf(
            Component.literal("§bGlacite Powder"),
            Component.literal("§7Spent: §a" + "%,d".format(member.miningCore.powderSpentGlacite.toLong())),
            Component.literal("§7Current: §a" + "%,d".format(member.miningCore.powderGlacite.toLong())),
            Component.literal("§7Total: §a" + "%,d".format(glaciteTotal.toLong()))
        )
        drawStatLine(
            "Glacite",
            "%,d".format(member.miningCore.powderGlacite.toLong()),
            glaciteTotal >= 20000000,
            glaciteTooltip
        )

        cy += 4


        ren2d.drawString(context, "§d§nGlacite Tunnels", 5, cy)
        cy += 13

        val mineshafts = member.glacite.mineshaftsEntered
        drawStatLine("Mineshafts", "%,d".format(mineshafts.toLong()))

        val fossilsCount = HotmUtils.getFossilsCount(member.glacite.fossilsDonated)
        val fossilsTooltip = mutableListOf<Component>().apply {
            add(Component.literal("§9Donated Fossils"))
            HotmUtils.fossilsList.forEach { (id, name) ->
                val shortId = id.split("_").first().uppercase(java.util.Locale.ROOT)
                val donated = member.glacite.fossilsDonated.contains(shortId)
                val statusStr = if (donated) "§aDonated" else "§cNot Donated"
                add(Component.literal("§7- §e$name§7: $statusStr"))
            }
        }
        drawStatLine("Fossils", "$fossilsCount / 8", fossilsCount >= 8, fossilsTooltip)

        val corpses = member.glacite.corpsesLooted
        val corpsesTotal = corpses.values.sum()
        val corpsesTooltip = listOf(
            Component.literal("§bCorpses Looted"),
            Component.literal("§9Lapis: §f${corpses["lapis"] ?: 0}"),
            Component.literal("§7Tungsten: §f${corpses["tungsten"] ?: 0}"),
            Component.literal("§6Umber: §f${corpses["umber"] ?: 0}"),
            Component.literal("§bVanguard: §f${corpses["vanguard"] ?: 0}"),
            Component.literal("§7Total: §a$corpsesTotal")
        )
        drawStatLine("Corpses", "$corpsesTotal", false, corpsesTooltip)

        val commissionMilestone = HotmUtils.getCommissionMilestone(member.objectives.tutorial)
        val commissionsTooltip = listOf(
            Component.literal("§dCommissions"),
            Component.literal("§7Milestone: §e$commissionMilestone / 6")
        )
        drawStatLine("Commissions Milestone", "$commissionMilestone", commissionMilestone >= 6, commissionsTooltip)

        cy += 4


        ren2d.drawString(context, "§d§nCrystals", 5, cy)
        cy += 13

        val crystalColors = mapOf(
            "Jade" to "§a",
            "Amber" to "§6",
            "Amethyst" to "§5",
            "Sapphire" to "§b",
            "Topaz" to "§e",
            "Ruby" to "§c",
            "Jasper" to "§d",
            "Opal" to "§f",
            "Aquamarine" to "§b",
            "Citrine" to "§c",
            "Peridot" to "§a",
            "Onyx" to "§8"
        )

        val nucRuns = HotmUtils.getNucleusRuns(member.miningCore.crystals)
        val nucTooltip = mutableListOf<Component>().apply {
            add(Component.literal("§dCrystal Hollows"))
            HotmUtils.nucleusRunCrystals.forEachIndexed { index, (apiKey, name) ->
                val crystal = member.miningCore.crystals[apiKey]
                val state = crystal?.state ?: "NOT_FOUND"
                val (color, stateName) = when (state) {
                    "PLACED" -> "§a" to "Placed"
                    "FOUND" -> "§e" to "Found"
                    else -> "§7" to "Not Found"
                }
                val nameColor = crystalColors[name] ?: "§7"
                add(Component.literal("$nameColor$name§7: $color$stateName"))
            }
        }
        val hollowColor = when {
            HotmUtils.nucleusRunCrystals.all { member.miningCore.crystals[it.first]?.state == "PLACED" } -> "§a"
            HotmUtils.nucleusRunCrystals.any {
                member.miningCore.crystals[it.first]?.state in listOf(
                    "FOUND",
                    "PLACED"
                )
            } -> "§e"

            else -> "§7"
        }
        drawStatLine("Crystal Hollows", "$hollowColor$nucRuns Runs", false, nucTooltip)

        val otherCrystalsCount = HotmUtils.getOtherCrystalsCount(member.miningCore.crystals)
        val otherTooltip = mutableListOf<Component>().apply {
            add(Component.literal("§bGlacite Tunnels"))
            HotmUtils.otherCrystals.forEachIndexed { index, (apiKey, name) ->
                val crystal = member.miningCore.crystals[apiKey]
                val state = crystal?.state ?: "NOT_FOUND"
                val (color, stateName) = when (state) {
                    "PLACED" -> "§a" to "Found"
                    "FOUND" -> "§e" to "Found"
                    else -> "§7" to "Not Found"
                }
                val nameColor = crystalColors[name] ?: "§7"
                add(Component.literal("$nameColor$name§7: $color$stateName"))
            }
        }
        drawStatLine("Glacite Tunnels", "$otherCrystalsCount / 7", otherCrystalsCount >= 7, otherTooltip)
    }

    private fun drawRightPanel(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float) {
        val inScissor = isAreaHovered(150f, 25f, 190f, 185f, mouseX, mouseY)
        val cellSize = 24
        val cellStep = 25

        val maxRow = 9
        for (idx in 0 until 90) {
            val col = (idx % 9) - 1
            val row = idx / 9

            if (col !in 0..6) continue

            val rx = col * cellStep + 7
            val ry = (maxRow - row) * cellStep + 5

            val screenX = 150 + rx
            val screenY = 25 + ry

            val stack = perkGrid[idx]
            val node = HotmUtils.nodes[idx]

            if (node != null && !stack.isEmpty) {

                ren2d.drawRect(context, rx, ry, cellSize, cellSize, Palette.Crust)
                ren2d.drawHollowRect(context, rx, ry, cellSize, cellSize, 1, Palette.Surface0)

                ren2d.renderItem(
                    context,
                    stack,
                    rx.toFloat() + ((cellSize - 16) / 2f),
                    ry.toFloat() + ((cellSize - 16) / 2f),
                    1f
                )

                if (inScissor && isAreaHovered(
                        screenX.toFloat(),
                        screenY.toFloat() + rightScrollOffset,
                        cellSize.toFloat(),
                        cellSize.toFloat(),
                        mouseX,
                        mouseY
                    )
                ) {
                    val levelVal = member.skillTree.nodes["mining"]?.get(node.apiKey)
                    val level = (levelVal as? Number)?.toInt() ?: 0
                    val isEnabledVal = member.skillTree.nodes["mining"]?.get("toggle_${node.apiKey}")
                    val isEnabled = isEnabledVal as? Boolean ?: true

                    hoveredItem = stack
                    val tooltipComps = HotmUtils.getFormattedTooltip(
                        node,
                        level,
                        isEnabled,
                        hotmLevel,
                        member.skillTree.selectedAbility["mining"]
                    )
                    context.setTooltipForNextFrame(
                        client.font,
                        tooltipComps.map { it.visualOrderText },
                        mouseX.toInt(),
                        mouseY.toInt()
                    )
                }
            }
        }
    }

    override fun mouseScrolled(mouseX: Float, mouseY: Float, amount: Float, horizontalAmount: Float): Boolean {
        if (isAreaHovered(10f, 25f, 135f, 185f, mouseX, mouseY)) {
            targetLeftOffset = ren2d.calculateScroll(targetLeftOffset, amount, 240, 185)
            leftScrollOffset = targetLeftOffset
            return true
        }
        if (isAreaHovered(150f, 25f, 190f, 185f, mouseX, mouseY)) {
            targetRightOffset = ren2d.calculateScroll(targetRightOffset, amount, 265, 185)
            rightScrollOffset = targetRightOffset
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, amount, horizontalAmount)
    }
}