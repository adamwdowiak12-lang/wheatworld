package com.wheatworld.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Makes all farmland permanently fully moist and impossible to turn back into dirt. */
@Mixin(FarmlandBlock.class)
public abstract class FarmlandBlockMixin {

    /** Vanilla random tick is where farmland dries out. Replace it with "force moisture 7". */
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void wheatworld$alwaysWet(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        if (state.get(FarmlandBlock.MOISTURE) != FarmlandBlock.MAX_MOISTURE) {
            world.setBlockState(pos, state.with(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE), Block.NOTIFY_LISTENERS);
        }
        ci.cancel();
    }

    /** Every path that turns farmland into dirt (trampling, blocked-above tick, no-crop tick) ends here. */
    @Inject(method = "setToDirt", at = @At("HEAD"), cancellable = true)
    private static void wheatworld$neverDirt(Entity entity, BlockState state, World world, BlockPos pos, CallbackInfo ci) {
        ci.cancel();
    }
}
