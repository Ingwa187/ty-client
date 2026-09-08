package com.tyclient.module.render;

import com.tyclient.module.Category;
import com.tyclient.module.Module;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class EspModule extends Module {
    private static final int LINE_COLOR = 0xFFFF0000;
    private static final int BG = 0x901D1E21;

    public EspModule() {
        super("Esp", Category.RENDER);
    }

    public void render(GuiGraphicsExtractor graphics) {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Camera camera = mc.gameRenderer.mainCamera();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        Matrix4f vp = camera.getViewRotationProjectionMatrix(new Matrix4f());
        Vec3 cam = camera.position();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player) continue;
            if (!(entity instanceof Player) && !(entity instanceof Silverfish) && !(entity instanceof IronGolem)) continue;
            if (!entity.isAlive()) continue;

            float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            Vec3 offset = entity.getPosition(partial).subtract(entity.position());
            AABB bb = entity.getBoundingBox().move(offset);

            double minX = bb.minX, minY = bb.minY, minZ = bb.minZ;
            double maxX = bb.maxX, maxY = bb.maxY, maxZ = bb.maxZ;

            boolean behind = false;
            float xMin = Float.MAX_VALUE, xMax = -Float.MAX_VALUE;
            float yMin = Float.MAX_VALUE, yMax = -Float.MAX_VALUE;

            for (int i = 0; i < 8; i++) {
                double px = (i & 1) == 0 ? minX : maxX;
                double py = (i & 2) == 0 ? minY : maxY;
                double pz = (i & 4) == 0 ? minZ : maxZ;
                Vec3 rel = new Vec3(px - cam.x, py - cam.y, pz - cam.z);
                Vector4f clip = vp.transform(new Vector4f((float) rel.x, (float) rel.y, (float) rel.z, 1f));
                if (clip.w < 0.1f) {
                    behind = true;
                    break;
                }
                float ndcX = clip.x / clip.w;
                float ndcY = clip.y / clip.w;
                float sx = (ndcX * 0.5f + 0.5f) * screenW;
                float sy = (0.5f - ndcY * 0.5f) * screenH;
                if (sx < xMin) xMin = sx;
                if (sx > xMax) xMax = sx;
                if (sy < yMin) yMin = sy;
                if (sy > yMax) yMax = sy;
            }
            if (behind) continue;

            float x0 = xMin - 2;
            float y0 = yMin - 2;
            float x1 = xMax + 2;
            float y1 = yMax + 2;

            graphics.text(font(), Component.literal(entity.getName().getString()),
                    Math.round(x1 + 3), Math.round(y0), LINE_COLOR, true);
            drawOutline(graphics, Math.round(x0), Math.round(y0), Math.round(x1), Math.round(y1));
        }
    }

    private void drawOutline(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1) {
        graphics.fill(x0, y0, x1, y0 + 1, LINE_COLOR);
        graphics.fill(x0, y1 - 1, x1, y1, LINE_COLOR);
        graphics.fill(x0, y0, x0 + 1, y1, LINE_COLOR);
        graphics.fill(x1 - 1, y0, x1, y1, LINE_COLOR);
    }

    private Font font() {
        return Minecraft.getInstance().font;
    }
}
