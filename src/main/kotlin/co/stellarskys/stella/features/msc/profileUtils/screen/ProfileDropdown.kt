package co.stellarskys.stella.features.msc.profileUtils.screen

import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.config.ui.Palette.withAlpha
import co.stellarskys.stella.api.horizon.mc.BaseElement
import co.stellarskys.stella.api.hypixel.SkyblockResponse
import co.stellarskys.stella.api.zenith.client
import co.stellarskys.stella.features.msc.profileUtils.PvScreen
import net.minecraft.client.gui.GuiGraphicsExtractor
import tech.thatgravyboat.skyblockapi.platform.pushPop

class ProfileDropdown(
    val playerName: String,
    val currentMember: SkyblockResponse.SkyblockMember
): BaseElement() {
    private var isOpen = false
    private val buttonWidth = 100f
    private val buttonHeight = 15f
    
    init {
        this.width = buttonWidth
        this.height = buttonHeight
        this.x = 0f
        this.y = 222f
    }

    override fun render(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float, delta: Float) {
        val profiles = currentMember.allProfiles ?: return
        if (profiles.isEmpty()) return

        context.pushPop {
            context.pose().translate(x, y)
            
            val hovered = isAreaHovered(0f, 0f, buttonWidth, buttonHeight, mouseX, mouseY)
            ren2d.drawRect(context, 0, 0, buttonWidth.toInt(), buttonHeight.toInt(), Palette.Crust.withAlpha(if (hovered) 200 else 150))
            ren2d.drawHollowRect(context, 0, 0, buttonWidth.toInt(), buttonHeight.toInt(), 1, Palette.Purple)
            
            val currentName = currentMember.profile?.cuteName ?: "Unknown"
            ren2d.drawString(context, "§dProfile: §6$currentName", 5, 4)

            if (isOpen) {
                var currentY = buttonHeight
                profiles.forEach { profile ->
                    val isCurrent = profile == currentMember.profile
                    val itemHovered = isAreaHovered(0f, currentY, buttonWidth, buttonHeight, mouseX, mouseY)
                    
                    val bgColor = if (isCurrent) Palette.Green.withAlpha(100)
                                  else if (itemHovered) Palette.Surface1.withAlpha(200)
                                  else Palette.Crust.withAlpha(200)
                    
                    ren2d.drawRect(context, 0, currentY.toInt(), buttonWidth.toInt(), buttonHeight.toInt(), bgColor)
                    ren2d.drawHollowRect(context, 0, currentY.toInt(), buttonWidth.toInt(), buttonHeight.toInt(), 1, Palette.Purple)
                    ren2d.drawString(context, "§6${profile.cuteName ?: "Unknown"}", 5, currentY.toInt() + 4)
                    
                    currentY += buttonHeight
                }
            }
        }
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        val profiles = currentMember.allProfiles ?: return false
        
        if (isAreaHovered(0f, 0f, buttonWidth, buttonHeight, mouseX, mouseY)) {
            isOpen = !isOpen
            return true
        }
        
        if (isOpen) {
            var currentY = buttonHeight
            profiles.forEach { profile ->
                if (isAreaHovered(0f, currentY, buttonWidth, buttonHeight, mouseX, mouseY)) {
                    if (profile != currentMember.profile) {
                        val uuid = currentMember.uuid
                        if (uuid != null) {
                            val newMember = profile.members[uuid.toString().replace("-", "")]
                            if (newMember != null) {
                                newMember.profile = profile
                                newMember.allProfiles = profiles
                                newMember.uuid = uuid
                                PvScreen.open(playerName, newMember)
                            }
                        }
                    }
                    isOpen = false
                    return true
                }
                currentY += buttonHeight
            }
            isOpen = false
        }
        return false
    }
}
