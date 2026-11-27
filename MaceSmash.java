package com.imperial.client.modules;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;

/**
 * Mace smash exploit: before each melee hit with a mace, tells the server you went up and fell back down,
 * so the server counts a big fall and the mace does smash damage without you actually jumping.
 * Works on servers that don't check movement strictly (e.g. most anarchy servers). Needs clear space above you.
 */
public final class MaceSmash extends Module {
    /** Max fake fall height in blocks. More = more damage, but servers reject moves above ~100. */
    public static int maxHeight = 22;
    private static MaceSmash instance;

    public MaceSmash() {
        super("mace_smash", "MaceSmash", Category.COMBAT);
        instance = this;
    }

    @Override public boolean persist() { return false; }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            Minecraft mc = Minecraft.getInstance();
            if (instance != null && instance.isEnabled() && level.isClientSide() && player == mc.player)
                instance.smash(mc);
            return InteractionResult.PASS;
        });
    }

    private void smash(Minecraft mc) {
        var p = mc.player;
        var conn = mc.getConnection();
        if (conn == null || !p.getMainHandItem().is(Items.MACE)) return;
        if (!p.onGround() && p.fallDistance > 1.5) return; // already falling: a real smash works

        int h = 0; // how many blocks of free space above us
        while (h < maxHeight && mc.level.noCollision(p, p.getBoundingBox().move(0, h + 1, 0))) h++;
        if (h < 3) return;

        double x = p.getX(), y = p.getY(), z = p.getZ();
        conn.send(new ServerboundMovePlayerPacket.Pos(x, y + h, z, false, false));
        conn.send(new ServerboundMovePlayerPacket.Pos(x, y, z, false, p.horizontalCollision));
    }
}
