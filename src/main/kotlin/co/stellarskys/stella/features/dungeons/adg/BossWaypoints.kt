package co.stellarskys.stella.features.dungeons.adg

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.api.config.core.Keybind
import co.stellarskys.stella.api.dungeons.Dungeon
import co.stellarskys.stella.api.dungeons.players.DungeonPlayerManager
import co.stellarskys.stella.api.dungeons.utils.DungeonClass
import co.stellarskys.stella.api.dungeons.utils.Phases
import co.stellarskys.stella.api.handlers.Chronos
import co.stellarskys.stella.api.zenith.client
import co.stellarskys.stella.events.core.DungeonEvent
import co.stellarskys.stella.events.core.RenderEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.features.dungeons.adg.editor.BossWaypointEditor
import co.stellarskys.stella.utils.config
import net.minecraft.world.phys.BlockHitResult
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

//? if >= 26.2 {
/*import co.stellarskys.stella.api.zenith.setScreen
*///? }

@Module
object BossWaypoints: Feature("bossWP", island = SkyBlockIsland.THE_CATACOMBS) {
    val editing by config.property<Boolean>("bossWP.editing")
    val bind by config.property<Keybind.Handler>("bossWP.addWaypoint")

    val dclass get() = DungeonPlayerManager.you?.dclass ?: DungeonClass.UNKNOWN
    val registry get() = WaypointRegistry.waypoints()
    var waypoints: List<WaypointRegistry.Waypoint> = listOf()

    override fun initialize() {
        on<DungeonEvent.PhaseChange> { waypoints = registry.get(it.phase, dclass) }
        on<DungeonEvent.Start> { waypoints = registry.get(Phases.F7.P1, dclass) }

        on<RenderEvent.World.Last> {
            if (editing) registry.getAll().forEach { it.render() }
            else if (Dungeon.inBoss && Dungeon.floorNumber == 7) waypoints.forEach { it.render() }
        }

        bind.onPress {
            if (!editing) return@onPress
            val hitresult = client.hitResult as? BlockHitResult ?: return@onPress

            Chronos.Tick post {
                client.setScreen(BossWaypointEditor(hitresult.blockPos, Phases.currentPhase, dclass))
            }
        }
    }
}