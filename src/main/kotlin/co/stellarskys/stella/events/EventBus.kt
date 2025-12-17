package co.stellarskys.stella.events

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.api.Event
import co.stellarskys.stella.events.api.EventBus
import co.stellarskys.stella.events.api.EventHandle
import co.stellarskys.stella.events.core.*
import co.stellarskys.stella.managers.events.EventBusManager
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import org.lwjgl.glfw.GLFW
import co.stellarskys.stella.events.core.GuiEvent
import co.stellarskys.stella.utils.render.RenderContext
import net.minecraft.network.protocol.Packet
import net.minecraft.world.level.EmptyBlockGetter
import net.minecraft.world.phys.shapes.CollisionContext
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

//#if MC < 1.21.9
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonFloor
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockArea

//#else
//$$ import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
//#endif

@Module
object EventBus : EventBus() {
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

        WorldRenderEvents.AFTER_ENTITIES.register { context ->
            post(RenderEvent.World.AfterEntities(RenderContext.fromContext(context)))
        }

        //#if MC < 1.21.9
        WorldRenderEvents.LAST.register { context ->
            post(RenderEvent.World.Last(RenderContext.fromContext(context)))
        }

        WorldRenderEvents.BLOCK_OUTLINE.register { context, blockContext ->
            val ctx = RenderContext.fromContext(context).apply {
                blockPos = blockContext.blockPos()
                voxelShape = blockContext.blockState()
                    ?.getShape(
                        EmptyBlockGetter.INSTANCE,
                        blockContext.blockPos(),
                        CollisionContext.of(context.camera().entity
                        )
                    )
            }

            !post(RenderEvent.World.BlockOutline(ctx))
        }
        //#else
        //$$ WorldRenderEvents.END_MAIN.register { context ->
        //$$    post(RenderEvent.World.Last(RenderContext.fromContext(context)))
        //$$ }
        //$$
        //$$ WorldRendpackage co.stellarskys.stella.events
        //
        //import co.stellarskys.stella.annotations.Module
        //import co.stellarskys.stella.events.api.Event
        //import co.stellarskys.stella.events.api.EventBus
        //import co.stellarskys.stella.events.core.*
        //import co.stellarskys.stella.managers.events.EventBusManager
        //import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents
        //import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
        //import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
        //import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
        //import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
        //import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents
        //import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
        //import org.lwjgl.glfw.GLFW
        //import co.stellarskys.stella.events.core.GuiEvent
        //import co.stellarskys.stella.utils.render.RenderContext
        //import net.minecraft.network.protocol.Packet
        //import net.minecraft.world.level.EmptyBlockGetter
        //import net.minecraft.world.phys.shapes.CollisionContext
        //import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
        //
        ////#if MC < 1.21.9
        //import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
        ////#else
        ////$$ import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
        ////#endif
        //
        //@Module
        //object EventBus : EventBus(true) {
        //    init {
        //        ClientReceiveMessageEvents.ALLOW_GAME.register { message, isActionBar ->
        //            !post(ChatEvent.Receive(message, isActionBar))
        //        }
        //
        //        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
        //            post(ServerEvent.Connect())
        //        }
        //
        //        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
        //            post(ServerEvent.Disconnect())
        //        }
        //
        //        ClientLifecycleEvents.CLIENT_STARTED.register { _ ->
        //            post(GameEvent.Start())
        //        }
        //
        //        ClientLifecycleEvents.CLIENT_STOPPING.register { _ ->
        //            post(GameEvent.Stop())
        //        }
        //
        //        ClientEntityEvents.ENTITY_UNLOAD.register { entity, _ ->
        //            post(EntityEvent.Death(entity))
        //        }
        //
        //        ScreenEvents.BEFORE_INIT.register { _, screen, _, _ ->
        //            //#if MC >= 1.21.9
        //            //$$ ScreenMouseEvents.allowMouseClick(screen).register { _, click ->
        //            //$$    !post(GuiEvent.Click(click.x, click.y, click.button(), true, screen))
        //            //$$ }
        //            //$$
        //            //$$ ScreenMouseEvents.allowMouseRelease(screen).register { _, click ->
        //            //$$    !post(GuiEvent.Click(click.x, click.y, click.button(), false, screen))
        //            //$$ }
        //            //$$
        //            //$$ ScreenKeyboardEvents.allowKeyPress(screen).register { _, keyInput ->
        //            //$$    val charTyped = GLFW.glfwGetKeyName(keyInput.key, keyInput.scancode)?.firstOrNull() ?: '\u0000'
        //            //$$    !post(GuiEvent.Key(GLFW.glfwGetKeyName(keyInput.key, keyInput.scancode), keyInput.key, charTyped, keyInput.key, screen))
        //            //$$ }
        //            //#else
        //            ScreenMouseEvents.allowMouseClick(screen).register { _, mx, my, mouseButton ->
        //                !post(GuiEvent.Click(mx, my, mouseButton, true, screen))
        //            }
        //
        //            ScreenMouseEvents.allowMouseRelease(screen).register { _, mx, my, mouseButton ->
        //                !post(GuiEvent.Click(mx, my, mouseButton, false, screen))
        //            }
        //
        //            ScreenKeyboardEvents.allowKeyPress(screen).register { _, key, scancode, modifiers ->
        //                val charTyped = GLFW.glfwGetKeyName(key, scancode)?.firstOrNull() ?: '\u0000'
        //                !post(GuiEvent.Key(GLFW.glfwGetKeyName(key, scancode), key, charTyped, scancode, screen))
        //            }
        //            //#endif
        //        }
        //
        //        ScreenEvents.BEFORE_INIT.register { _, screen, _, _ ->
        //            if (screen != null) post(GuiEvent.Open(screen))
        //        }
        //
        //        WorldRenderEvents.AFTER_ENTITIES.register { context ->
        //            post(RenderEvent.World.AfterEntities(RenderContext.fromContext(context)))
        //        }
        //
        //        //#if MC < 1.21.9
        //        WorldRenderEvents.LAST.register { context ->
        //            post(RenderEvent.World.Last(RenderContext.fromContext(context)))
        //        }
        //
        //        WorldRenderEvents.BLOCK_OUTLINE.register { context, blockContext ->
        //            val ctx = RenderContext.fromContext(context).apply {
        //                blockPos = blockContext.blockPos()
        //                voxelShape = blockContext.blockState()
        //                    ?.getShape(
        //                        EmptyBlockGetter.INSTANCE,
        //                        blockContext.blockPos(),
        //                        CollisionContext.of(context.camera().entity
        //                        )
        //                    )
        //            }
        //
        //            !post(RenderEvent.World.BlockOutline(ctx))
        //        }
        //        //#else
        //        //$$ WorldRenderEvents.END_MAIN.register { context ->
        //        //$$    post(RenderEvent.World.Last(RenderContext.fromContext(context)))
        //        //$$ }
        //        //$$
        //        //$$ WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register { context, outlineRenderState ->
        //        //$$    val ctx = RenderContext.fromContext(context).apply {
        //        //$$        blockPos = outlineRenderState.pos
        //        //$$        voxelShape = outlineRenderState.shape
        //        //$$    }
        //        //$$    !post(RenderEvent.World.BlockOutline(ctx))
        //        //$$ }
        //        //#endif
        //    }
        //
        //    fun onPacketReceived(packet: Packet<*>): Boolean {
        //        return post(PacketEvent.Received(packet))
        //    }
        //
        //    inline fun <reified T : Event> registerIn(
        //        vararg islands: SkyBlockIsland,
        //        skyblockOnly: Boolean = false,
        //        noinline callback: (T) -> Unit
        //    ) {
        //        val eventCall = register<T>(add = false, callback = callback)
        //        val islandSet = if (islands.isNotEmpty()) islands.toSet() else null
        //        EventBusManager.trackConditionalEvent(islandSet, skyblockOnly, eventCall)
        //    }
        //}erEvents.BEFORE_BLOCK_OUTLINE.register { context, outlineRenderState ->
        //$$    val ctx = RenderContext.fromContext(context).apply {
        //$$        blockPos = outlineRenderState.pos
        //$$        voxelShape = outlineRenderState.shape
        //$$    }
        //$$    !post(RenderEvent.World.BlockOutline(ctx))
        //$$ }
        //#endif
    }

    fun onPacketReceived(packet: Packet<*>): Boolean {
        return post(PacketEvent.Received(packet))
    }

    inline fun <reified T : Event> on(
        vararg scope: Any,
        skyblockOnly: Boolean = false,
        register: Boolean = true,
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

        // create handle but don’t register yet
        val handle = EventHandle(this, T::class.java, handler)
        // track with manager
        EventBusManager.trackConditionalEvent(
            islands = islands.ifEmpty { null },
            arias = arias.ifEmpty { null },
            floors = floors.ifEmpty { null },
            skyblockOnly = skyblockOnly,
            handle = handle
        )

        if (register) handle.register()
        return handle
    }
}