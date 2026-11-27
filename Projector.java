package com.imperial.client.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Projects world positions to the GUI layer so overlays need no world-render API. First person only. */
public final class Projector {
    private double cx, cy, cz, rx, rz, ux, uy, uz, fx, fy, fz, focal;
    private int w, h;

    public void update(Minecraft mc, float delta) {
        LocalPlayer p = mc.player;
        Vec3 pos = p.getPosition(delta);
        cx = pos.x; cy = pos.y + (p.getEyeY() - p.getY()); cz = pos.z;
        double yaw = Math.toRadians(p.getViewYRot(delta)), pitch = Math.toRadians(p.getViewXRot(delta));
        double sy = Math.sin(yaw), cyw = Math.cos(yaw), sp = Math.sin(pitch), cp = Math.cos(pitch);
        fx = -sy * cp; fy = -sp; fz = cyw * cp;
        rx = -cyw; rz = -sy;
        ux = -sy * sp; uy = cp; uz = cyw * sp;
        w = mc.getWindow().getGuiScaledWidth();
        h = mc.getWindow().getGuiScaledHeight();
        focal = (h / 2.0) / Math.tan(Math.toRadians(mc.options.fov().get()) / 2.0);
    }

    public double[] project(double x, double y, double z) {
        double dx = x - cx, dy = y - cy, dz = z - cz;
        double depth = dx * fx + dy * fy + dz * fz;
        if (depth < 0.05) return null;
        double sx = dx * rx + dz * rz, sy = dx * ux + dy * uy + dz * uz;
        return new double[]{w / 2.0 + sx / depth * focal, h / 2.0 - sy / depth * focal};
    }

    public double distance(double x, double y, double z) {
        double dx = x - cx, dy = y - cy, dz = z - cz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public boolean drawBox(GuiGraphics g, AABB b, int argb, int fillArgb) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            double[] s = project((i & 1) == 0 ? b.minX : b.maxX, (i & 2) == 0 ? b.minY : b.maxY, (i & 4) == 0 ? b.minZ : b.maxZ);
            if (s == null) return false;
            minX = Math.min(minX, s[0]); maxX = Math.max(maxX, s[0]);
            minY = Math.min(minY, s[1]); maxY = Math.max(maxY, s[1]);
        }
        if (maxX < 0 || maxY < 0 || minX > w || minY > h) return false;
        int x1 = (int) Math.floor(minX), y1 = (int) Math.floor(minY), x2 = (int) Math.ceil(maxX), y2 = (int) Math.ceil(maxY);
        if (x2 - x1 < 2) x2 = x1 + 2;
        if (y2 - y1 < 2) y2 = y1 + 2;
        if ((fillArgb >>> 24) != 0) g.fill(x1, y1, x2, y2, fillArgb);
        g.fill(x1, y1, x2, y1 + 1, argb);
        g.fill(x1, y2 - 1, x2, y2, argb);
        g.fill(x1, y1, x1 + 1, y2, argb);
        g.fill(x2 - 1, y1, x2, y2, argb);
        return true;
    }

    public void drawTracer(GuiGraphics g, double x, double y, double z, int argb) {
        double[] s = project(x, y, z);
        if (s == null) return;
        double x0 = w / 2.0, y0 = h / 2.0, dx = s[0] - x0, dy = s[1] - y0;
        int steps = (int) Math.min(600, Math.max(Math.abs(dx), Math.abs(dy)));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : (double) i / steps;
            int px = (int) (x0 + dx * t), py = (int) (y0 + dy * t);
            g.fill(px, py, px + 1, py + 1, argb);
        }
    }

    public int width() { return w; }
    public int height() { return h; }
}
