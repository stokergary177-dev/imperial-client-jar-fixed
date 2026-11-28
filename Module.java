package com.imperial.client.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public abstract class Module {
    public enum Category {
        RENDER("Render"), WORLD("World"), MOVEMENT("Movement"), COMBAT("Combat"), PLAYER("Player");
        public final String label;
        Category(String label) { this.label = label; }
    }

    private final String id, name;
    private final Category category;
    private final int defaultKey;
    private boolean enabled;

    protected Module(String id, String name, Category category, int defaultKey) {
        this.id = id; this.name = name; this.category = category; this.defaultKey = defaultKey;
    }
    protected Module(String id, String name, Category category) { this(id, name, category, GLFW.GLFW_KEY_UNKNOWN); }

    public String id() { return id; }
    public String name() { return name; }
    public Category category() { return category; }
    public int defaultKey() { return defaultKey; }
    public boolean isEnabled() { return enabled; }
    /** false = never restored from the config at startup (modules that change game options). */
    public boolean persist() { return true; }

    public void setEnabled(boolean v) { if (v == enabled) return; enabled = v; if (v) onEnable(Minecraft.getInstance()); else onDisable(Minecraft.getInstance()); }
    public void toggle() { setEnabled(!enabled); }

    protected void onEnable(Minecraft mc) {}
    protected void onDisable(Minecraft mc) {}
    public void onTick(Minecraft mc) {}
    public void onHud(GuiGraphics g, Minecraft mc, float delta) {}
}
