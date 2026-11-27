package com.imperial.client.modules;

import net.minecraft.client.Minecraft;

public final class Zoom extends Module {
    private int previousFov = 70;
    public Zoom() { super("zoom", "Zoom", Category.RENDER, org.lwjgl.glfw.GLFW.GLFW_KEY_V); }
    @Override public boolean persist() { return false; }
    @Override protected void onEnable(Minecraft mc) { previousFov = mc.options.fov().get(); mc.options.fov().set(30); }
    @Override protected void onDisable(Minecraft mc) { mc.options.fov().set(previousFov); }
}
