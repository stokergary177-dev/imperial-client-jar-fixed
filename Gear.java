package com.imperial.client.modules;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Armor / gear helpers shared by AutoArmor, Nametags, ThreatRadar and KillAura priority. */
public final class Gear {
    private Gear() {}

    public static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    // index 0 = head, 1 = chest, 2 = legs, 3 = feet; each row goes from weakest to strongest
    private static final Item[][] ARMOR = {
            {Items.LEATHER_HELMET, Items.TURTLE_HELMET, Items.GOLDEN_HELMET, Items.CHAINMAIL_HELMET, Items.IRON_HELMET, Items.DIAMOND_HELMET, Items.NETHERITE_HELMET},
            {Items.LEATHER_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE},
            {Items.LEATHER_LEGGINGS, Items.GOLDEN_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.IRON_LEGGINGS, Items.DIAMOND_LEGGINGS, Items.NETHERITE_LEGGINGS},
            {Items.LEATHER_BOOTS, Items.GOLDEN_BOOTS, Items.CHAINMAIL_BOOTS, Items.IRON_BOOTS, Items.DIAMOND_BOOTS, Items.NETHERITE_BOOTS}
    };
    private static final int[] HEAD_TIER = {1, 1, 2, 2, 3, 4, 5};
    private static final int[] BODY_TIER = {1, 2, 2, 3, 4, 5};

    /** Which armor slot (0 head .. 3 feet) this item goes in, or -1 if it is not a known armor piece. */
    public static int slotOf(ItemStack s) {
        if (s.isEmpty()) return -1;
        for (int slot = 0; slot < 4; slot++)
            for (Item it : ARMOR[slot]) if (s.is(it)) return slot;
        return -1;
    }

    /** 1 leather .. 5 netherite, 0 = not armor. Enchantments are not counted. */
    public static int tier(ItemStack s) {
        if (s.isEmpty()) return 0;
        for (int slot = 0; slot < 4; slot++) {
            Item[] row = ARMOR[slot];
            int[] t = slot == 0 ? HEAD_TIER : BODY_TIER;
            for (int i = 0; i < row.length; i++) if (s.is(row[i])) return t[i];
        }
        return 0;
    }

    /** Rough danger rating of a player from what they wear and hold. Netherite set + mace is about 30. */
    public static int threat(Player p) {
        int t = 0;
        for (EquipmentSlot sl : ARMOR_SLOTS) t += tier(p.getItemBySlot(sl));
        ItemStack main = p.getMainHandItem(), off = p.getOffhandItem();
        if (main.is(Items.MACE)) t += 6;
        else if (main.is(Items.NETHERITE_SWORD)) t += 4;
        else if (main.is(Items.DIAMOND_SWORD) || main.is(Items.NETHERITE_AXE)) t += 3;
        if (main.is(Items.END_CRYSTAL) || off.is(Items.END_CRYSTAL)) t += 4;
        if (off.is(Items.TOTEM_OF_UNDYING)) t += 2;
        if (p.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) t += 2;
        return t;
    }

    /** Short gear summary like "NETHx4 ELYTRA MACE TOTEM". */
    public static String tags(Player p) {
        int[] cnt = new int[6];
        for (EquipmentSlot sl : ARMOR_SLOTS) cnt[tier(p.getItemBySlot(sl))]++;
        StringBuilder sb = new StringBuilder();
        if (cnt[5] > 0) sb.append("NETHx").append(cnt[5]).append(' ');
        if (cnt[4] > 0) sb.append("DIAx").append(cnt[4]).append(' ');
        if (cnt[3] > 0) sb.append("IRONx").append(cnt[3]).append(' ');
        if (p.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) sb.append("ELYTRA ");
        ItemStack main = p.getMainHandItem(), off = p.getOffhandItem();
        if (main.is(Items.MACE)) sb.append("MACE ");
        else if (main.is(Items.NETHERITE_SWORD) || main.is(Items.DIAMOND_SWORD)) sb.append("SWORD ");
        if (main.is(Items.END_CRYSTAL) || off.is(Items.END_CRYSTAL)) sb.append("CRYSTAL ");
        if (off.is(Items.TOTEM_OF_UNDYING)) sb.append("TOTEM ");
        return sb.toString().trim();
    }
}
