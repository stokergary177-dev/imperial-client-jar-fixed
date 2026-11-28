package com.imperial.client;

import com.imperial.client.modules.*;
import com.imperial.client.modules.Module;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class ImperialClient implements ClientModInitializer {
    public static final String MOD_NAME = "IMPERIAL CLIENT";
    public static final Projector PROJECTOR = new Projector();
    public static final List<Module> MODULES = new ArrayList<>();
    private static final Map<Module, KeyMapping> KEYS = new LinkedHashMap<>();
    private static KeyMapping openMenu;
    private static Path configFile;
    private static boolean openSettings;

    @Override
    public void onInitializeClient() {
        MODULES.addAll(List.of(
                new Zoom(), new Fullbright(), new XRay(), new StorageEsp(), new BaseFinder(), new PlayerEsp(), new Tracers(),
                new Flight(), new NoFallMp(), new SimpleModules.AutoSprint(), new SimpleModules.AutoJump(),
                new AutoTotem(), new SimpleModules.TriggerBot(), new SimpleModules.KillAura(), new MaceSmash(), new LagSwitch(),
                new AutoEat(), new SimpleModules.Coordinates(), new SimpleModules.Fps(),
                // v1.5.0
                new Nametags(), new ItemEsp(), new ThreatRadar(), new TrailTracker(),
                new StashScore(), new Waypoints(),
                new MoveModules.Speed(), new MoveModules.Step(), new MoveModules.Spider(), new MoveModules.Jesus(),
                new MoveModules.Glide(), new MoveModules.HighJump(), new MoveModules.AutoWalk(), new MoveModules.ElytraFly(),
                new ElytraNav(), new MoveModules.Scaffold(),
                new CombatModules.Criticals(), new CombatModules.AutoArmor(), new CombatModules.AutoGapple(), new CombatModules.Panic(),
                new Velocity(), new Hitboxes(),
                new UtilModules.AntiAFK(), new UtilModules.AutoRespawn(), new UtilModules.ChestStealer(), new UtilModules.AutoTool()));

        openMenu = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.imperial_client.open_menu",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, KeyMapping.Category.MISC));
        for (Module m : MODULES)
            KEYS.put(m, KeyBindingHelper.registerKeyBinding(new KeyMapping("key.imperial_client." + m.id(),
                    InputConstants.Type.KEYSYM, m.defaultKey(), KeyMapping.Category.MISC)));
        MaceSmash.register();
        CombatModules.Criticals.register();
        Waypoints.register();
        TrailTracker.register();
        ElytraNav.register();
        Clip.register();
        loadConfig();

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openMenu.consumeClick()) {
                if (mc.screen == null) mc.setScreen(new ImperialScreen());
                else if (mc.screen instanceof ImperialScreen) mc.setScreen(null);
            }
            for (Map.Entry<Module, KeyMapping> e : KEYS.entrySet())
                while (e.getValue().consumeClick()) { if (mc.screen == null) toggle(mc, e.getKey()); }
            if (openSettings && mc.screen == null) { openSettings = false; mc.setScreen(new SettingsScreen(null)); }
            if (mc.player == null || mc.level == null) { LagSwitch.onDisconnect(); return; }
            for (Module m : MODULES) if (m.isEnabled()) m.onTick(mc);
        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("imperial_client", "overlay"), (g, delta) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null || mc.options.hideGui) return;
            float pt = delta.getGameTimeDeltaPartialTick(false);
            PROJECTOR.update(mc, pt);
            List<String> names = new ArrayList<>();
            for (Module m : MODULES) {
                if (!m.isEnabled()) continue;
                m.onHud(g, mc, pt);
                if (m.category() != Module.Category.PLAYER) names.add(m.name());
            }
            if (Settings.showModuleList) {
                names.sort((a, b) -> mc.font.width(b) - mc.font.width(a)); // longest first
                int y = 30;
                for (String n : names) {
                    g.drawString(mc.font, n, 4, y, 0xFFB57CFF, true);
                    y += 10;
                }
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
                dispatcher.register(ClientCommandManager.literal("imperial")
                        .then(ClientCommandManager.literal("list").executes(ctx -> {
                            for (Module m : MODULES)
                                ctx.getSource().sendFeedback(Component.literal("[IMPERIAL] " + m.id() + " : " + (m.isEnabled() ? "ON" : "OFF")));
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("settings").executes(ctx -> { openSettings = true; return 1; }))
                        .then(ClientCommandManager.literal("friend")
                                .then(ClientCommandManager.literal("list").executes(ctx -> {
                                    ctx.getSource().sendFeedback(Component.literal("[IMPERIAL] friends (" + Friends.names().size() + "): "
                                            + (Friends.names().isEmpty() ? "none" : String.join(", ", Friends.names()))));
                                    return 1;
                                }))
                                .then(ClientCommandManager.literal("add")
                                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                                .suggests((c, b) -> {
                                                    Minecraft mc = Minecraft.getInstance();
                                                    if (mc.level != null) for (var pl : mc.level.players()) b.suggest(pl.getName().getString());
                                                    return b.buildFuture();
                                                })
                                                .executes(ctx -> {
                                                    String n = StringArgumentType.getString(ctx, "name");
                                                    boolean added = Friends.add(n);
                                                    saveConfig();
                                                    ctx.getSource().sendFeedback(Component.literal("[IMPERIAL] " + n + (added ? " added to friends" : " is already a friend")));
                                                    return 1;
                                                })))
                                .then(ClientCommandManager.literal("remove")
                                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                                .suggests((c, b) -> { for (String n : Friends.names()) b.suggest(n); return b.buildFuture(); })
                                                .executes(ctx -> {
                                                    String n = StringArgumentType.getString(ctx, "name");
                                                    boolean removed = Friends.remove(n);
                                                    saveConfig();
                                                    ctx.getSource().sendFeedback(Component.literal("[IMPERIAL] " + n + (removed ? " removed from friends" : " was not a friend")));
                                                    return 1;
                                                })))
                                .then(ClientCommandManager.literal("protect").executes(ctx -> {
                                    Friends.protect = !Friends.protect;
                                    saveConfig();
                                    ctx.getSource().sendFeedback(Component.literal("[IMPERIAL] friend protection " + (Friends.protect ? "ON" : "OFF")));
                                    return 1;
                                })))
                        .then(ClientCommandManager.argument("module", StringArgumentType.word())
                                .suggests((c, b) -> { for (Module m : MODULES) b.suggest(m.id()); return b.buildFuture(); })
                                .executes(ctx -> {
                                    String id = StringArgumentType.getString(ctx, "module");
                                    for (Module m : MODULES)
                                        if (m.id().equalsIgnoreCase(id)) { toggle(Minecraft.getInstance(), m); return 1; }
                                    ctx.getSource().sendFeedback(Component.literal("[IMPERIAL] unknown module: " + id));
                                    return 0;
                                }))));
    }

    public static void toggle(Minecraft mc, Module m) {
        m.toggle();
        saveConfig();
        notify(mc, m.name() + " " + (m.isEnabled() ? "ON" : "OFF"));
    }

    public static void notify(Minecraft mc, String msg) {
        if (mc.player != null) mc.player.displayClientMessage(Component.literal("§5[IMPERIAL] §f" + msg), true);
    }

    private static void loadConfig() {
        configFile = FabricLoader.getInstance().getConfigDir().resolve("imperial_client.properties");
        if (!Files.exists(configFile)) return;
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(configFile)) { props.load(in); } catch (IOException e) { return; }
        for (Module m : MODULES) if (m.persist()) m.setEnabled(Boolean.parseBoolean(props.getProperty(m.id(), "false")));
        Settings.load(props);
        Friends.load(props);
        Waypoints.load(props);
    }

    public static void saveConfig() {
        if (configFile == null) return;
        Properties props = new Properties();
        for (Module m : MODULES) if (m.persist()) props.setProperty(m.id(), Boolean.toString(m.isEnabled()));
        Settings.save(props);
        Friends.save(props);
        Waypoints.save(props);
        try (OutputStream out = Files.newOutputStream(configFile)) { props.store(out, "IMPERIAL CLIENT"); } catch (IOException ignored) {}
    }
}
