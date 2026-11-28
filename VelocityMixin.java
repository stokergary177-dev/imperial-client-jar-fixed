package com.imperial.client.mixin;

import com.imperial.client.modules.Velocity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Drops the server's "set your velocity" packet for your own player. require=0 so a mismatch can't crash the game. */
@Mixin(ClientPacketListener.class)
public abstract class VelocityMixin {
    @Inject(method = "handleSetEntityMotion", at = @At("HEAD"), cancellable = true, require = 0)
    private void imperial$velocity(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (Velocity.active() && mc.player != null && packet.getId() == mc.player.getId()) ci.cancel();
    }
}
