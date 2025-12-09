@file:Suppress("UNUSED")

package co.stellarskys.stella.events.core

import net.minecraft.network.protocol.Packet
import co.stellarskys.stella.events.api.CancellableEvent
import co.stellarskys.stella.events.api.Event

abstract class PacketEvent {
    class Received(
        val packet: Packet<*>
    ) : CancellableEvent()

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