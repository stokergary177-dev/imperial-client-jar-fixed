package com.imperial.client.modules;

import com.imperial.client.Friends;
import com.imperial.client.ImperialClient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

public final class PlayerEsp extends Module {
    public PlayerEsp() { super("player_esp", "PlayerESP", Category.RENDER); }

    @Override public void onHud(GuiGraphics g, Minecraft mc, float delta) {
        Projector p = ImperialClient.PROJECTOR;
        for (Player o : mc.level.players()) {
            if (o == mc.player || o.isSpectator()) continue;
            Vec3 lp = o.getPosition(delta);
            AABB box = o.getBoundingBox().move(lp.x - o.getX(), lp.y - o.getY(), lp.z - o.getZ());
            boolean fr = Friends.isFriend(o);
            if (p.drawBox(g, box, fr ? 0xFF30FF30 : 0xFFFF3030, fr ? 0x2030FF30 : 0x20FF3030)) {
                double[] top = p.project(lp.x, box.maxY + 0.3, lp.z);
                if (top != null) {
                    String label = String.format(Locale.ROOT, "%s  %.0fhp  %.0fm", o.getName().getString(),
                            o.getHealth() + o.getAbsorptionAmount(), p.distance(lp.x, lp.y, lp.z));
                    g.drawString(mc.font, label, (int) top[0] - mc.font.width(label) / 2, (int) top[1] - 8, 0xFFFFFFFF, true);
                }
            }
        }
    }
}
