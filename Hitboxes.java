package com.imperial.client.modules;

/** Makes other players' hitboxes bigger for your crosshair (see HitboxMixin). The server still uses the real hitbox, so keep it small. */
public final class Hitboxes extends Module {
    public static double expand = 0.2;
    private static Hitboxes instance;
    public Hitboxes() { super("hitboxes", "Hitboxes", Category.COMBAT); instance = this; }
    public static boolean active() { return instance != null && instance.isEnabled(); }
}
