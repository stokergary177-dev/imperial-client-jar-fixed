package com.imperial.client.modules;

import com.imperial.client.Friends;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

import java.util.Locale;

/** Small modules grouped together. */
public final class SimpleModules {
    private SimpleModules() {}

    public static final class AutoSprint extends Module {
        public AutoSprint() { super("auto_sprint", "AutoSprint", Category.MOVEMENT); }
        @Override public void onTick(Minecraft mc) {
            var p = mc.player;
            if (mc.options.keyUp.isDown() && !p.horizontalCollision && !p.isShiftKeyDown()
                    && p.getFoodData().getFoodLevel() > 6) p.setSprinting(true);
        }
    }

    public static final class AutoJump extends Module {
        public AutoJump() { super("auto_jump", "AutoJump", Category.MOVEMENT); }
        @Override public void onTick(Minecraft mc) {
            var p = mc.player;
            if (mc.options.keyUp.isDown() && p.onGround() && mc.screen == null) p.jumpFromGround();
        }
    }

    public static final class TriggerBot extends Module {
        public TriggerBot() { super("trigger_bot", "TriggerBot", Category.COMBAT); }
        @Override public boolean persist() { return false; }
        @Override public void onTick(Minecraft mc) {
            if (mc.screen != null || mc.gameMode == null) return;
            if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity target
                    && target.isAlive() && !target.isSpectator() && (target instanceof Player || target instanceof Enemy)
                    && !Friends.isFriend(target) && mc.player.getAttackStrengthScale(0.5f) >= 0.95f) {
                mc.gameMode.attack(mc.player, target);
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
        }
    }

    /** Attacks the nearest player/hostile mob in reach. */
    public static final class KillAura extends Module {
        public static double range = 3.8;
        public static boolean playersOnly = false;
        /** 0 = nearest, 1 = lowest health, 2 = most dangerous (best gear). */
        public static int priority = 0;
        public KillAura() { super("kill_aura", "KillAura", Category.COMBAT); }
        @Override public boolean persist() { return false; }
        @Override public void onTick(Minecraft mc) {
            if (mc.screen != null || mc.gameMode == null) return;
            var p = mc.player;
            if (p.getAttackStrengthScale(0.5f) < 0.95f) return;
            LivingEntity best = null;
            double bestKey = -Double.MAX_VALUE;
            double maxD = range * range;
            for (var e : mc.level.entitiesForRendering()) {
                if (!(e instanceof LivingEntity le) || e == p || !le.isAlive() || le.isSpectator() || Friends.isFriend(e)) continue;
                boolean ok = e instanceof Player || (!playersOnly && e instanceof Enemy);
                if (!ok) continue;
                double d = p.distanceToSqr(e);
                if (d > maxD) continue;
                double key;
                if (priority == 1) key = -(le.getHealth() + le.getAbsorptionAmount());
                else if (priority == 2) key = (e instanceof Player pl ? Gear.threat(pl) : 0) * 1000.0 - d;
                else key = -d;
                if (key > bestKey) { bestKey = key; best = le; }
            }
            if (best != null) {
                mc.gameMode.attack(p, best);
                p.swing(InteractionHand.MAIN_HAND);
            }
        }
    }

    public static final class Coordinates extends Module {
        public Coordinates() { super("coordinates", "Coordinates", Category.PLAYER); }
        @Override public void onHud(GuiGraphics g, Minecraft mc, float delta) {
            var p = mc.player;
            g.drawString(mc.font, String.format(Locale.ROOT, "XYZ  %.1f  %.1f  %.1f", p.getX(), p.getY(), p.getZ()), 4, 4, 0xFFFFFFFF, true);
        }
    }

    public static final class Fps extends Module {
        public Fps() { super("fps", "FPS", Category.PLAYER); }
        @Override public void onHud(GuiGraphics g, Minecraft mc, float delta) {
            g.drawString(mc.font, mc.getFps() + " FPS", 4, 15, 0xFFFFFFFF, true);
        }
    }
}
