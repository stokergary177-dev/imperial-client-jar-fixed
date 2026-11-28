package com.imperial.client.modules;

import com.imperial.client.ImperialClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Boxes around dropped items; valuable ones (totems, netherite, elytra, gear...) are gold and named. */
public final class ItemEsp extends Module {
    public static double maxDistance = 96;
    public ItemEsp() { super("item_esp", "ItemESP", Category.RENDER); }

    private static boolean valuable(ItemStack s) {
        return s.is(Items.TOTEM_OF_UNDYING) || s.is(Items.ELYTRA) || s.is(Items.ENCHANTED_GOLDEN_APPLE)
                || s.is(Items.NETHERITE_INGOT) || s.is(Items.NETHERITE_SCRAP) || s.is(Items.ANCIENT_DEBRIS)
                || s.is(Items.DIAMOND) || s.is(Items.MACE) || s.is(Items.TRIDENT) || s.is(Items.END_CRYSTAL)
                || s.is(Items.NETHERITE_SWORD) || s.is(Items.NETHERITE_PICKAXE) || Gear.tier(s) >= 4;
    }

    @Override public void onHud(GuiGraphics g, Minecraft mc, float delta) {
        Projector p = ImperialClient.PROJECTOR;
        int drawn = 0;
        for (var e : mc.level.entitiesForRendering()) {
            if (!(e instanceof ItemEntity ie) || drawn >= 120) continue;
            Vec3 lp = ie.getPosition(delta);
            if (p.distance(lp.x, lp.y, lp.z) > maxDistance) continue;
            ItemStack s = ie.getItem();
            boolean v = valuable(s);
            AABB box = ie.getBoundingBox().move(lp.x - ie.getX(), lp.y - ie.getY(), lp.z - ie.getZ()).inflate(0.1);
            int color = v ? 0xFFFFD700 : 0xFF9AA0A6;
            if (p.drawBox(g, box, color, v ? 0x40FFD700 : 0x10FFFFFF)) {
                drawn++;
                if (v) {
                    double[] top = p.project(lp.x, box.maxY + 0.2, lp.z);
                    if (top != null) {
                        String label = s.getCount() + "x " + s.getHoverName().getString();
                        g.drawString(mc.font, label, (int) top[0] - mc.font.width(label) / 2, (int) top[1] - 8, color, true);
                    }
                }
            }
        }
    }
}
