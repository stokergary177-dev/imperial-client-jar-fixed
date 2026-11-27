package com.imperial.client.mixin;

import com.imperial.client.modules.XRay;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** X-ray: while enabled only ores (and a few valuables) have visible faces. require=0 so a mismatch can't crash the game. */
@Mixin(Block.class)
public abstract class BlockMixin {
    @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true, require = 0)
    private static void imperial$xray(BlockState state, BlockState neighbor, Direction dir, CallbackInfoReturnable<Boolean> cir) {
        if (XRay.active()) cir.setReturnValue(XRay.isVisible(state));
    }
}
