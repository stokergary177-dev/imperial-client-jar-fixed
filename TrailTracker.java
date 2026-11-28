package com.imperial.client.modules;

import com.imperial.client.ImperialClient;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Custom: remembers where every player was while you could see them and draws their path as dots (older = dimmer).
 * When a player leaves your render distance a "last seen" marker stays where they went, so you can follow them home.
 * /trail clear wipes the trails.
 */
public final class TrailTracker extends Module {
    private static final int MAX_POINTS = 300;
    private static final long FORGET_TICKS = 24000; // 20 minutes
    private static final int[] COLORS = {0xFFFF5555, 0xFF55FF55, 0xFF5599FF, 0xFFFFFF55, 0xFFFF55FF, 0xFF55FFFF};

    private static final class Trail {
        String name = "?", dim = "";
        final ArrayDeque<Vec3> pts = new ArrayDeque<>();
        long lastSeen;
    }
    private static final Map<UUID, Trail> TRAILS = new HashMap<>();
    private int ticks;

    public TrailTracker() { super("trail_tracker", "TrailTracker", Category.RENDER); }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
                dispatcher.register(ClientCommandManager.literal("trail")
                        .then(ClientCommandManager.literal("clear").executes(ctx -> {
                            TRAILS.clear();
                            ctx.getSource().sendFeedback(Component.literal("[IMPERIAL] trails cleared"));
                            return 1;
                        }))));
    }

    @Override public void onTick(Minecraft mc) {
        if (ticks++ % 5 != 0) return;
        String dim = Waypoints.dimTag(mc);
        long now = mc.level.getGameTime();
        for (Player o : mc.level.players()) {
            if (o == mc.player || o.isSpectator()) continue;
            Trail t = TRAILS.computeIfAbsent(o.getUUID(), k -> new Trail());
            t.name = o.getName().getString();
            if (!dim.equals(t.dim)) { t.pts.clear(); t.dim = dim; }
            Vec3 pos = o.position();
            Vec3 last = t.pts.peekLast();
            if (last == null || last.distanceToSqr(pos) > 1.0) {
                t.pts.addLast(pos);
                if (t.pts.size() > MAX_POINTS) t.pts.removeFirst();
            }
            t.lastSeen = now;
        }
        TRAILS.values().removeIf(t -> now - t.lastSeen > FORGET_TICKS);
    }

    @Override public void onHud(GuiGraphics g, Minecraft mc, float delta) {
        Projector p = ImperialClient.PROJECTOR;
        String dim = Waypoints.dimTag(mc);
        long now = mc.level.getGameTime();
        for (Map.Entry<UUID, Trail> e : TRAILS.entrySet()) {
            Trail t = e.getValue();
            if (!dim.equals(t.dim) || t.pts.isEmpty()) continue;
            int base = COLORS[Math.floorMod(t.name.hashCode(), COLORS.length)];
            int n = t.pts.size(), i = 0;
            for (Vec3 v : t.pts) {
                i++;
                double[] s = p.project(v.x, v.y + 0.1, v.z);
                if (s == null) continue;
                int alpha = 70 + (int) (185.0 * i / n);
                int sx = (int) s[0], sy = (int) s[1];
                g.fill(sx - 1, sy - 1, sx + 1, sy + 1, (alpha << 24) | (base & 0xFFFFFF));
            }
            if (mc.level.getPlayerByUUID(e.getKey()) == null) { // out of render distance: show the last-seen spot
                Vec3 v = t.pts.peekLast();
                double[] s = p.project(v.x, v.y + 1, v.z);
                if (s != null) {
                    String label = String.format(java.util.Locale.ROOT, "%s (seen %ds ago)  %.0fm", t.name, (now - t.lastSeen) / 20, p.distance(v.x, v.y, v.z));
                    int sx = (int) s[0], sy = (int) s[1];
                    g.fill(sx - 3, sy - 3, sx + 3, sy + 3, base);
                    g.drawString(mc.font, label, sx + 6, sy - 4, base, true);
                }
            }
        }
    }
}
