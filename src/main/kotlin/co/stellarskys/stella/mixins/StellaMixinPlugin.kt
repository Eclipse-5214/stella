package co.stellarskys.stella.mixins

import org.spongepowered.asm.lib.tree.ClassNode
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin
import org.spongepowered.asm.mixin.extensibility.IMixinInfo

class StellaMixinPlugin : IMixinConfigPlugin {
    private val mixins = mutableSetOf<String>()

    override fun onLoad(mixinPackage: String) {
        println("[StellaMixinPlugin] onLoad called with mixinPackage: $mixinPackage")

        //#if MC >= 1.21.5
        mixins += "MixinGameRenderer"
        mixins += "MixinNetworkManager"
        mixins += "accessors.AccessorNetHandlerPlayClient"
        mixins += "accessors.AccessorMapState"
        //#elseif MC == 1.8.9
        //$$ mixins += "MixinNetworkManager"
        //$$ mixins += "accessors.AccessorNetHandlerPlayClient"
        // Exclude GameRenderer and MapState for 1.8.9
        //#endif

        println("[StellaMixinPlugin] Registered mixins:")
        mixins.forEach { println("  - $it") }
    }

    override fun getMixins(): List<String> {
        println("[StellaMixinPlugin] getMixins called")
        return mixins.toList()
    }

    override fun shouldApplyMixin(targetClassName: String, mixinClassName: String): Boolean {
        val simpleName = mixinClassName.substringAfterLast('.')
        val shouldApply = mixins.any { it.endsWith(simpleName) }

        println("[StellaMixinPlugin] shouldApplyMixin called:")
        println("  - Target class: $targetClassName")
        println("  - Mixin class: $mixinClassName")
        println("  - Simple name: $simpleName")
        println("  - Should apply: $shouldApply")

        return shouldApply
    }

    override fun getRefMapperConfig(): String? {
        println("[StellaMixinPlugin] getRefMapperConfig called")
        return null
    }

    override fun acceptTargets(myTargets: Set<String>, otherTargets: Set<String>) {
        println("[StellaMixinPlugin] acceptTargets called")
        println("  - My targets: $myTargets")
        println("  - Other targets: $otherTargets")
    }

    override fun preApply(targetClassName: String, targetClass: ClassNode, mixinClassName: String, mixinInfo: IMixinInfo) {
        println("[StellaMixinPlugin] preApply called for $mixinClassName on $targetClassName")
    }

    override fun postApply(targetClassName: String, targetClass: ClassNode, mixinClassName: String, mixinInfo: IMixinInfo) {
        println("[StellaMixinPlugin] postApply called for $mixinClassName on $targetClassName")
    }
}
