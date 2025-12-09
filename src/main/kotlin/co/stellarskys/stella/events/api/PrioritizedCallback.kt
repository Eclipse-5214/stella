package co.stellarskys.stella.events.api

data class PrioritizedCallback<T : Event>(
    val priority: Int,
    val callback: (T) -> Unit
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PrioritizedCallback<*>) return false
        return callback === other.callback
    }

    override fun hashCode(): Int = callback.hashCode()
}