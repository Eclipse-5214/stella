package co.stellarskys.stella.features.msc.lockUtils

import co.stellarskys.stella.api.handlers.Capsule

object LockRegistry {
    val state = Capsule("slot_locks", LockData())
    fun getData() = state()

    fun getActiveProfile(): LockData.LockProfile = state().getActive()
    fun switchProfile(name: String) { state.update { activeProfile = name } }

    fun deleteProfile(name: String) { state.update {
        activeProfile = "default"
        profiles.remove(name)
    }}
}