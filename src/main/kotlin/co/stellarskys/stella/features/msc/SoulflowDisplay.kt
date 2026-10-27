package co.stellarskys.stella.features.msc

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.GuiEvent
import co.stellarskys.stella.events.core.PacketEvent
import co.stellarskys.stella.events.core.TablistEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.hud.HUDManager
import co.stellarskys.stella.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import tech.thatgravyboat.skyblockapi.utils.extentions.getLore
import tech.thatgravyboat.skyblockapi.utils.extentions.getSkyBlockId
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped

@Module
object SoulflowDisplay: Feature("soulflowDisplay", true) {
    private const val INTERNALIZED_PREFIX = "Internalized: "
    private val SOULFLOW_IDS = setOf("SOULFLOW_PILE", "SOULFLOW_BATTERY", "SOULFLOW_SUPERCELL")
    private val SOULFLOW_PATTERN = """Soulflow:\s*\D*([\d,]+)""".toRegex()

    private const val displayName = "soulflowDisplay"
    private var soulflow = -1

    override fun initialize() {
        HUDManager.register(displayName, "§3500⸎ Soulflow", "soulflowDisplay")

        on<TablistEvent.Change> { event ->
            event.new.flatten().firstNotNullOfOrNull { SOULFLOW_PATTERN.find(it.stripped)?.groupValues?.get(1) }?.replace("\\D".toRegex(), "")?.toInt()?.let { soulflow = it }
        }

        on<PacketEvent.Received> { event ->
            if (event.packet !is ClientboundContainerSetSlotPacket) return@on
            if (event.packet.item.getSkyBlockId() !in SOULFLOW_IDS) return@on
            event.packet.item.getLore().firstOrNull { it.stripped.startsWith(INTERNALIZED_PREFIX) }?.stripped?.replace("\\D".toRegex(), "")?.toInt()?.let { soulflow = it }
        }

        on<GuiEvent.RenderHUD> { if(soulflow != -1) render(it.context) }
    }

    private fun render(context: GuiGraphicsExtractor) = HUDManager.renderHud(displayName, context) { Render2D.drawString(context, "§3${String.format("%,d", soulflow)}⸎ Soulflow", 0, 0) }
}