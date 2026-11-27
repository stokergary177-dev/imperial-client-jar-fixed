package com.imperial.client;

import com.imperial.client.modules.Module;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class ImperialScreen extends Screen {
    private static final int PURPLE = 0xFF9B59FF;
    private static final int PANEL = 0xE9151020;
    private static final int COL_W = 96, BTN_H = 18, GAP = 4;
    private int panelLeft, panelTop, panelW, panelH;

    public ImperialScreen() { super(Component.literal("IMPERIAL CLIENT")); }

    private static Component label(Module m) {
        return Component.literal(m.name()).withStyle(m.isEnabled() ? ChatFormatting.GREEN : ChatFormatting.GRAY);
    }

    @Override
    protected void init() {
        Module.Category[] cats = Module.Category.values();
        int tallest = 0;
        List<List<Module>> cols = new ArrayList<>();
        for (Module.Category c : cats) {
            List<Module> l = new ArrayList<>();
            for (Module m : ImperialClient.MODULES) if (m.category() == c) l.add(m);
            cols.add(l);
            tallest = Math.max(tallest, l.size());
        }
        panelW = cats.length * (COL_W + GAP) + GAP + 8;
        panelH = 58 + tallest * (BTN_H + GAP) + 30;
        panelLeft = this.width / 2 - panelW / 2;
        panelTop = this.height / 2 - panelH / 2;
        for (int c = 0; c < cats.length; c++) {
            int x = panelLeft + 4 + GAP + c * (COL_W + GAP);
            int y = panelTop + 50;
            for (Module m : cols.get(c)) {
                this.addRenderableWidget(Button.builder(label(m), b -> {
                    ImperialClient.toggle(this.minecraft, m);
                    b.setMessage(label(m));
                }).bounds(x, y, COL_W, BTN_H).build());
                y += BTN_H + GAP;
            }
        }
        this.addRenderableWidget(Button.builder(Component.literal("Close"), b -> this.onClose())
                .bounds(this.width / 2 - 40, panelTop + panelH - 24, 80, 18).build());
    }

    // 1.21.11's Screen.render() already draws the blurred background once.
    // Draw our panel inside renderBackground (after the blur) and do NOT call it from render().
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(panelLeft, panelTop, panelLeft + panelW, panelTop + panelH, PANEL);
        g.fill(panelLeft, panelTop, panelLeft + panelW, panelTop + 3, PURPLE);
        g.drawCenteredString(this.font, "IMPERIAL CLIENT", this.width / 2, panelTop + 10, 0xFFFFFFFF);
        g.drawCenteredString(this.font, "Right Shift: menu  |  /imperial <module>  |  bind keys in Controls",
                this.width / 2, panelTop + 22, 0xFFB9A0D8);
        Module.Category[] cats = Module.Category.values();
        for (int c = 0; c < cats.length; c++)
            g.drawString(this.font, cats[c].label, panelLeft + 4 + GAP + c * (COL_W + GAP), panelTop + 38, PURPLE, true);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
