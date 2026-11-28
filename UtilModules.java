package com.imperial.client.modules;

import com.imperial.client.ImperialClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

/** Player / utility modules. */
public final class UtilModules {
    private UtilModules() {}

    /** Turns your view a little and hops every 20 seconds so you are not kicked for idling. */
    public static final class AntiAFK extends Module {
        private int ticks;
        public AntiAFK() { super("anti_afk", "AntiAFK", Category.PLAYER); }
        @Override public boolean persist() { return false; }
        @Override public void onTick(Minecraft mc) {
            if (mc.screen != null) return;
            var p = mc.player;
            if (++ticks % 400 == 0) {
                p.setYRot(p.getYRot() + (ticks % 800 == 0 ? 12f : -12f));
                if (p.onGround()) p.jumpFromGround();
            }
        }
    }

    /** Respawns you instantly and saves a "death" waypoint where you died (see /waypoint list). */
    public static final class AutoRespawn extends Module {
        private boolean recorded;
        private int cooldown;
        public AutoRespawn() { super("auto_respawn", "AutoRespawn", Category.PLAYER); }
        @Override public void onTick(Minecraft mc) {
            var p = mc.player;
            if (!p.isDeadOrDying()) { recorded = false; cooldown = 0; return; }
            if (!recorded) {
                recorded = true;
                Waypoints.add(mc, "death");
                ImperialClient.notify(mc, "Death point saved as waypoint \"death\"");
            }
            if (cooldown-- <= 0) { p.respawn(); cooldown = 10; }
        }
    }

    /** Open a chest / barrel and it moves everything into your inventory, one stack at a time. */
    public static final class ChestStealer extends Module {
        public static int delay = 2;
        private int ticks;
        public ChestStealer() { super("chest_stealer", "ChestStealer", Category.PLAYER); }
        @Override public boolean persist() { return false; }
        @Override public void onTick(Minecraft mc) {
            if (mc.gameMode == null || !(mc.player.containerMenu instanceof ChestMenu menu)) return;
            if (++ticks % Math.max(1, delay) != 0) return;
            int n = menu.getRowCount() * 9;
            for (int i = 0; i < n; i++)
                if (!menu.getSlot(i).getItem().isEmpty()) {
                    mc.gameMode.handleInventoryMouseClick(menu.containerId, i, 0, ClickType.QUICK_MOVE, mc.player);
                    return;
                }
        }
    }

    /** While you mine, switches to the fastest tool in your hotbar and switches back when you stop. */
    public static final class AutoTool extends Module {
        private int previous = -1;
        public AutoTool() { super("auto_tool", "AutoTool", Category.PLAYER); }
        @Override protected void onDisable(Minecraft mc) { restore(mc); }
        private void restore(Minecraft mc) {
            if (mc != null && mc.player != null && previous != -1) mc.player.getInventory().setSelectedSlot(previous);
            previous = -1;
        }
        @Override public void onTick(Minecraft mc) {
            var p = mc.player;
            if (mc.screen != null || !mc.options.keyAttack.isDown() || !(mc.hitResult instanceof BlockHitResult hit)) { restore(mc); return; }
            var state = mc.level.getBlockState(hit.getBlockPos());
            if (state.isAir()) return;
            int best = p.getInventory().getSelectedSlot();
            float bestSpeed = p.getInventory().getItem(best).getDestroySpeed(state);
            for (int i = 0; i < 9; i++) {
                ItemStack s = p.getInventory().getItem(i);
                float sp = s.getDestroySpeed(state);
                if (sp > bestSpeed + 0.01f) { bestSpeed = sp; best = i; }
            }
            if (best != p.getInventory().getSelectedSlot()) {
                if (previous == -1) previous = p.getInventory().getSelectedSlot();
                p.getInventory().setSelectedSlot(best);
            }
        }
    }
}
