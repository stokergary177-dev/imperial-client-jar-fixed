package com.imperial.client.modules;

import com.imperial.client.Friends;
import com.imperial.client.ImperialClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/** Name, health, held item and gear summary above every player, visible through walls. */
public final class Nametags extends Module {
    public Nametags() { super("nametags", "Nametags", Category.RENDER); }

    @Override public void onHud(GuiGraphics g, Minecraft mc, float delta) {
        Projector p = ImperialClient.PROJECTOR;
        for (Player o : mc.level.players()) {
            if (o == mc.player || o.isSpectator()) continue;
            Vec3 lp = o.getPosition(delta);
            double[] top = p.project(lp.x, lp.y + o.getBbHeight() + 0.7, lp.z);
            if (top == null) continue;
            float hp = o.getHealth() + o.getAbsorptionAmount();
            String l1 = String.format(Locale.ROOT, "%s  %.0fhp", o.getName().getString(), hp);
            String held = o.getMainHandItem().isEmpty() ? "" : o.getMainHandItem().getHoverName().getString();
            String l2 = (Gear.tags(o) + "  " + held).trim();
            int c1 = Friends.isFriend(o) ? 0xFF55FF55 : hp >= 15 ? 0xFFFFFFFF : hp >= 8 ? 0xFFFFAA00 : 0xFFFF5555;
            int x = (int) top[0], y = (int) top[1];
            int w = Math.max(mc.font.width(l1), mc.font.width(l2));
            g.fill(x - w / 2 - 2, y - 22, x + w / 2 + 2, y + (l2.isEmpty() ? -11 : 0), 0x90000000);
            g.drawString(mc.font, l1, x - mc.font.width(l1) / 2, y - 20, c1, true);
            if (!l2.isEmpty()) g.drawString(mc.font, l2, x - mc.font.width(l2) / 2, y - 10, 0xFFB9A0D8, true);
        }
    }
}
