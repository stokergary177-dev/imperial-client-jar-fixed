package com.imperial.client.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side velocity flight (works on servers that allow flight; vanilla servers kick for "flying"
 * after ~4s, so a tiny periodic drop is applied as anti-kick. On your own server set allow-flight=true).
 */
public final class Flight extends Module {
    public static double speed = 0.7;
    private int ticks;
    public Flight() { super("flight", "Flight", Category.MOVEMENT, org.lwjgl.glfw.GLFW.GLFW_KEY_G); }
    @Override public boolean persist() { return false; }

    @Override public void onTick(Minecraft mc) {
        var p = mc.player;
        if (p.isFallFlying() || p.isPassenger()) return;
        double f = (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0);
        double s = (mc.options.keyLeft.isDown() ? 1 : 0) - (mc.options.keyRight.isDown() ? 1 : 0);
        double yaw = Math.toRadians(p.getYRot());
        double vx = -Math.sin(yaw) * f + Math.cos(yaw) * s;
        double vz = Math.cos(yaw) * f + Math.sin(yaw) * s;
        double len = Math.sqrt(vx * vx + vz * vz);
        if (len > 1e-4) { vx = vx / len * speed; vz = vz / len * speed; } else { vx = 0; vz = 0; }
        double vy = (mc.options.keyJump.isDown() ? speed : 0) - (mc.options.keyShift.isDown() ? speed : 0);
        if (vy == 0 && ++ticks % 30 == 0) vy = -0.05; // anti-kick nudge
        p.setDeltaMovement(new Vec3(vx, vy, vz));
        p.fallDistance = 0;
    }
}
