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

    @Override
    public void onInitializeClient() {
        MODULES.addAll(List.of(
                new Zoom(), new Fullbright(), new XRay(), new StorageEsp(), new BaseFinder(), new PlayerEsp(), new Tracers(),
                new Flight(), new NoFallMp(), new SimpleModules.AutoSprint(), new SimpleModules.AutoJump(),
                new AutoTotem(), new SimpleModules.TriggerBot(), new SimpleModules.KillAura(),
                new AutoEat(), new SimpleModules.Coordinates(), new SimpleModules.Fps()));

        openMenu = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.imperial_client.open_menu",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, KeyMapping.Category.MISC));
        for (Module m : MODULES)
            KEYS.put(m, KeyBindingHelper.registerKeyBinding(new KeyMapping("key.imperial_client." + m.id(),
                    InputConstants.Type.KEYSYM, m.defaultKey(), KeyMapping.Category.MISC)));
        loadConfig();

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openMenu.consumeClick()) {
                if (mc.screen == null) mc.setScreen(new ImperialScreen());
                else if (mc.screen instanceof ImperialScreen) mc.setScreen(null);
            }
            for (Map.Entry<Module, KeyMapping> e : KEYS.entrySet())
                while (e.getValue().consumeClick()) { if (mc.screen == null) toggle(mc, e.getKey()); }
            if (mc.player == null || mc.level == null) return;
            for (Module m : MODULES) if (m.isEnabled()) m.onTick(mc);
        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("imperial_client", "overlay"), (g, delta) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null || mc.options.hideGui) return;
            float pt = delta.getGameTimeDeltaPartialTick(false);
            PROJECTOR.update(mc, pt);
            int y = 30;
            for (Module m : MODULES) {
                if (!m.isEnabled()) continue;
                m.onHud(g, mc, pt);
                if (m.category() != Module.Category.PLAYER) {
                    g.drawString(mc.font, m.name(), 4, y, 0xFFB57CFF, true);
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
    }

    private static void saveConfig() {
        Properties props = new Properties();
        for (Module m : MODULES) if (m.persist()) props.setProperty(m.id(), Boolean.toString(m.isEnabled()));
        try (OutputStream out = Files.newOutputStream(configFile)) { props.store(out, "IMPERIAL CLIENT"); } catch (IOException ignored) {}
    }
}
