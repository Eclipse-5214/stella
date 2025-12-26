package co.stellarskys.stella.features.msc

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.GuiEvent
import co.stellarskys.stella.events.core.PacketEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.hud.HUDManager
import co.stellarskys.stella.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import tech.thatgravyboat.skyblockapi.utils.extentions.getLore
import tech.thatgravyboat.skyblockapi.utils.extentions.getSkyBlockId
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped

@Module
object soulflowDisplay: Feature("soulflowDisplay", true) {
    private const val INTERNALIZED_PREFIX = "Internalized: "
    private val SOULFLOW_IDS = setOf("SOULFLOW_PILE", "SOULFLOW_BATTERY", "SOULFLOW_SUPERCELL")

    private val displayName = "soulflowDisplay"
    private var soulflow = ""

    override fun initialize() {
        HUDManager.register(displayName, "§3500⸎ Soulflow", "soulflowDisplay")

        on<PacketEvent.Received> { event ->
            if (event.packet !is ClientboundContainerSetSlotPacket) return@on
            if (event.packet.item.getSkyBlockId() !in SOULFLOW_IDS) return@on
            soulflow = event.packet.item.getLore().firstOrNull { it.stripped.startsWith(INTERNALIZED_PREFIX) }?.stripped?.removePrefix(INTERNALIZED_PREFIX)?.takeIf { it.isNotBlank() } ?: ""
        }

        on<GuiEvent.RenderHUD> { render(it.context) }
    }

    private fun render(context: GuiGraphics) = HUDManager.renderHud(displayName, context) { Render2D.drawString(context, "§3$soulflow", 0, 0) }
}