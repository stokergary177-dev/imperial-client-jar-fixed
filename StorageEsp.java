package com.imperial.client.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public final class StorageEsp extends Module {
    public static double maxDistance = 160;
    private record Hit(BlockPos pos, int color) {}
    private final List<Hit> hits = new ArrayList<>();
    private int ticks;

    public StorageEsp() { super("storage_esp", "StorageESP", Category.RENDER); }

    @Override public void onTick(Minecraft mc) {
        if (ticks++ % 10 != 0) return;
        List<Hit> next = new ArrayList<>();
        int px = mc.player.chunkPosition().x, pz = mc.player.chunkPosition().z;
        int r = Math.min(mc.options.getEffectiveRenderDistance(), 32);
        for (int cx = px - r; cx <= px + r; cx++)
            for (int cz = pz - r; cz <= pz + r; cz++) {
                ChunkAccess c = mc.level.getChunk(cx, cz, ChunkStatus.FULL, false);
                if (!(c instanceof LevelChunk lc)) continue;
                for (BlockEntity be : lc.getBlockEntities().values()) {
                    int color = colorFor(be);
                    if (color != 0) next.add(new Hit(be.getBlockPos(), color));
                }
            }
        synchronized (hits) { hits.clear(); hits.addAll(next); }
    }

    private static int colorFor(BlockEntity be) {
        if (be instanceof TrappedChestBlockEntity) return 0xFFFF4040;
        if (be instanceof ChestBlockEntity) return 0xFFFFA500;
        if (be instanceof BarrelBlockEntity) return 0xFFC08040;
        if (be instanceof ShulkerBoxBlockEntity) return 0xFFFF55FF;
        if (be instanceof EnderChestBlockEntity) return 0xFFAA55FF;
        if (be instanceof HopperBlockEntity) return 0xFF909090;
        if (be instanceof DispenserBlockEntity) return 0xFF55AAFF; // droppers too
        if (be instanceof AbstractFurnaceBlockEntity) return 0xFFDDDDDD;
        return 0;
    }

    @Override public void onHud(GuiGraphics g, Minecraft mc, float delta) {
        Projector p = ImperialClient.PROJECTOR;
        synchronized (hits) {
            for (Hit h : hits) {
                BlockPos bp = h.pos();
                if (p.distance(bp.getX() + .5, bp.getY() + .5, bp.getZ() + .5) > maxDistance) continue;
                p.drawBox(g, new AABB(bp), h.color(), (0x30 << 24) | (h.color() & 0xFFFFFF));
            }
        }
    }
}
