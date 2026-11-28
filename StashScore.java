package com.imperial.client.modules;

import com.imperial.client.ImperialClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.*;

/**
 * Custom: finds stashes. Counts chests, barrels and shulker boxes per chunk, adds up each chunk with its 8
 * neighbours (so a stash that crosses chunk borders counts as one), and lists the best clusters with how many
 * containers they hold and how far away they are. Shulkers weigh the most. BaseFinder looks at all player blocks;
 * this looks only at storage.
 */
public final class StashScore extends Module {
    public static int minContainers = 6;
    private record Cluster(int cx, int cz, int containers, int shulkers, int weight) {}

    private final List<Cluster> clusters = new ArrayList<>();
    private final Set<Long> announced = new HashSet<>();
    private int ticks;

    public StashScore() { super("stash_score", "StashScore", Category.WORLD); }

    @Override protected void onDisable(Minecraft mc) { clusters.clear(); announced.clear(); }

    @Override public void onTick(Minecraft mc) {
        if (ticks++ % 40 != 0) return;
        Map<Long, int[]> per = new HashMap<>(); // {containers, shulkers, weight}
        int px = mc.player.chunkPosition().x, pz = mc.player.chunkPosition().z;
        int r = Math.min(mc.options.getEffectiveRenderDistance(), 32);
        for (int cx = px - r; cx <= px + r; cx++)
            for (int cz = pz - r; cz <= pz + r; cz++) {
                ChunkAccess c = mc.level.getChunk(cx, cz, ChunkStatus.FULL, false);
                if (!(c instanceof LevelChunk lc)) continue;
                int n = 0, sh = 0, w = 0;
                for (BlockEntity be : lc.getBlockEntities().values()) {
                    if (be instanceof ShulkerBoxBlockEntity) { n++; sh++; w += 5; }
                    else if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity) { n++; w += 2; }
                }
                if (n > 0) per.put(ChunkPos.asLong(cx, cz), new int[]{n, sh, w});
            }

        // sum each chunk with its neighbours
        List<Cluster> all = new ArrayList<>();
        for (Long key : per.keySet()) {
            ChunkPos cp = new ChunkPos(key);
            int n = 0, sh = 0, w = 0;
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    int[] v = per.get(ChunkPos.asLong(cp.x + dx, cp.z + dz));
                    if (v != null) { n += v[0]; sh += v[1]; w += v[2]; }
                }
            if (n >= minContainers) all.add(new Cluster(cp.x, cp.z, n, sh, w));
        }
        all.sort(Comparator.comparingInt(Cluster::weight).reversed());

        // keep the best cluster of each area (skip anything within 2 chunks of one already kept)
        List<Cluster> kept = new ArrayList<>();
        for (Cluster c : all) {
            boolean near = false;
            for (Cluster k : kept) if (Math.max(Math.abs(c.cx() - k.cx()), Math.abs(c.cz() - k.cz())) <= 2) { near = true; break; }
            if (near) continue;
            kept.add(c);
            if (kept.size() >= 5) break;
        }
        clusters.clear();
        clusters.addAll(kept);
        for (Cluster c : kept)
            if (announced.add(ChunkPos.asLong(c.cx(), c.cz())))
                ImperialClient.notify(mc, "Stash: " + c.containers() + " containers near " + (c.cx() * 16 + 8) + ", " + (c.cz() * 16 + 8));
    }

    @Override public void onHud(GuiGraphics g, Minecraft mc, float delta) {
        Projector p = ImperialClient.PROJECTOR;
        int right = p.width() - 4;
        int y = p.height() - 14 - 10 * (clusters.size() + 1) - 30;
        String header = "STASH SCORE (" + clusters.size() + ")";
        g.drawString(mc.font, header, right - mc.font.width(header), y, 0xFF55DDFF, true);
        y += 11;
        for (Cluster c : clusters) {
            int wx = c.cx() * 16 + 8, wz = c.cz() * 16 + 8;
            double dist = Math.sqrt(Math.pow(wx - mc.player.getX(), 2) + Math.pow(wz - mc.player.getZ(), 2));
            String line = String.format(Locale.ROOT, "%d, %d  %d boxes%s  %.0fm", wx, wz, c.containers(),
                    c.shulkers() > 0 ? " (" + c.shulkers() + " shulkers)" : "", dist);
            g.drawString(mc.font, line, right - mc.font.width(line), y, 0xFFFFFFFF, true);
            y += 10;
            double[] s = p.project(wx, mc.player.getY() + 1, wz);
            if (s != null) {
                int sx = (int) s[0], sy = (int) s[1];
                g.fill(sx - 3, sy - 3, sx + 3, sy + 3, 0xFF55DDFF);
                g.drawString(mc.font, "stash " + c.containers(), sx + 6, sy - 4, 0xFF55DDFF, true);
            }
        }
    }
}
