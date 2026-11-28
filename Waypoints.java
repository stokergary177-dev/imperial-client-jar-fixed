package com.imperial.client.modules;

import com.imperial.client.ImperialClient;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/** Saved locations drawn on screen. /waypoint add <name> | remove <name> | list. Saved in the config file. */
public final class Waypoints extends Module {
    public record Wp(String name, String dim, double x, double y, double z) {}

    private static final List<Wp> LIST = new ArrayList<>();
    private static Waypoints instance;

    public Waypoints() { super("waypoints", "Waypoints", Category.WORLD); instance = this; }

    /** "overworld", "nether" or "end" (anything else counts as its own tag). */
    public static String dimTag(Minecraft mc) {
        if (mc.level == null) return "overworld";
        String d = mc.level.dimension().toString();
        if (d.contains("the_nether")) return "nether";
        if (d.contains("the_end")) return "end";
        if (d.contains("overworld")) return "overworld";
        return d;
    }

    public static List<Wp> all() { return LIST; }

    public static void add(Minecraft mc, String name) {
        if (mc.player == null) return;
        String dim = dimTag(mc);
        LIST.removeIf(w -> w.name().equalsIgnoreCase(name) && w.dim().equals(dim));
        LIST.add(new Wp(name, dim, mc.player.getX(), mc.player.getY(), mc.player.getZ()));
        ImperialClient.saveConfig();
    }

    public static Wp find(Minecraft mc, String name) {
        String dim = dimTag(mc);
        for (Wp w : LIST) if (w.name().equalsIgnoreCase(name) && w.dim().equals(dim)) return w;
        return null;
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
                dispatcher.register(ClientCommandManager.literal("waypoint")
                        .then(ClientCommandManager.literal("add")
                                .then(ClientCommandManager.argument("name", StringArgumentType.word()).executes(ctx -> {
                                    Minecraft mc = Minecraft.getInstance();
                                    String n = StringArgumentType.getString(ctx, "name");
                                    add(mc, n);
                                    if (instance != null && !instance.isEnabled()) instance.setEnabled(true);
                                    ctx.getSource().sendFeedback(Component.literal(String.format(Locale.ROOT,
                                            "[IMPERIAL] waypoint %s saved at %.0f %.0f %.0f", n, mc.player.getX(), mc.player.getY(), mc.player.getZ())));
                                    return 1;
                                })))
                        .then(ClientCommandManager.literal("remove")
                                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                        .suggests((c, b) -> { for (Wp w : LIST) b.suggest(w.name()); return b.buildFuture(); })
                                        .executes(ctx -> {
                                            String n = StringArgumentType.getString(ctx, "name");
                                            boolean gone = LIST.removeIf(w -> w.name().equalsIgnoreCase(n));
                                            ImperialClient.saveConfig();
                                            ctx.getSource().sendFeedback(Component.literal("[IMPERIAL] " + (gone ? "removed " : "no waypoint named ") + n));
                                            return 1;
                                        })))
                        .then(ClientCommandManager.literal("list").executes(ctx -> {
                            if (LIST.isEmpty()) ctx.getSource().sendFeedback(Component.literal("[IMPERIAL] no waypoints"));
                            for (Wp w : LIST)
                                ctx.getSource().sendFeedback(Component.literal(String.format(Locale.ROOT,
                                        "[IMPERIAL] %s (%s)  %.0f %.0f %.0f", w.name(), w.dim(), w.x(), w.y(), w.z())));
                            return 1;
                        }))));
    }

    public static void load(Properties props) {
        LIST.clear();
        for (String entry : props.getProperty("waypoints", "").split(";")) {
            String[] f = entry.split("\\|");
            if (f.length != 5) continue;
            try { LIST.add(new Wp(f[0], f[1], Double.parseDouble(f[2]), Double.parseDouble(f[3]), Double.parseDouble(f[4]))); }
            catch (NumberFormatException ignored) {}
        }
    }

    public static void save(Properties props) {
        StringBuilder sb = new StringBuilder();
        for (Wp w : LIST) {
            if (sb.length() > 0) sb.append(';');
            sb.append(w.name()).append('|').append(w.dim()).append('|').append(w.x()).append('|').append(w.y()).append('|').append(w.z());
        }
        props.setProperty("waypoints", sb.toString());
    }

    @Override public void onHud(GuiGraphics g, Minecraft mc, float delta) {
        Projector p = ImperialClient.PROJECTOR;
        String dim = dimTag(mc);
        for (Wp w : LIST) {
            if (!w.dim().equals(dim)) continue;
            double[] s = p.project(w.x(), w.y() + 1, w.z());
            if (s == null) continue;
            int sx = (int) s[0], sy = (int) s[1];
            g.fill(sx - 2, sy - 2, sx + 2, sy + 2, 0xFF55FFFF);
            g.drawString(mc.font, String.format(Locale.ROOT, "%s  %.0fm", w.name(), p.distance(w.x(), w.y() + 1, w.z())), sx + 5, sy - 4, 0xFF55FFFF, true);
        }
    }
}
