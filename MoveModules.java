package com.imperial.client.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Movement modules. All are client-side velocity changes, so the server has to allow them (your anarchy server does). */
public final class MoveModules {
    private MoveModules() {}

    /** Horizontal movement vector from the WASD keys, relative to where you look. ZERO when no key is held. */
    static Vec3 input(Minecraft mc, double speed) {
        var p = mc.player;
        double f = (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0);
        double s = (mc.options.keyLeft.isDown() ? 1 : 0) - (mc.options.keyRight.isDown() ? 1 : 0);
        double yaw = Math.toRadians(p.getYRot());
        double vx = -Math.sin(yaw) * f + Math.cos(yaw) * s;
        double vz = Math.cos(yaw) * f + Math.sin(yaw) * s;
        double len = Math.sqrt(vx * vx + vz * vz);
        if (len < 1e-4) return Vec3.ZERO;
        return new Vec3(vx / len * speed, 0, vz / len * speed);
    }

    /** Faster ground and air movement. 1.0 = normal sprint speed. */
    public static final class Speed extends Module {
        public static double multiplier = 1.4;
        public Speed() { super("speed", "Speed", Category.MOVEMENT); }
        @Override public boolean persist() { return false; }
        @Override public void onTick(Minecraft mc) {
            var p = mc.player;
            if (p.isFallFlying() || p.isPassenger() || p.getAbilities().flying || p.isInWater() || p.isInLava()) return;
            Vec3 mv = input(mc, 0.28 * multiplier);
            if (mv.lengthSqr() == 0) return;
            Vec3 dm = p.getDeltaMovement();
            p.setDeltaMovement(new Vec3(mv.x, dm.y, mv.z));
        }
    }

    /** Walk up blocks without jumping. */
    public static final class Step extends Module {
        public static double height = 1.5;
        public Step() { super("step", "Step", Category.MOVEMENT); }
        @Override public boolean persist() { return false; }
        private static void apply(Minecraft mc, double v) {
            if (mc.player == null) return;
            var a = mc.player.getAttribute(Attributes.STEP_HEIGHT);
            if (a != null && a.getBaseValue() != v) a.setBaseValue(v);
        }
        @Override protected void onDisable(Minecraft mc) { apply(mc, 0.6); }
        @Override public void onTick(Minecraft mc) { apply(mc, height); }
    }

    /** Climb walls by walking into them. */
    public static final class Spider extends Module {
        public Spider() { super("spider", "Spider", Category.MOVEMENT); }
        @Override public boolean persist() { return false; }
        @Override public void onTick(Minecraft mc) {
            var p = mc.player;
            if (p.horizontalCollision && !p.isShiftKeyDown() && mc.options.keyUp.isDown()) {
                Vec3 dm = p.getDeltaMovement();
                p.setDeltaMovement(new Vec3(dm.x, 0.2, dm.z));
            }
        }
    }

    /** Stay on the surface of water and lava. Hold sneak to sink. */
    public static final class Jesus extends Module {
        public Jesus() { super("jesus", "Jesus", Category.MOVEMENT); }
        @Override public boolean persist() { return false; }
        @Override public void onTick(Minecraft mc) {
            var p = mc.player;
            if ((p.isInWater() || p.isInLava()) && !p.isShiftKeyDown() && !p.isPassenger()) {
                Vec3 dm = p.getDeltaMovement();
                p.setDeltaMovement(new Vec3(dm.x, Math.max(dm.y, 0.11), dm.z));
            }
        }
    }

    /** Slow fall: your fall speed is capped. */
    public static final class Glide extends Module {
        public Glide() { super("glide", "Glide", Category.MOVEMENT); }
        @Override public boolean persist() { return false; }
        @Override public void onTick(Minecraft mc) {
            var p = mc.player;
            Vec3 dm = p.getDeltaMovement();
            if (!p.onGround() && !p.isFallFlying() && !p.isInWater() && !p.isInLava() && dm.y < -0.12) {
                p.setDeltaMovement(new Vec3(dm.x, -0.12, dm.z));
                p.fallDistance = 0;
            }
        }
    }

    public static final class HighJump extends Module {
        public static double power = 0.8;
        public HighJump() { super("high_jump", "HighJump", Category.MOVEMENT); }
        @Override public boolean persist() { return false; }
        @Override public void onTick(Minecraft mc) {
            var p = mc.player;
            if (mc.screen == null && p.onGround() && mc.options.keyJump.isDown()) {
                Vec3 dm = p.getDeltaMovement();
                p.setDeltaMovement(new Vec3(dm.x, power, dm.z));
            }
        }
    }

    public static final class AutoWalk extends Module {
        public AutoWalk() { super("auto_walk", "AutoWalk", Category.MOVEMENT); }
        @Override public boolean persist() { return false; }
        @Override protected void onDisable(Minecraft mc) { mc.options.keyUp.setDown(false); }
        @Override public void onTick(Minecraft mc) { if (mc.screen == null) mc.options.keyUp.setDown(true); }
    }

    /** While gliding: W = fly where you look at a fixed speed, S = stop, Space = rise, Shift = dive. */
    public static final class ElytraFly extends Module {
        public static double speed = 1.5;
        public ElytraFly() { super("elytra_fly", "ElytraFly", Category.MOVEMENT); }
        @Override public boolean persist() { return false; }
        @Override public void onTick(Minecraft mc) {
            var p = mc.player;
            if (!p.isFallFlying()) return;
            Vec3 dm = p.getDeltaMovement();
            if (mc.options.keyUp.isDown()) p.setDeltaMovement(p.getLookAngle().scale(speed));
            else if (mc.options.keyDown.isDown()) p.setDeltaMovement(Vec3.ZERO);
            else if (mc.options.keyJump.isDown()) p.setDeltaMovement(new Vec3(dm.x, 0.6, dm.z));
            else if (mc.options.keyShift.isDown()) p.setDeltaMovement(new Vec3(dm.x, -0.6, dm.z));
        }
    }

    /** Places the block in your hand under your feet while you move, so you can bridge without stopping. */
    public static final class Scaffold extends Module {
        public Scaffold() { super("scaffold", "Scaffold", Category.MOVEMENT); }
        @Override public boolean persist() { return false; }
        @Override public void onTick(Minecraft mc) {
            var p = mc.player;
            if (mc.screen != null || mc.gameMode == null || p.isFallFlying()) return;
            if (!(p.getMainHandItem().getItem() instanceof BlockItem)) return;
            BlockPos target = BlockPos.containing(p.getX(), p.getY() - 0.5, p.getZ());
            if (!mc.level.getBlockState(target).canBeReplaced()) return;
            Direction[] order = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
            for (Direction d : order) {
                BlockPos n = target.relative(d);
                BlockState ns = mc.level.getBlockState(n);
                if (ns.isAir() || ns.canBeReplaced()) continue;
                Direction face = d.getOpposite();
                Vec3 loc = Vec3.atCenterOf(n).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
                mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, new BlockHitResult(loc, face, n, false));
                p.swing(InteractionHand.MAIN_HAND);
                return;
            }
        }
    }
}
