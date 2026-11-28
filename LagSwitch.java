package com.imperial.client.modules;

import com.imperial.client.ImperialClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;

import java.util.ArrayDeque;
import java.util.Locale;

/**
 * Lag switch / burst: while ON, your movement AND your attacks/actions are held back instead of sent.
 * Others see you frozen where you were; on your own screen you keep playing. When you turn it OFF (or it
 * auto-releases) everything is sent in order, so you appear next to the target and your hits land at once.
 * Keep-alives are never held, so you won't time out. Only YOUR packets are delayed, nothing is flooded.
 *
 * Limits: while held, the server still thinks you are at the old spot, so enemies CAN hit that spot,
 * and your hits only register when released.
 */
public final class LagSwitch extends Module {
    /** Auto-release after this many ticks (20 = 1 second). Long holds can get you set back by the server. */
    public static int maxTicks = 40;
    /** true = also hold attacks/swings/item use (burst), false = movement only (pure blink). */
    public static boolean holdActions = true;

    private static final ArrayDeque<Packet<?>> QUEUE = new ArrayDeque<>();
    private static volatile boolean active;
    private static boolean releasing;
    private int held;

    public LagSwitch() { super("lag_switch", "LagSwitch", Category.COMBAT); }

    @Override public boolean persist() { return false; }

    /** Called by ConnectionMixin for every outgoing packet. */
    public static boolean shouldHold(Packet<?> p) {
        if (!active || releasing) return false;
        if (p instanceof ServerboundMovePlayerPacket) return true;
        if (!holdActions) return false;
        return p instanceof ServerboundInteractPacket || p instanceof ServerboundSwingPacket
                || p instanceof ServerboundPlayerActionPacket || p instanceof ServerboundUseItemOnPacket
                || p instanceof ServerboundUseItemPacket || p instanceof ServerboundSetCarriedItemPacket
                || p instanceof ServerboundPlayerCommandPacket;
    }

    public static synchronized void hold(Packet<?> packet) { QUEUE.add(packet); }

    @Override protected void onEnable(Minecraft mc) { held = 0; active = true; }

    @Override protected void onDisable(Minecraft mc) {
        active = false;
        flush(mc);
    }

    private static synchronized void flush(Minecraft mc) {
        var conn = mc.getConnection();
        releasing = true;
        try {
            while (!QUEUE.isEmpty()) {
                Packet<?> p = QUEUE.poll();
                if (conn != null) conn.send(p);
            }
        } finally { releasing = false; }
    }

    @Override public void onTick(Minecraft mc) {
        if (++held >= maxTicks) {
            setEnabled(false); // releases the packets
            ImperialClient.notify(mc, "LagSwitch released");
        }
    }

    @Override public void onHud(GuiGraphics g, Minecraft mc, float delta) {
        String s = String.format(Locale.ROOT, "LAG  %.1fs  (%d packets held)", held / 20.0, QUEUE.size());
        g.drawString(mc.font, s, mc.getWindow().getGuiScaledWidth() / 2 - mc.font.width(s) / 2, 40, 0xFFFF5555, true);
    }
}
