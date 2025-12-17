package co.stellarskys.stella.features.dungeons

import co.stellarskys.stella.Stella.SHORTPREFIX
import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.ChatEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.utils.ChatUtils
import co.stellarskys.stella.utils.clearCodes
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

@Module
object termTracker : Feature("termTracker", island = SkyBlockIsland.THE_CATACOMBS) {
    private lateinit var completed: MutableMap<String, MutableMap<String, Int>>
    private val pattern = Regex("""^(\w{1,16}) (?:activated|completed) a (\w+)! \(\d/\d\)$""")

    override fun initialize() {
        completed = mutableMapOf()
        on<ChatEvent.Receive> { event ->
            val msg = event.message.string.clearCodes()
            val matcher = pattern.find(msg)

            when {
                msg == "The Core entrance is opening!" -> {
                    completed.forEach { (user, data) ->
                        ChatUtils.fakeMessage("$SHORTPREFIX §b$user §7completed §f${data["terminal"] ?: 0} §7 terms, §f${data["device"] ?: 0} §7devices, and §f${data["lever"] ?: 0} §7levers!")
                    }
                }
                pattern.matches(msg) -> {
                    val match = pattern.matchEntire(msg)!!
                    val (user, type) = match.destructured
                    if (type in listOf("terminal", "lever", "device")) {
                        completed.getOrPut(user) { mutableMapOf() }[type] =
                            (completed[user]?.get(type) ?: 0) + 1
                    }
                }
            }
        }
    }

    override fun onRegister() {
        if (this::completed.isInitialized) completed.clear()
        super.onRegister()
    }

    override fun onUnregister() {
        if (this::completed.isInitialized) completed.clear()
        super.onUnregister()
    }
}