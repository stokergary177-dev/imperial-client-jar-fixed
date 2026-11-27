package com.imperial.client.modules;

import com.imperial.client.ImperialClient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class Tracers extends Module {
    public Tracers() { super("tracers", "Tracers", Category.RENDER); }

    @Override public void onHud(GuiGraphics g, Minecraft mc, float delta) {
        for (Player o : mc.level.players()) {
            if (o == mc.player || o.isSpectator()) continue;
            Vec3 lp = o.getPosition(delta);
            ImperialClient.PROJECTOR.drawTracer(g, lp.x, lp.y + o.getBbHeight() / 2.0, lp.z, 0xCCFF3030);
        }
    }
}
