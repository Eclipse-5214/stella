@file:Suppress("UNUSED")

package co.stellarskys.stella.events.core

import net.minecraft.network.protocol.Packet
import co.stellarskys.stella.events.api.Event

abstract class PacketEvent {
    class Received(
        val packet: Packet<*>
    ) : Event(cancelable = true)

    class ReceivedPost(
        val packet: Packet<*>
    ) : Event()

    class Sent(
        val packet: Packet<*>
    ) : Event()
    
    class SentPost(
        val packet: Packet<*>
    ) : Event()
}