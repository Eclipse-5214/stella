package co.stellarskys.stella.events

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.api.Event
import co.stellarskys.stella.events.api.EventBus
import co.stellarskys.stella.events.core.*
import co.stellarskys.stella.managers.events.EventBusManager
import co.stellarskys.stella.utils.skyblock.location.SkyBlockIsland
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import org.lwjgl.glfw.GLFW
import co.stellarskys.stella.events.core.GuiEvent
import dev.deftu.omnicore.api.eventBus
import dev.deftu.eventbus.on
import dev.deftu.omnicore.api.client.events.ClientTickEvent
import dev.deftu.omnicore.api.events.ServerTickEvent
import net.minecraft.network.protocol.Packet

@Module
object EventBus : EventBus(true) {
    init {
        register<WorldRenderEvent.Last> { event ->
            post(RenderEvent.World.Last(event.context))
        }

        register<WorldRenderEvent.AfterEntities> { event ->
            post(RenderEvent.World.AfterEntities(event.context))
        }

        register<WorldRenderEvent.BlockOutline> { event ->
            if (post(RenderEvent.World.BlockOutline(event.context))) event.cancel()
        }

        ClientReceiveMessageEvents.ALLOW_GAME.register { message, isActionBar ->
            !post(ChatEvent.Receive(message, isActionBar))
        }

        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            post(ServerEvent.Connect())
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            post(ServerEvent.Disconnect())
        }

        eventBus.on<ServerTickEvent.Pre> {
            post(TickEvent.Server())
        }

        eventBus.on<ClientTickEvent.Pre> {
            post(TickEvent.Client())
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
            //#if MC >= 1.21.9
            //$$ ScreenMouseEvents.allowMouseClick(screen).register { _, click ->
            //$$    !post(GuiEvent.Click(click.x, click.y, click.button(), true, screen))
            //$$ }
            //$$
            //$$ ScreenMouseEvents.allowMouseRelease(screen).register { _, click ->
            //$$    !post(GuiEvent.Click(click.x, click.y, click.button(), false, screen))
            //$$ }
            //$$
            //$$ ScreenKeyboardEvents.allowKeyPress(screen).register { _, keyInput ->
            //$$    val charTyped = GLFW.glfwGetKeyName(keyInput.key, keyInput.scancode)?.firstOrNull() ?: '\u0000'
            //$$    !post(GuiEvent.Key(GLFW.glfwGetKeyName(keyInput.key, keyInput.scancode), keyInput.key, charTyped, keyInput.key, screen))
            //$$ }
            //#else
            ScreenMouseEvents.allowMouseClick(screen).register { _, mx, my, mouseButton ->
                !post(GuiEvent.Click(mx, my, mouseButton, true, screen))
            }

            ScreenMouseEvents.allowMouseRelease(screen).register { _, mx, my, mouseButton ->
                !post(GuiEvent.Click(mx, my, mouseButton, false, screen))
            }

            ScreenKeyboardEvents.allowKeyPress(screen).register { _, key, scancode, modifiers ->
                val charTyped = GLFW.glfwGetKeyName(key, scancode)?.firstOrNull() ?: '\u0000'
                !post(GuiEvent.Key(GLFW.glfwGetKeyName(key, scancode), key, charTyped, scancode, screen))
            }
            //#endif
        }

        ScreenEvents.BEFORE_INIT.register { _, screen, _, _ ->
            if (screen != null) post(GuiEvent.Open(screen))
        }

    }

    fun onPacketReceived(packet: Packet<*>): Boolean {
        return post(PacketEvent.Received(packet))
    }

    inline fun <reified T : Event> registerIn(
        vararg islands: SkyBlockIsland,
        skyblockOnly: Boolean = false,
        noinline callback: (T) -> Unit
    ) {
        val eventCall = register<T>(add = false, callback = callback)
        val islandSet = if (islands.isNotEmpty()) islands.toSet() else null
        EventBusManager.trackConditionalEvent(islandSet, skyblockOnly, eventCall)
    }
}