package com.imperial.client;

import com.imperial.client.modules.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/** Every tweakable value, shown in the Settings screen and saved to the config. Add new ones at the bottom. */
public final class Settings {
    private Settings() {}

    public static boolean showModuleList = true;

    public static final class Setting {
        public final String key, label;
        public final double min, max, step;
        public final boolean toggle;
        public final String[] names; // optional: label for each whole number (for "mode" settings)
        public final DoubleSupplier get;
        public final DoubleConsumer set;

        Setting(String key, String label, double min, double max, double step, boolean toggle, String[] names, DoubleSupplier get, DoubleConsumer set) {
            this.key = key; this.label = label; this.min = min; this.max = max; this.step = step;
            this.toggle = toggle; this.names = names; this.get = get; this.set = set;
        }

        public String format() {
            double v = get.getAsDouble();
            if (toggle) return v > 0.5 ? "ON" : "OFF";
            if (names != null) return names[(int) Math.max(0, Math.min(names.length - 1, Math.round(v)))];
            if (step >= 1) return Integer.toString((int) Math.round(v));
            return String.format(Locale.ROOT, step >= 0.1 ? "%.1f" : "%.2f", v);
        }

        public void setClamped(double v) { set.accept(Math.max(min, Math.min(max, v))); }
    }

    public static final List<Setting> ALL = new ArrayList<>();

    private static void num(String key, String label, double min, double max, double step, DoubleSupplier g, DoubleConsumer s) {
        ALL.add(new Setting(key, label, min, max, step, false, null, g, s));
    }
    private static void mode(String key, String label, String[] names, DoubleSupplier g, DoubleConsumer s) {
        ALL.add(new Setting(key, label, 0, names.length - 1, 1, false, names, g, s));
    }
    private static void flag(String key, String label, DoubleSupplier g, DoubleConsumer s) {
        ALL.add(new Setting(key, label, 0, 1, 1, true, null, g, s));
    }

    static {
        // combat
        num("kill_aura.range", "KillAura range", 2.5, 6.0, 0.1, () -> SimpleModules.KillAura.range, v -> SimpleModules.KillAura.range = v);
        mode("kill_aura.priority", "KillAura target", new String[]{"Nearest", "Lowest health", "Most dangerous"},
                () -> SimpleModules.KillAura.priority, v -> SimpleModules.KillAura.priority = (int) Math.round(v));
        flag("kill_aura.players_only", "KillAura players only", () -> SimpleModules.KillAura.playersOnly ? 1 : 0, v -> SimpleModules.KillAura.playersOnly = v > 0.5);
        num("mace_smash.height", "MaceSmash height", 4, 22, 1, () -> MaceSmash.maxHeight, v -> MaceSmash.maxHeight = (int) Math.round(v));
        num("lag_switch.seconds", "LagSwitch max hold (s)", 0.5, 4.0, 0.1, () -> LagSwitch.maxTicks / 20.0, v -> LagSwitch.maxTicks = (int) Math.round(v * 20));
        flag("lag_switch.hold_actions", "LagSwitch holds attacks", () -> LagSwitch.holdActions ? 1 : 0, v -> LagSwitch.holdActions = v > 0.5);
        num("hitboxes.expand", "Hitbox size", 0.0, 0.6, 0.05, () -> Hitboxes.expand, v -> Hitboxes.expand = v);
        num("auto_gapple.health", "AutoGapple below (hp)", 4, 19, 1, () -> CombatModules.AutoGapple.healthBelow, v -> CombatModules.AutoGapple.healthBelow = v);
        num("panic.health", "Panic logout at (hp)", 1, 10, 1, () -> CombatModules.Panic.logoutHealth, v -> CombatModules.Panic.logoutHealth = v);
        // movement
        num("speed.multiplier", "Speed", 1.0, 3.0, 0.1, () -> MoveModules.Speed.multiplier, v -> MoveModules.Speed.multiplier = v);
        num("step.height", "Step height", 1.0, 2.5, 0.5, () -> MoveModules.Step.height, v -> MoveModules.Step.height = v);
        num("high_jump.power", "HighJump power", 0.5, 1.5, 0.1, () -> MoveModules.HighJump.power, v -> MoveModules.HighJump.power = v);
        num("elytra_fly.speed", "ElytraFly speed", 0.5, 3.0, 0.1, () -> MoveModules.ElytraFly.speed, v -> MoveModules.ElytraFly.speed = v);
        num("elytra_nav.cruise", "ElytraNav cruise above target", 10, 80, 5, () -> ElytraNav.cruise, v -> ElytraNav.cruise = v);
        // world / render / player
        num("threat_radar.range", "ThreatRadar range", 32, 256, 16, () -> ThreatRadar.range, v -> ThreatRadar.range = v);
        num("stash_score.min", "StashScore min boxes", 2, 30, 1, () -> StashScore.minContainers, v -> StashScore.minContainers = (int) Math.round(v));
        num("chest_stealer.delay", "ChestStealer delay (ticks)", 1, 10, 1, () -> UtilModules.ChestStealer.delay, v -> UtilModules.ChestStealer.delay = (int) Math.round(v));
        // general
        flag("friends.protect", "Protect friends", () -> Friends.protect ? 1 : 0, v -> Friends.protect = v > 0.5);
        flag("hud.module_list", "Show module list", () -> showModuleList ? 1 : 0, v -> showModuleList = v > 0.5);
    }

    static void load(Properties props) {
        for (Setting s : ALL) {
            String raw = props.getProperty("setting." + s.key);
            if (raw == null) continue;
            try { s.setClamped(Double.parseDouble(raw)); } catch (NumberFormatException ignored) {}
        }
    }

    static void save(Properties props) {
        for (Setting s : ALL) props.setProperty("setting." + s.key, Double.toString(s.get.getAsDouble()));
    }
}
