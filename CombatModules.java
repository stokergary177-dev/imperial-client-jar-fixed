package com.imperial.client.modules;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Combat helper modules. */
public final class CombatModules {
    private CombatModules() {}

    /** Every melee hit is sent as a critical: a tiny hop packet goes out right before the attack. */
    public static final class Criticals extends Module {
        private static Criticals instance;
        public Criticals() { super("criticals", "Criticals", Category.COMBAT); instance = this; }

        public static void register() {
            AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
                Minecraft mc = Minecraft.getInstance();
                if (instance != null && instance.isEnabled() && level.isClientSide() && player == mc.player) instance.crit(mc);
                return InteractionResult.PASS;
            });
        }

        private void crit(Minecraft mc) {
            var p = mc.player;
            var conn = mc.getConnection();
            if (conn == null || !p.onGround() || p.isInWater() || p.isInLava() || p.onClimbable()) return;
            double x = p.getX(), y = p.getY(), z = p.getZ();
            conn.send(new ServerboundMovePlayerPacket.Pos(x, y + 0.0625, z, false, false));
            conn.send(new ServerboundMovePlayerPacket.Pos(x, y, z, false, false));
        }
    }

    /** Wears the best armor in your inventory and swaps in better pieces as you find them. */
    public static final class AutoArmor extends Module {
        private int cooldown;
        public AutoArmor() { super("auto_armor", "AutoArmor", Category.COMBAT); }

        @Override public void onTick(Minecraft mc) {
            if (mc.screen != null || mc.gameMode == null) return;
            if (cooldown > 0) { cooldown--; return; }
            var p = mc.player;
            int id = p.inventoryMenu.containerId;
            for (int slot = 0; slot < 4; slot++) {
                ItemStack worn = p.getItemBySlot(Gear.ARMOR_SLOTS[slot]);
                if (!worn.isEmpty() && Gear.slotOf(worn) != slot) continue; // elytra, pumpkin... leave it alone
                int bestTier = Gear.tier(worn), best = -1;
                for (int m = 9; m <= 44; m++) {
                    ItemStack s = p.inventoryMenu.getSlot(m).getItem();
                    if (Gear.slotOf(s) == slot && Gear.tier(s) > bestTier) { bestTier = Gear.tier(s); best = m; }
                }
                if (best == -1) continue;
                int armorMenuSlot = 5 + slot; // 5 head, 6 chest, 7 legs, 8 feet
                if (worn.isEmpty()) {
                    mc.gameMode.handleInventoryMouseClick(id, best, 0, ClickType.QUICK_MOVE, p);
                } else {
                    mc.gameMode.handleInventoryMouseClick(id, best, 0, ClickType.PICKUP, p);
                    mc.gameMode.handleInventoryMouseClick(id, armorMenuSlot, 0, ClickType.PICKUP, p);
                    mc.gameMode.handleInventoryMouseClick(id, best, 0, ClickType.PICKUP, p);
                }
                cooldown = 5;
                return;
            }
        }
    }

    /** Eats a golden apple from your hotbar when your health drops below the set level. */
    public static final class AutoGapple extends Module {
        public static double healthBelow = 10;
        private int previousSlot = -1, eatTicks, cooldown;
        private boolean eating;

        public AutoGapple() { super("auto_gapple", "AutoGapple", Category.COMBAT); }

        @Override protected void onDisable(Minecraft mc) { stop(mc); }

        private void stop(Minecraft mc) {
            if (mc == null) return;
            mc.options.keyUse.setDown(false);
            if (mc.player != null && previousSlot != -1) mc.player.getInventory().setSelectedSlot(previousSlot);
            previousSlot = -1;
            eating = false;
            cooldown = 10;
        }

        @Override public void onTick(Minecraft mc) {
            var p = mc.player;
            if (mc.screen != null) { if (eating) stop(mc); return; }
            if (cooldown > 0) { cooldown--; return; }
            if (eating) {
                eatTicks++;
                mc.options.keyUse.setDown(true);
                if (eatTicks > 80 || (eatTicks > 8 && !p.isUsingItem())) stop(mc);
                return;
            }
            if (p.getHealth() > healthBelow || p.getAbsorptionAmount() >= 4) return;
            int slot = -1;
            for (int i = 0; i < 9 && slot == -1; i++)
                if (p.getInventory().getItem(i).is(Items.ENCHANTED_GOLDEN_APPLE)) slot = i;
            for (int i = 0; i < 9 && slot == -1; i++)
                if (p.getInventory().getItem(i).is(Items.GOLDEN_APPLE)) slot = i;
            if (slot == -1) return;
            previousSlot = p.getInventory().getSelectedSlot();
            p.getInventory().setSelectedSlot(slot);
            eating = true;
            eatTicks = 0;
        }
    }

    /** Last-resort logout: disconnects you when health is at or below the set level and you have no totem left. */
    public static final class Panic extends Module {
        public static double logoutHealth = 5;
        private boolean fired; // fires once; re-arms when your health is back above the level
        public Panic() { super("panic", "Panic", Category.COMBAT); }

        private static boolean hasTotem(net.minecraft.client.player.LocalPlayer p) {
            if (p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return true;
            for (int i = 0; i < 36; i++) if (p.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) return true;
            return false;
        }

        @Override public void onTick(Minecraft mc) {
            var p = mc.player;
            if (p.getHealth() > logoutHealth) { fired = false; return; }
            if (fired || p.isDeadOrDying() || hasTotem(p)) return;
            var conn = mc.getConnection();
            if (conn == null) return;
            fired = true;
            conn.getConnection().disconnect(Component.literal("[IMPERIAL] Panic logout at " + Math.round(p.getHealth()) + " hp"));
        }
    }
}
