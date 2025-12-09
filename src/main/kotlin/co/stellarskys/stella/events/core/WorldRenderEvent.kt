package co.stellarskys.stella.events.core

import co.stellarskys.stella.events.api.CancellableEvent
import co.stellarskys.stella.events.api.Event
import net.minecraft.world.phys.HitResult

sealed class WorldRenderEvent {
    class Start(val context: RenderContext) : Event()

    class AfterSetup(val context: RenderContext) : Event()

    class BeforeEntities(val context: RenderContext) : Event()

    class AfterEntities(val context: RenderContext) : Event()

    class BeforeBlockOutline(val context: RenderContext, val hitResult: HitResult?) : CancellableEvent()

    class BlockOutline(val context: RenderContext) : CancellableEvent()

    class BeforeDebugRender(val context: RenderContext) : Event()

    class AfterTranslucent(val context: RenderContext) : Event()

    class Last(val context: RenderContext) : Event()

    class End(val context: RenderContext) : Event()

    class InvalidateRenderState : Event()

    class RenderWeather(val context: RenderContext) : CancellableEvent()

    class RenderClouds(val context: RenderContext) : CancellableEvent()

    class RenderSky(val context: RenderContext) : CancellableEvent()
}