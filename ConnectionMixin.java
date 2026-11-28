package com.imperial.client.mixin;

import com.imperial.client.modules.LagSwitch;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets LagSwitch hold outgoing movement packets. require=0 so a mismatch can never crash the game. */
@Mixin(Connection.class)
public abstract class ConnectionMixin {
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void imperial$holdPackets(Packet<?> packet, CallbackInfo ci) {
        if (LagSwitch.shouldHold(packet)) {
            LagSwitch.hold(packet);
            ci.cancel();
        }
    }
}
