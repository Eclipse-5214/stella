package co.stellarskys.stella.utils.CompatHelpers

import co.stellarskys.stella.Stella
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.player.EntityPlayer

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

val EntityPlayerSP.yaw: Float
    get() = this.rotationYaw

val EntityPlayer.x: Double
    get() = this.posX

val EntityPlayer.y: Double
    get() = this.posY

val EntityPlayer.z: Double
    get() = this.posZ

val EntityPlayer.yaw: Float
    get() = this.rotationYaw