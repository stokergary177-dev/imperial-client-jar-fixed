package com.imperial.client.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/** Multiplayer no-fall (the built-in one only protects the integrated-server player). */
public final class NoFallMp extends Module {
    public NoFallMp() { super("nofall_mp", "NoFall", Category.MOVEMENT); }

    @Override public void onTick(Minecraft mc) {
        var p = mc.player;
        if (p.fallDistance > 2.5 && !p.isFallFlying() && !p.getAbilities().flying && mc.getConnection() != null)
            mc.getConnection().send(new ServerboundMovePlayerPacket.StatusOnly(true, p.horizontalCollision));
    }
}
