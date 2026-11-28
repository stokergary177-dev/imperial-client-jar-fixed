package com.imperial.client.modules;

import com.imperial.client.Friends;
import com.imperial.client.ImperialClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Custom: lists nearby players with the direction they are in (relative to where you look), distance, health,
 * gear tags, and whether they are closing in (<<) or backing off (>>). Dangerous players (good gear) are colored
 * hotter and trigger a one-time warning when they come within 80 blocks.
 */
public final class ThreatRadar extends Module {
    public static double range = 128;
    private static final int ALERT_THREAT = 14;

    private record Row(String name, double dist, double delta, float rel, float hp, int threat, String tags, boolean friend) {}

    private final List<Row> rows = new ArrayList<>();
    private final Map<UUID, Double> last = new HashMap<>();
    private final Map<UUID, Long> alerted = new HashMap<>();
    private int ticks;

    public ThreatRadar() { super("threat_radar", "ThreatRadar", Category.RENDER); }

    private static String dir(float rel) {
        float a = Math.abs(rel);
        String side = rel > 0 ? "R" : "L";
        if (a < 22.5f) return "AHEAD";
        if (a < 67.5f) return "AHEAD-" + side;
        if (a < 112.5f) return rel > 0 ? "RIGHT" : "LEFT";
        if (a < 157.5f) return "BEHIND-" + side;
        return "BEHIND";
    }

    @Override public void onTick(Minecraft mc) {
        if (ticks++ % 10 != 0) return;
        List<Row> next = new ArrayList<>();
        Map<UUID, Double> cur = new HashMap<>();
        long now = mc.level.getGameTime();
        var me = mc.player;
        for (Player o : mc.level.players()) {
            if (o == me || o.isSpectator()) continue;
            double d = me.distanceTo(o);
            if (d > range) continue;
            Double prev = last.get(o.getUUID());
            double delta = prev == null ? 0 : d - prev;
            cur.put(o.getUUID(), d);
            float bearing = (float) Math.toDegrees(Math.atan2(-(o.getX() - me.getX()), o.getZ() - me.getZ()));
            float rel = Mth.wrapDegrees(bearing - me.getYRot());
            int threat = Gear.threat(o);
            boolean friend = Friends.isFriend(o);
            next.add(new Row(o.getName().getString(), d, delta, rel, o.getHealth() + o.getAbsorptionAmount(), threat, Gear.tags(o), friend));
            if (!friend && threat >= ALERT_THREAT && d < 80) {
                Long t = alerted.get(o.getUUID());
                if (t == null || now - t > 1200) {
                    alerted.put(o.getUUID(), now);
                    ImperialClient.notify(mc, String.format(Locale.ROOT, "Threat: %s %.0fm [%s]", o.getName().getString(), d, Gear.tags(o)));
                }
            }
        }
        next.sort(Comparator.comparingDouble(Row::dist));
        last.clear();
        last.putAll(cur);
        rows.clear();
        rows.addAll(next);
    }

    @Override public void onHud(GuiGraphics g, Minecraft mc, float delta) {
        int right = ImperialClient.PROJECTOR.width() - 4, y = 110;
        String header = "THREAT RADAR (" + rows.size() + ")";
        g.drawString(mc.font, header, right - mc.font.width(header), y, 0xFFFF7755, true);
        y += 11;
        int shown = 0;
        for (Row r : rows) {
            if (shown++ >= 6) break;
            String closing = r.delta() < -3 ? " <<" : r.delta() > 3 ? " >>" : "";
            String line = String.format(Locale.ROOT, "%s  %s  %.0fm%s  %.0fhp  %s", r.name(), dir(r.rel()), r.dist(), closing, r.hp(), r.tags()).trim();
            int color = r.friend() ? 0xFF55FF55 : r.threat() >= 18 ? 0xFFFF5555 : r.threat() >= 10 ? 0xFFFFAA00 : 0xFFFFFF55;
            g.drawString(mc.font, line, right - mc.font.width(line), y, color, true);
            y += 10;
        }
    }
}
