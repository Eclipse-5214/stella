package co.stellarskys.stella.events

import co.stellarskys.stella.Stella
import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.api.Event
import co.stellarskys.stella.events.api.EventBus
import co.stellarskys.stella.events.api.EventHandle
import co.stellarskys.stella.events.core.*
import co.stellarskys.stella.managers.events.EventBusManager
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.resources.ResourceLocation
import org.lwjgl.glfw.GLFW
import co.stellarskys.stella.events.core.GuiEvent
import co.stellarskys.stella.utils.render.RenderContext
import dev.deftu.omnicore.api.client.*
import net.minecraft.network.protocol.Packet
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonFloor
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockArea

@Module
object EventBus : EventBus() {
    private val STELLA_HUDS = ResourceLocation.fromNamespaceAndPath(Stella.NAMESPACE, "stella_hud")

    init {
        ClientReceiveMessageEvents.ALLOW_GAME.register { message, isActionBar ->
            !post(ChatEvent.Receive(message, isActionBar))
        }

        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            post(ServerEvent.Connect())
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            post(ServerEvent.Disconnect())
        }

        ClientLifecycleEvents.CLIENT_STARTED.register { _ ->
            post(GameEvent.Start())
        }

        ClientLifecycleEvents.CLIENT_STOPPING.register { _ ->
            post(GameEvent.Stop())
        }

        ClientEntityEvents.ENTITY_UNLOAD.register { entity, _ ->
            post(EntityEvent.Death(entity))
        }

        ScreenEvents.BEFORE_INIT.register { _, screen, _, _ ->
            ScreenMouseEvents.allowMouseClick(screen).register { _, click ->
               !post(GuiEvent.Click(click.x, click.y, click.button(), true, screen))
            }

            ScreenMouseEvents.allowMouseRelease(screen).register { _, click ->
               !post(GuiEvent.Click(click.x, click.y, click.button(), false, screen))
            }

            ScreenKeyboardEvents.allowKeyPress(screen).register { _, keyInput ->
               val charTyped = GLFW.glfwGetKeyName(keyInput.key, keyInput.scancode)?.firstOrNull() ?: '\u0000'
               !post(GuiEvent.Key(GLFW.glfwGetKeyName(keyInput.key, keyInput.scancode), keyInput.key, charTyped, keyInput.key, screen))
            }
        }

        ScreenEvents.BEFORE_INIT.register { _, screen, _, _ ->
            if (screen != null) post(GuiEvent.Open(screen))
        }

        WorldRenderEvents.AFTER_ENTITIES.register { context ->
            post(RenderEvent.World.AfterEntities(RenderContext.fromContext(context)))
        }

        WorldRenderEvents.END_MAIN.register { context ->
           post(RenderEvent.World.Last(RenderContext.fromContext(context)))
        }

        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register { context, outlineRenderState ->
           val ctx = RenderContext.fromContext(context).apply {
               blockPos = outlineRenderState.pos
               voxelShape = outlineRenderState.shape
           }
           !post(RenderEvent.World.BlockOutline(ctx))
        }

        HudElementRegistry.attachElementBefore(VanillaHudElements.SLEEP, STELLA_HUDS) { context, _ ->
            if (client.options.hideGui || world == null || player == null) return@attachElementBefore
            post(GuiEvent.RenderHUD(context))
            Stella.DELTA.updateDelta()
        }
    }

    fun onPacketReceived(packet: Packet<*>): Boolean {
        return post(PacketEvent.Received(packet))
    }

    inline fun <reified T : Event> on(
        vararg scope: Any,
        skyblockOnly: Boolean = false,
        priority: Int = 0,
        noinline handler: (T) -> Unit
    ): EventHandle<T> { // partition scopes into sets
        val islands = mutableSetOf<SkyBlockIsland>()
        val arias = mutableSetOf<SkyBlockArea>()
        val floors = mutableSetOf<DungeonFloor>()

        scope.forEach {
            when (it) {
                is SkyBlockIsland -> islands += it
                is SkyBlockArea -> arias += it
                is DungeonFloor -> floors += it
                else -> throw IllegalArgumentException(
                    "Unsupported scope type: ${it::class.simpleName}. "
                            + "Must be SkyBlockIsland, SkyBlockArea, or DungeonFloor."
                )
            }
        }

        val handle = on<T>(priority, handler = handler, register = false)

            EventBusManager.trackConditionalEvent(
                islands = islands.ifEmpty { null },
                arias = arias.ifEmpty { null },
                floors = floors.ifEmpty { null },
                skyblockOnly = skyblockOnly,
                handle = handle
            )

        return handle
    }
}