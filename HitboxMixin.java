package com.imperial.client.mixin;

import com.imperial.client.modules.Hitboxes;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Bigger crosshair hitbox for other players while Hitboxes is on. require=0 so a mismatch can't crash the game. */
@Mixin(Entity.class)
public abstract class HitboxMixin {
    @Inject(method = "getPickRadius", at = @At("HEAD"), cancellable = true, require = 0)
    private void imperial$pick(CallbackInfoReturnable<Float> cir) {
        if (!Hitboxes.active()) return;
        Entity self = (Entity) (Object) this;
        if (self instanceof Player && self != Minecraft.getInstance().player) cir.setReturnValue((float) Hitboxes.expand);
    }
}
