package com.imperial.client.modules;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/**
 * /vclip <blocks> moves you straight up (or down if negative); /hclip <blocks> moves you forward where you look.
 * Up to about 20 blocks. The move is sent as up to 5 packets so the server's per-tick move limit lets it through.
 * You need free space at the target or the server will pull you back.
 */
public final class Clip {
    private Clip() {}

    private static final double[] LIMITS = {9.5, 13.5, 16.5, 19.0, 21.0};

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
            dispatcher.register(ClientCommandManager.literal("vclip")
                    .then(ClientCommandManager.argument("blocks", DoubleArgumentType.doubleArg(-21, 21)).executes(ctx -> {
                        move(Minecraft.getInstance(), 0, DoubleArgumentType.getDouble(ctx, "blocks"), 0, ctx.getSource());
                        return 1;
                    })));
            dispatcher.register(ClientCommandManager.literal("hclip")
                    .then(ClientCommandManager.argument("blocks", DoubleArgumentType.doubleArg(-21, 21)).executes(ctx -> {
                        Minecraft mc = Minecraft.getInstance();
                        double d = DoubleArgumentType.getDouble(ctx, "blocks");
                        double yaw = Math.toRadians(mc.player.getYRot());
                        move(mc, -Math.sin(yaw) * d, 0, Math.cos(yaw) * d, ctx.getSource());
                        return 1;
                    })));
        });
    }

    private static void move(Minecraft mc, double dx, double dy, double dz, net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource src) {
        var p = mc.player;
        var conn = mc.getConnection();
        if (p == null || conn == null) return;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 0.01) return;
        double x = p.getX(), y = p.getY(), z = p.getZ();
        for (int i = 0; i < LIMITS.length; i++) {
            double reach = Math.min(dist, LIMITS[i]);
            double f = reach / dist;
            conn.send(new ServerboundMovePlayerPacket.Pos(x + dx * f, y + dy * f, z + dz * f, false, p.horizontalCollision));
            if (reach >= dist) break;
        }
        p.setPos(x + dx, y + dy, z + dz);
        src.sendFeedback(Component.literal("[IMPERIAL] clipped " + Math.round(dist * 10) / 10.0 + " blocks"));
    }
}
