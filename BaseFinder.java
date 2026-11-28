package com.imperial.client.modules;

import com.imperial.client.ImperialClient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.*;

/** Scores loaded chunks by player-made blocks (underground included). Villages will score too. */
public final class BaseFinder extends Module {
    public static int threshold = 30;
    private static final int CHUNKS_PER_TICK = 2;
    private static final long RESCAN_MS = 45_000;

    private final Map<Long, Integer> scores = new HashMap<>();
    private final Map<Long, Long> scannedAt = new HashMap<>();
    private final Set<Long> announced = new HashSet<>();
    private final List<Map.Entry<Long, Integer>> top = new ArrayList<>();
    private int ticks;

    public BaseFinder() { super("base_finder", "BaseFinder", Category.WORLD); }

    @Override protected void onDisable(Minecraft mc) { scores.clear(); scannedAt.clear(); announced.clear(); top.clear(); }

    private static boolean strong(BlockState s) {
        Block b = s.getBlock();
        return s.is(BlockTags.BEDS) || s.is(BlockTags.DOORS) || s.is(BlockTags.SHULKER_BOXES)
                || b == Blocks.CRAFTING_TABLE || b == Blocks.FURNACE || b == Blocks.BLAST_FURNACE
                || b == Blocks.SMOKER || b == Blocks.CHEST || b == Blocks.TRAPPED_CHEST
                || b == Blocks.BARREL || b == Blocks.ENDER_CHEST || b == Blocks.HOPPER
                || b == Blocks.ENCHANTING_TABLE || b == Blocks.ANVIL || b == Blocks.BREWING_STAND
                || b == Blocks.BEACON || b == Blocks.DISPENSER || b == Blocks.DROPPER
                || b == Blocks.OBSERVER || b == Blocks.PISTON || b == Blocks.STICKY_PISTON;
    }

    private static boolean weak(BlockState s) {
        Block b = s.getBlock();
        return s.is(BlockTags.PLANKS) || s.is(BlockTags.WOOL) || s.is(BlockTags.WOODEN_FENCES)
                || s.is(BlockTags.SIGNS) || s.is(BlockTags.CONCRETE_POWDER)
                || b == Blocks.GLASS || b == Blocks.GLASS_PANE || b == Blocks.BRICKS
                || b == Blocks.TORCH || b == Blocks.WALL_TORCH || b == Blocks.LANTERN
                || b == Blocks.GLOWSTONE || b == Blocks.SEA_LANTERN || b == Blocks.NETHER_BRICKS
                || b == Blocks.COBBLESTONE_STAIRS || b == Blocks.STONE_BRICK_STAIRS
                || b == Blocks.POLISHED_ANDESITE || b == Blocks.POLISHED_DEEPSLATE
                || b == Blocks.SMOOTH_STONE || b == Blocks.REDSTONE_WIRE || b == Blocks.REPEATER
                || b == Blocks.COMPARATOR || b == Blocks.LADDER || b == Blocks.RAIL;
    }

    private static boolean interesting(BlockState s) { return strong(s) || weak(s); }

    @Override public void onTick(Minecraft mc) {
        int px = mc.player.chunkPosition().x, pz = mc.player.chunkPosition().z;
        int r = Math.min(mc.options.getEffectiveRenderDistance(), 24);
        long now = System.currentTimeMillis();
        int done = 0;
        outer:
        for (int d = 0; d <= r; d++)
            for (int cx = px - d; cx <= px + d; cx++)
                for (int cz = pz - d; cz <= pz + d; cz++) {
                    if (Math.max(Math.abs(cx - px), Math.abs(cz - pz)) != d) continue;
                    long key = ChunkPos.asLong(cx, cz);
                    Long last = scannedAt.get(key);
                    if (last != null && now - last < RESCAN_MS) continue;
                    ChunkAccess c = mc.level.getChunk(cx, cz, ChunkStatus.FULL, false);
                    if (!(c instanceof LevelChunk lc)) continue;
                    scores.put(key, scan(lc));
                    scannedAt.put(key, now);
                    if (++done >= CHUNKS_PER_TICK) break outer;
                }
        if (ticks++ % 20 == 0) rebuild(mc);
    }

    private int scan(LevelChunk chunk) {
        int score = 0;
        for (BlockEntity be : chunk.getBlockEntities().values())
            if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity
                    || be instanceof ShulkerBoxBlockEntity || be instanceof EnderChestBlockEntity) score += 4;
        for (LevelChunkSection section : chunk.getSections()) {
            if (section == null || section.hasOnlyAir() || !section.maybeHas(BaseFinder::interesting)) continue;
            for (int x = 0; x < 16; x++)
                for (int y = 0; y < 16; y++)
                    for (int z = 0; z < 16; z++) {
                        BlockState s = section.getBlockState(x, y, z);
                        if (s.isAir()) continue;
                        if (strong(s)) score += 3; else if (weak(s)) score += 1;
                    }
        }
        return score;
    }

    private void rebuild(Minecraft mc) {
        top.clear();
        for (Map.Entry<Long, Integer> e : scores.entrySet()) if (e.getValue() >= threshold) top.add(e);
        int px = mc.player.chunkPosition().x, pz = mc.player.chunkPosition().z;
        top.sort(Comparator.comparingLong(e -> {
            ChunkPos cp = new ChunkPos(e.getKey());
            long dx = cp.x - px, dz = cp.z - pz;
            return dx * dx + dz * dz;
        }));
        for (Map.Entry<Long, Integer> e : top)
            if (announced.add(e.getKey())) {
                ChunkPos cp = new ChunkPos(e.getKey());
                ImperialClient.notify(mc, "BaseFinder: possible base near " + (cp.x * 16 + 8) + ", " + (cp.z * 16 + 8) + " (score " + e.getValue() + ")");
            }
    }

    @Override public void onHud(GuiGraphics g, Minecraft mc, float delta) {
        Projector p = ImperialClient.PROJECTOR;
        int y = 4, right = p.width() - 4, shown = 0;
        String header = "BaseFinder (" + top.size() + ")";
        g.drawString(mc.font, header, right - mc.font.width(header), y, 0xFFFFD700, true);
        y += 11;
        for (Map.Entry<Long, Integer> e : top) {
            if (shown++ >= 8) break;
            ChunkPos cp = new ChunkPos(e.getKey());
            int wx = cp.x * 16 + 8, wz = cp.z * 16 + 8;
            double dist = Math.sqrt(Math.pow(wx - mc.player.getX(), 2) + Math.pow(wz - mc.player.getZ(), 2));
            String line = String.format(Locale.ROOT, "%d, %d  [%d]  %.0fm", wx, wz, e.getValue(), dist);
            g.drawString(mc.font, line, right - mc.font.width(line), y, 0xFFFFFFFF, true);
            y += 10;
            double[] s = p.project(wx, mc.player.getY() + 1, wz);
            if (s != null) {
                int sx = (int) s[0], sy = (int) s[1];
                g.fill(sx - 3, sy - 3, sx + 3, sy + 3, 0xFFFFD700);
                g.drawString(mc.font, "base?", sx + 6, sy - 4, 0xFFFFD700, true);
            }
        }
    }
}
