package com.imperial.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Sliders and toggles for every entry in {@link Settings}. Saved when you leave the screen. */
public final class SettingsScreen extends Screen {
    private final Screen parent;

    public SettingsScreen(Screen parent) {
        super(Component.literal("IMPERIAL SETTINGS"));
        this.parent = parent;
    }

    private static Component toggleLabel(Settings.Setting s) {
        return Component.literal(s.label + ": " + s.format());
    }

    @Override
    protected void init() {
        int n = Settings.ALL.size();
        int perCol = Math.max(1, (this.height - 80) / 24);
        int cols = (n + perCol - 1) / perCol;
        int w = Math.min(220, (this.width - 20) / cols - 6);
        int rows = Math.min(n, perCol);
        int left = this.width / 2 - (cols * (w + 6) - 6) / 2;
        int top = Math.max(28, this.height / 2 - (rows * 24 + 30) / 2);
        int i = 0, y = top;
        for (Settings.Setting s : Settings.ALL) {
            int x = left + (i / perCol) * (w + 6);
            y = top + (i % perCol) * 24;
            i++;
            if (s.toggle) {
                this.addRenderableWidget(Button.builder(toggleLabel(s), b -> {
                    s.setClamped(s.get.getAsDouble() > 0.5 ? 0 : 1);
                    b.setMessage(toggleLabel(s));
                }).bounds(x, y, w, 20).build());
            } else {
                this.addRenderableWidget(new Slider(x, y, w, 20, s));
            }
        }
        this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> this.onClose())
                .bounds(this.width / 2 - 40, top + rows * 24 + 6, 80, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, "IMPERIAL SETTINGS", this.width / 2, 12, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        ImperialClient.saveConfig();
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private static final class Slider extends AbstractSliderButton {
        private final Settings.Setting s;

        Slider(int x, int y, int w, int h, Settings.Setting s) {
            super(x, y, w, h, Component.empty(), (s.get.getAsDouble() - s.min) / (s.max - s.min));
            this.s = s;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            if (s == null) return; // super constructor may call this before the field is set
            this.setMessage(Component.literal(s.label + ": " + s.format()));
        }

        @Override
        protected void applyValue() {
            double v = s.min + this.value * (s.max - s.min);
            v = s.min + Math.round((v - s.min) / s.step) * s.step;
            s.setClamped(v);
        }
    }
}
