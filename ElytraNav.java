package com.imperial.client.modules;

import com.imperial.client.ImperialClient;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

/**
 * Custom: elytra autopilot. Start gliding yourself, then /elytra <waypoint> or /elytra goto <x> <z>.
 * It steers toward the target, climbs with fireworks from your hotbar when it has them (otherwise it
 * dives and climbs to keep its speed up), and switches itself off when you are within 25 blocks. /elytra stop cancels.
 */
public final class ElytraNav extends Module {
    /** How many blocks above the target height to cruise. */
    public static double cruise = 40;
    private static ElytraNav instance;
    private static double tx, ty, tz;
    private static boolean hasTarget;
    private int idle, fireTimer, restoreSlot = -1;

    public ElytraNav() { super("elytra_nav", "ElytraNav", Category.MOVEMENT); instance = this; }

    @Override public boolean persist() { return false; }

    private static void go(double x, double y, double z, String label) {
        tx = x; ty = y; tz = z; hasTarget = true;
        Minecraft mc = Minecraft.getInstance();
        ImperialClient.notify(mc, "ElytraNav -> " + label);
        if (instance != null) { instance.setEnabled(false); instance.setEnabled(true); }
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
                dispatcher.register(ClientCommandManager.literal("elytra")
                        .then(ClientCommandManager.literal("stop").executes(ctx -> {
                            if (instance != null) instance.setEnabled(false);
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("goto")
                                .then(ClientCommandManager.argument("x", IntegerArgumentType.integer())
                                        .then(ClientCommandManager.argument("z", IntegerArgumentType.integer()).executes(ctx -> {
                                            Minecraft mc = Minecraft.getInstance();
                                            int x = IntegerArgumentType.getInteger(ctx, "x"), z = IntegerArgumentType.getInteger(ctx, "z");
                                            go(x, mc.player.getY(), z, x + ", " + z);
                                            return 1;
                                        }))))
                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                .suggests((c, b) -> { for (Waypoints.Wp w : Waypoints.all()) b.suggest(w.name()); return b.buildFuture(); })
                                .executes(ctx -> {
                                    Minecraft mc = Minecraft.getInstance();
                                    String n = StringArgumentType.getString(ctx, "name");
                                    Waypoints.Wp w = Waypoints.find(mc, n);
                                    if (w == null) {
                                        ctx.getSource().sendFeedback(Component.literal("[IMPERIAL] no waypoint named " + n + " in this dimension"));
                                        return 0;
                                    }
                                    go(w.x(), w.y(), w.z(), w.name());
                                    return 1;
                                }))));
    }

    @Override protected void onEnable(Minecraft mc) { idle = 0; fireTimer = 0; }

    @Override protected void onDisable(Minecraft mc) {
        if (mc != null && mc.player != null && restoreSlot != -1) mc.player.getInventory().setSelectedSlot(restoreSlot);
        restoreSlot = -1;
    }

    @Override public void onTick(Minecraft mc) {
        var p = mc.player;
        if (restoreSlot != -1) { p.getInventory().setSelectedSlot(restoreSlot); restoreSlot = -1; }
        if (!hasTarget) { setEnabled(false); return; }
        if (!p.isFallFlying()) {
            if (++idle > 60) { ImperialClient.notify(mc, "ElytraNav: not gliding, stopped"); setEnabled(false); }
            return;
        }
        idle = 0;
        double dx = tx - p.getX(), dz = tz - p.getZ();
        if (Math.sqrt(dx * dx + dz * dz) < 25) {
            ImperialClient.notify(mc, "ElytraNav: arrived");
            setEnabled(false);
            return;
        }
        // steer: turn toward the target at most 10 degrees per tick
        float want = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float diff = net.minecraft.util.Mth.wrapDegrees(want - p.getYRot());
        p.setYRot(p.getYRot() + Math.max(-10f, Math.min(10f, diff)));

        // pitch: dive when slow, climb when low, otherwise glide down gently
        double speed = p.getDeltaMovement().length();
        double minAlt = ty + cruise;
        float wantPitch = speed < 0.75 ? 35f : (p.getY() < minAlt ? -40f : 10f);
        p.setXRot(p.getXRot() + Math.max(-6f, Math.min(6f, wantPitch - p.getXRot())));

        if (p.getY() < minAlt && speed < 1.1 && ++fireTimer >= 30) {
            fireTimer = 0;
            useFirework(mc);
        }
    }

    private void useFirework(Minecraft mc) {
        var p = mc.player;
        if (mc.gameMode == null) return;
        for (int i = 0; i < 9; i++)
            if (p.getInventory().getItem(i).is(Items.FIREWORK_ROCKET)) {
                if (restoreSlot == -1) restoreSlot = p.getInventory().getSelectedSlot();
                p.getInventory().setSelectedSlot(i);
                mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
                return;
            }
    }
}
