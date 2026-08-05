package co.stellarskys.stella.features.msc.profileUtils.screen.pages

import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.horizon.animation.AnimType
import co.stellarskys.stella.api.hypixel.SkyblockResponse
import co.stellarskys.stella.api.zenith.client
import co.stellarskys.stella.features.msc.profileUtils.HotmUtils
import co.stellarskys.stella.features.msc.profileUtils.NodeType
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

    private var hoveredItem: ItemStack? = null
    private val perkGrid = Array<ItemStack>(90) { ItemStack.EMPTY }
    private var activePresetIndex = member.skillTree.getSelectedMiningPresetIndex()

    init {
        leftScrollOffset = 0f
        targetRightOffset = -(265f - 183f)
        rightScrollOffset = targetRightOffset
        rebuildPerkGrid()
    }

    private fun rebuildPerkGrid() {
        for (i in 0 until 90) perkGrid[i] = ItemStack.EMPTY
        val currentNodes = member.skillTree.getMiningPresetNodes(activePresetIndex)
        HotmUtils.nodes.forEach { (slot, node) ->
            if (slot in 0..89) {
                val levelVal = currentNodes[node.apiKey]
                val level = (levelVal as? Number)?.toInt() ?: 0
                val isEnabledVal = currentNodes["toggle_${node.apiKey}"]
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

        fun drawStatLine(label: String, value: String, isMaxed: Boolean = false, tooltip: (() -> List<Component>)? = null) {
            val valColor = if (isMaxed) "§6" else "§e"
            ren2d.drawString(context, "§7$label: $valColor$value", 5, cy)

            if (inScissor && tooltip != null) {
                val absoluteY = 25 + cy + leftScrollOffset
                if (isAreaHovered(5f, absoluteY, 110f, 9f, mouseX, mouseY)) {
                    context.setTooltipForNextFrame(
                        client.font,
                        tooltip().map { it.visualOrderText },
                        mouseX.toInt(),
                        mouseY.toInt()
                    )
                }
            }
            cy += 11
        }


        ren2d.drawString(context, "§d§nHeart of the Mountain", 5, cy)
        cy += 13

        val curName = member.skillTree.getMiningPresetName(activePresetIndex)
        val selectedSlot = member.skillTree.getSelectedMiningPresetIndex()
        val isSelectedInGame = activePresetIndex == selectedSlot
        val activeBadge = if (isSelectedInGame) " §a[Active]" else ""

        ren2d.drawString(context, "§7Preset: §e$curName$activeBadge", 5, cy)
        cy += 11

        val btnW = 22
        val btnH = 13
        val presetBtnY = cy
        for (pIdx in 0 until 5) {
            val bx = 5 + pIdx * 24
            val isCurrent = pIdx == activePresetIndex
            val isGameActive = pIdx == selectedSlot
            val borderColor = if (isGameActive) Palette.Green else if (isCurrent) Palette.Purple else Palette.Surface0
            ren2d.drawHollowRect(context, bx, presetBtnY, btnW, btnH, 1, borderColor)

            val numStr = if (isCurrent) "§f${pIdx + 1}" else if (isGameActive) "§a${pIdx + 1}" else "§7${pIdx + 1}"
            val textWidth = client.font.width(numStr)
            val textX = bx + (btnW - textWidth) / 2
            val textY = presetBtnY + (btnH - 8) / 2
            ren2d.drawString(context, numStr, textX, textY)

            if (inScissor) {
                val absoluteY = 25 + presetBtnY + leftScrollOffset
                if (isAreaHovered(bx.toFloat() + 10f, absoluteY, btnW.toFloat(), btnH.toFloat(), mouseX, mouseY)) {
                    val pName = member.skillTree.getMiningPresetName(pIdx)
                    context.setTooltipForNextFrame(
                        client.font,
                        listOf(Component.literal("§d$pName").visualOrderText),
                        mouseX.toInt(),
                        mouseY.toInt()
                    )
                }
            }
        }
        cy += 18

        drawStatLine("Tier", "$hotmLevel", hotmLevel >= 10)

        val potmLevel = (member.skillTree.nodes["mining"]?.get("core_of_the_mountain") as? Number)?.toInt() ?: 0
        val totalTokens = HotmUtils.calcHotmTokens(hotmLevel, potmLevel)
        val tokensSpent = member.skillTree.tokensSpent["mountain"] ?: 0
        val tokens = (totalTokens - tokensSpent).coerceAtLeast(0)

        drawStatLine("Tokens", "$tokensSpent / $totalTokens", totalTokens >= 25) {
            listOf(
                Component.literal("§dTokens of the Mountain"),
                Component.literal("§7Spent: §a$tokensSpent"),
                Component.literal("§7Unspent: §a$tokens"),
                Component.literal("§7Total: §a$totalTokens")
            )
        }

        drawStatLine("Core", "$potmLevel / 10", potmLevel >= 10) {
            listOf(
                Component.literal("§6Core of the Mountain"),
                Component.literal("§7Level: §e$potmLevel / 10")
            )
        }

        val activeAbility = member.skillTree.selectedAbility["mining"]
        val abilityName = HotmUtils.getActiveAbilityName(activeAbility)
        drawStatLine("Ability", abilityName)

        cy += 4


        ren2d.drawString(context, "§d§nPowders", 5, cy)
        cy += 13

        val mithrilTotal = member.miningCore.powderSpentMithril + member.miningCore.powderMithril
        drawStatLine(
            "Mithril",
            "%,d".format(member.miningCore.powderMithril.toLong()),
            mithrilTotal >= 12500000
        ) {
            listOf(
                Component.literal("§2Mithril Powder"),
                Component.literal("§7Spent: §a" + "%,d".format(member.miningCore.powderSpentMithril.toLong())),
                Component.literal("§7Current: §a" + "%,d".format(member.miningCore.powderMithril.toLong())),
                Component.literal("§7Total: §a" + "%,d".format(mithrilTotal.toLong()))
            )
        }

        val gemstoneTotal = member.miningCore.powderSpentGemstone + member.miningCore.powderGemstone
        drawStatLine(
            "Gemstone",
            "%,d".format(member.miningCore.powderGemstone.toLong()),
            gemstoneTotal >= 20000000
        ) {
            listOf(
                Component.literal("§dGemstone Powder"),
                Component.literal("§7Spent: §a" + "%,d".format(member.miningCore.powderSpentGemstone.toLong())),
                Component.literal("§7Current: §a" + "%,d".format(member.miningCore.powderGemstone.toLong())),
                Component.literal("§7Total: §a" + "%,d".format(gemstoneTotal.toLong()))
            )
        }

        val glaciteTotal = member.miningCore.powderSpentGlacite + member.miningCore.powderGlacite
        drawStatLine(
            "Glacite",
            "%,d".format(member.miningCore.powderGlacite.toLong()),
            glaciteTotal >= 20000000
        ) {
            listOf(
                Component.literal("§bGlacite Powder"),
                Component.literal("§7Spent: §a" + "%,d".format(member.miningCore.powderSpentGlacite.toLong())),
                Component.literal("§7Current: §a" + "%,d".format(member.miningCore.powderGlacite.toLong())),
                Component.literal("§7Total: §a" + "%,d".format(glaciteTotal.toLong()))
            )
        }

        cy += 4


        ren2d.drawString(context, "§d§nGlacite Tunnels", 5, cy)
        cy += 13

        val mineshafts = member.glacite.mineshaftsEntered
        drawStatLine("Mineshafts", "%,d".format(mineshafts.toLong()))

        val fossilsCount = HotmUtils.getFossilsCount(member.glacite.fossilsDonated)
        drawStatLine("Fossils", "$fossilsCount / 8", fossilsCount >= 8) {
            mutableListOf<Component>().apply {
                add(Component.literal("§9Donated Fossils"))
                HotmUtils.fossilsList.forEach { (shortId, name) ->
                    val donated = member.glacite.fossilsDonated.contains(shortId)
                    val statusStr = if (donated) "§aDonated" else "§cNot Donated"
                    add(Component.literal("§7- §e$name§7: $statusStr"))
                }
            }
        }

        val corpses = member.glacite.corpsesLooted
        val corpsesTotal = corpses.values.sum()
        drawStatLine("Corpses", "$corpsesTotal", false) {
            listOf(
                Component.literal("§bCorpses Looted"),
                Component.literal("§9Lapis: §f${corpses["lapis"] ?: 0}"),
                Component.literal("§7Tungsten: §f${corpses["tungsten"] ?: 0}"),
                Component.literal("§6Umber: §f${corpses["umber"] ?: 0}"),
                Component.literal("§bVanguard: §f${corpses["vanguard"] ?: 0}"),
                Component.literal("§7Total: §a$corpsesTotal")
            )
        }

        val commissionMilestone = HotmUtils.getCommissionMilestone(member.objectives.tutorial)
        drawStatLine("Commissions Milestone", "$commissionMilestone", commissionMilestone >= 6) {
            listOf(
                Component.literal("§dCommissions"),
                Component.literal("§7Milestone: §e$commissionMilestone / 6")
            )
        }

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
        val hollowColor = when {
            HotmUtils.nucleusRunCrystals.all { member.miningCore.crystals[it.first]?.state == "PLACED" } -> "§a"
            HotmUtils.nucleusRunCrystals.any {
                val state = member.miningCore.crystals[it.first]?.state
                state == "FOUND" || state == "PLACED"
            } -> "§e"

            else -> "§7"
        }
        drawStatLine("Crystal Hollows", "$hollowColor$nucRuns Runs", false) {
            mutableListOf<Component>().apply {
                add(Component.literal("§dCrystal Hollows"))
                HotmUtils.nucleusRunCrystals.forEach { (apiKey, name) ->
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
        }

        val otherCrystalsCount = HotmUtils.getOtherCrystalsCount(member.miningCore.crystals)
        drawStatLine("Glacite Tunnels", "$otherCrystalsCount / 7", otherCrystalsCount >= 7) {
            mutableListOf<Component>().apply {
                add(Component.literal("§bGlacite Tunnels"))
                HotmUtils.otherCrystals.forEach { (apiKey, name) ->
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
        }
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
                    val currentNodes = member.skillTree.getMiningPresetNodes(activePresetIndex)
                    val levelVal = currentNodes[node.apiKey]
                    val level = (levelVal as? Number)?.toInt() ?: 0
                    val isEnabledVal = currentNodes["toggle_${node.apiKey}"]
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

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (button == 0 && isAreaHovered(10f, 25f, 135f, 185f, mouseX, mouseY)) {
            val presetBtnY = 25 + 6 + 13 + 11 + leftScrollOffset
            val btnW = 22
            val btnH = 13
            for (pIdx in 0 until 5) {
                val bx = 11 + 5 + pIdx * 24
                if (isAreaHovered(bx.toFloat(), presetBtnY, btnW.toFloat(), btnH.toFloat(), mouseX, mouseY)) {
                    if (activePresetIndex != pIdx) {
                        activePresetIndex = pIdx
                        rebuildPerkGrid()
                    }
                    return true
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseScrolled(mouseX: Float, mouseY: Float, amount: Float, horizontalAmount: Float): Boolean {
        if (isAreaHovered(10f, 25f, 135f, 185f, mouseX, mouseY)) {
            targetLeftOffset = ren2d.calculateScroll(targetLeftOffset, amount, 260, 185)
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