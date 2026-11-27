package com.imperial.client.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;

public final class AutoTotem extends Module {
    public static int delayTicks = 1;
    private int cooldown;

    public AutoTotem() { super("auto_totem", "AutoTotem", Category.COMBAT); }

    @Override public void onTick(Minecraft mc) {
        if (cooldown > 0) { cooldown--; return; }
        var player = mc.player;
        if (mc.gameMode == null || mc.screen != null) return;
        if (player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;
        int slot = -1;
        for (int i = 0; i < 36; i++)
            if (player.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) { slot = i; break; }
        if (slot == -1) return;
        int menuSlot = slot < 9 ? slot + 36 : slot; // hotbar 0-8 -> 36-44, main 9-35 -> same
        mc.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, menuSlot, 40, ClickType.SWAP, player);
        cooldown = delayTicks;
    }
}
