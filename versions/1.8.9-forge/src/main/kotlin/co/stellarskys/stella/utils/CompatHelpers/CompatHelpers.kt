package co.stellarskys.stella.utils.CompatHelpers

import net.minecraft.client.entity.EntityPlayerSP

// text -> string
val String.string: String
    get() = this

// player stuff
val EntityPlayerSP.x: Double
    get() = this.posX

val EntityPlayerSP.y: Double
    get() = this.posY

val EntityPlayerSP.z: Double
    get() = this.posZ