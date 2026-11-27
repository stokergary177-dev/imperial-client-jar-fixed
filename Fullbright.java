package com.imperial.client.modules;

import net.minecraft.client.Minecraft;

public final class Fullbright extends Module {
    private double previousGamma = 0.5;
    public Fullbright() { super("fullbright", "Fullbright", Category.RENDER, org.lwjgl.glfw.GLFW.GLFW_KEY_B); }
    @Override public boolean persist() { return false; }
    @Override protected void onEnable(Minecraft mc) { previousGamma = mc.options.gamma().get(); mc.options.gamma().set(1.0); }
    @Override protected void onDisable(Minecraft mc) { mc.options.gamma().set(previousGamma); }
}
