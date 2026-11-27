package com.imperial.client.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class AutoEat extends Module {
    public static int hungerThreshold = 14;
    private int previousSlot = -1;
    private boolean eating;

    public AutoEat() { super("auto_eat", "AutoEat", Category.PLAYER); }

    @Override protected void onDisable(Minecraft mc) { stop(mc); }

    private void stop(Minecraft mc) {
        if (mc == null) return;
        mc.options.keyUse.setDown(false);
        if (mc.player != null && previousSlot != -1) mc.player.getInventory().setSelectedSlot(previousSlot);
        previousSlot = -1;
        eating = false;
    }

    private static boolean good(ItemStack s) {
        if (s.isEmpty() || s.get(DataComponents.FOOD) == null) return false;
        return !s.is(Items.ROTTEN_FLESH) && !s.is(Items.SPIDER_EYE) && !s.is(Items.POISONOUS_POTATO)
                && !s.is(Items.PUFFERFISH) && !s.is(Items.CHORUS_FRUIT) && !s.is(Items.SUSPICIOUS_STEW);
    }

    @Override public void onTick(Minecraft mc) {
        var player = mc.player;
        if (mc.screen != null) { if (eating) stop(mc); return; }
        int food = player.getFoodData().getFoodLevel();
        if (eating) {
            if (food >= 20 || (!player.isUsingItem() && !good(player.getMainHandItem()))) { stop(mc); return; }
            mc.options.keyUse.setDown(true);
            return;
        }
        if (food > hungerThreshold) return;
        for (int i = 0; i < 9; i++)
            if (good(player.getInventory().getItem(i))) {
                previousSlot = player.getInventory().getSelectedSlot();
                player.getInventory().setSelectedSlot(i);
                eating = true;
                return;
            }
    }
}
