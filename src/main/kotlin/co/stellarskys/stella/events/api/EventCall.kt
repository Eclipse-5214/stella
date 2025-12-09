package co.stellarskys.stella.events.api

interface EventCall {
    fun unregister(): Boolean
    fun register(): Boolean
    fun isRegistered(): Boolean
}