package co.stellarskys.stella.managers

import co.stellarskys.stella.api.handlers.Atlas

interface ModuleProvider {
    val modules: List<Class<*>>
    val commands: List<Atlas>
}