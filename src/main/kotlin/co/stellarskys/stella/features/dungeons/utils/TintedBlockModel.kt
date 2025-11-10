package co.stellarskys.stella.features.dungeons.utils

import net.minecraft.block.Blocks
import net.minecraft.client.render.model.Baker
import net.minecraft.client.render.model.SimpleBlockStateModel
import net.minecraft.client.render.model.json.ModelVariant
import net.minecraft.registry.Registries
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

object TintedBlockModel {
    @JvmStatic
    fun createBakedModels(baker: Baker, executor: Executor): CompletableFuture<Void?> {
        return CompletableFuture.supplyAsync({
            // Bake stone once
            val stoneUnbaked = SimpleBlockStateModel.Unbaked(ModelVariant(Registries.BLOCK.getId(Blocks.STONE)))
            val stoneModel = stoneUnbaked.bake(baker)

            // Assign stone model to every replacement
            Registries.BLOCK.forEach { block ->
                val state = block.defaultState
                baker.models().put(state, stoneModel)
            }

            // Complete immediately since we don’t need async tasks
            replacements.modelBakingFuture.complete(Unit)
            CompletableFuture.completedFuture(null)
        }, executor)
    }

}
