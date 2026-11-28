package com.imperial.client.modules;

/** Anti-knockback: knockback packets for you are dropped (see VelocityMixin). */
public final class Velocity extends Module {
    private static Velocity instance;
    public Velocity() { super("velocity", "Velocity", Category.COMBAT); instance = this; }
    public static boolean active() { return instance != null && instance.isEnabled(); }
}
