package co.stellarskys.stella.mixins;

import co.stellarskys.stella.features.dungeons.utils.TintedBlockModel;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.chunk.SectionBuilder;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.model.Baker;
import net.minecraft.client.render.model.ModelBaker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ModelBaker.class)
public class MixinBlockRenderer {
    @ModifyReturnValue(method = "bake", at = @At("RETURN"))
    private CompletableFuture<ModelBaker.BakedModels> injectMoreBlockModels(CompletableFuture<ModelBaker.BakedModels> original, @Local ModelBaker.BakerImpl baker, @Local(argsOnly = true) Executor executor) {
        Baker b = baker;
        return original.thenCombine(
                CustomBlockTextures.createBakedModels(b, executor),
                (a, _void) -> a
        );
    }
}