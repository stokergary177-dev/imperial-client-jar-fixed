package com.imperial.client.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Hides every block face except ores/valuables. Turn Fullbright on too so ores in caves are lit. */
public final class XRay extends Module {
    private static volatile boolean active;
    public XRay() { super("xray", "XRay", Category.RENDER, org.lwjgl.glfw.GLFW.GLFW_KEY_X); }
    @Override public boolean persist() { return false; }

    public static boolean active() { return active; }

    public static boolean isVisible(BlockState s) {
        Block b = s.getBlock();
        return s.is(BlockTags.COAL_ORES) || s.is(BlockTags.IRON_ORES) || s.is(BlockTags.GOLD_ORES)
                || s.is(BlockTags.DIAMOND_ORES) || s.is(BlockTags.EMERALD_ORES) || s.is(BlockTags.LAPIS_ORES)
                || s.is(BlockTags.REDSTONE_ORES) || s.is(BlockTags.COPPER_ORES)
                || b == Blocks.ANCIENT_DEBRIS || b == Blocks.NETHER_GOLD_ORE || b == Blocks.NETHER_QUARTZ_ORE
                || b == Blocks.SPAWNER || b == Blocks.CHEST || b == Blocks.TRAPPED_CHEST || b == Blocks.BARREL
                || b == Blocks.ENDER_CHEST || s.is(BlockTags.SHULKER_BOXES) || b == Blocks.TNT
                || b == Blocks.LAVA || b == Blocks.WATER;
    }

    private static void reload(Minecraft mc) { if (mc.levelRenderer != null && mc.level != null) mc.levelRenderer.allChanged(); }
    @Override protected void onEnable(Minecraft mc) { active = true; reload(mc); }
    @Override protected void onDisable(Minecraft mc) { active = false; reload(mc); }
}
