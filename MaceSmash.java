package com.imperial.client.modules;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Mace smash from the ground. Right before a melee hit with a mace, we send the server a short chain of
 * position packets (up, then back down to where you really are, never "on ground"), so the server thinks
 * you fell and the mace does smash damage.
 *
 * A vanilla/Paper server allows the Nth move packet of a tick to travel about sqrt(100*N) blocks, so the
 * fake jump is split into 2-4 packets that each stay under that limit. Needs free space above your head.
 */
public final class MaceSmash extends Module {
    /**
     * Fake fall height in blocks (4-22). 9 or less = 2 packets (safest). More = more damage but more
     * likely to be rejected (you get snapped into the air). Change in game with /mace <blocks>.
     */
    public static int maxHeight = 9;
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
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
                dispatcher.register(ClientCommandManager.literal("mace")
                        .then(ClientCommandManager.argument("blocks", IntegerArgumentType.integer(4, 22))
                                .executes(ctx -> {
                                    maxHeight = IntegerArgumentType.getInteger(ctx, "blocks");
                                    ctx.getSource().sendFeedback(Component.literal("[IMPERIAL] MaceSmash height = " + maxHeight + " blocks"));
                                    return 1;
                                }))));
    }

    private void smash(Minecraft mc) {
        var p = mc.player;
        var conn = mc.getConnection();
        if (conn == null || !p.getMainHandItem().is(Items.MACE)) return;
        if (!p.onGround() && p.fallDistance > 1.5) return; // really falling already: normal smash works

        // free blocks above our head
        int h = 0;
        while (h < Math.min(maxHeight, 22) && mc.level.noCollision(p, p.getBoundingBox().move(0, h + 1, 0))) h++;
        if (h < 4) return; // no room, don't risk a rubberband

        double x = p.getX(), y = p.getY(), z = p.getZ();
        List<Double> ys = new ArrayList<>();
        double up1 = Math.min(h, 9.5);          // packet 1: limit ~10 blocks
        ys.add(y + up1);
        if (h - up1 > 0.001) ys.add(y + h);     // packet 2: limit ~14 blocks
        double down1 = Math.min(h, 16.5);       // packet 3: limit ~17 blocks
        if (h - down1 > 0.001) { ys.add(y + h - down1); ys.add(y); } // packet 4 = back to the real spot
        else ys.add(y);

        for (int i = 0; i < ys.size(); i++) {
            boolean last = i == ys.size() - 1;
            conn.send(new ServerboundMovePlayerPacket.Pos(x, ys.get(i), z, false, last && p.horizontalCollision));
        }
    }
}
