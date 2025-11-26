package shake1227.betterroulette.client.renderer.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class RenderUtil {

    // 円盤描画 (中央のハブ用など)
    public static void drawDisk(PoseStack poseStack, VertexConsumer buffer, float radius, int segments, int color, int packedLight, int packedOverlay, float zOffset) {
        Matrix4f matrix = poseStack.last().pose();
        float y = zOffset;

        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = 1.0f;

        float angleStep = 360.0f / segments;

        for (int i = 0; i < segments; i++) {
            float startDeg = i * angleStep;
            float endDeg = (i + 1) * angleStep;

            float rad1 = (float) Math.toRadians(startDeg);
            float rad2 = (float) Math.toRadians(endDeg);

            float x1 = radius * Mth.cos(rad1);
            float z1 = radius * Mth.sin(rad1);
            float x2 = radius * Mth.cos(rad2);
            float z2 = radius * Mth.sin(rad2);

            // 4頂点で閉じる (Quad対応)
            buffer.vertex(matrix, 0, y, 0).color(r, g, b, a).uv(0.5f, 0.5f).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 1, 0).endVertex();
            buffer.vertex(matrix, x1, y, z1).color(r, g, b, a).uv(0.5f, 0.5f).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 1, 0).endVertex();
            buffer.vertex(matrix, x2, y, z2).color(r, g, b, a).uv(0.5f, 0.5f).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 1, 0).endVertex();
            buffer.vertex(matrix, x2, y, z2).color(r, g, b, a).uv(0.5f, 0.5f).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 1, 0).endVertex();
        }
    }

    // リング（枠線）描画
    public static void drawRing(PoseStack poseStack, VertexConsumer buffer, float innerRadius, float outerRadius, int segments, int color, int packedLight, int packedOverlay, float zOffset) {
        Matrix4f matrix = poseStack.last().pose();
        float y = zOffset;

        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = 1.0f;

        float angleStep = 360.0f / segments;

        for (int i = 0; i < segments; i++) {
            float startDeg = i * angleStep;
            float endDeg = (i + 1) * angleStep;
            float rad1 = (float) Math.toRadians(startDeg);
            float rad2 = (float) Math.toRadians(endDeg);

            float x1_in = innerRadius * Mth.cos(rad1);
            float z1_in = innerRadius * Mth.sin(rad1);
            float x2_in = innerRadius * Mth.cos(rad2);
            float z2_in = innerRadius * Mth.sin(rad2);

            float x1_out = outerRadius * Mth.cos(rad1);
            float z1_out = outerRadius * Mth.sin(rad1);
            float x2_out = outerRadius * Mth.cos(rad2);
            float z2_out = outerRadius * Mth.sin(rad2);

            buffer.vertex(matrix, x1_in, y, z1_in).color(r, g, b, a).uv(0.5f, 0.5f).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 1, 0).endVertex();
            buffer.vertex(matrix, x1_out, y, z1_out).color(r, g, b, a).uv(0.5f, 0.5f).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 1, 0).endVertex();
            buffer.vertex(matrix, x2_out, y, z2_out).color(r, g, b, a).uv(0.5f, 0.5f).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 1, 0).endVertex();
            buffer.vertex(matrix, x2_in, y, z2_in).color(r, g, b, a).uv(0.5f, 0.5f).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 1, 0).endVertex();
        }
    }

    // 三角形描画（ポインター用）
    public static void drawTriangle(PoseStack poseStack, VertexConsumer buffer, float x, float z, float size, int color, int packedLight, int packedOverlay, float zOffset) {
        Matrix4f matrix = poseStack.last().pose();
        float y = zOffset;

        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = 1.0f;

        float halfSize = size / 2.0f;
        float x1 = x - halfSize; float z1 = z - size;
        float x2 = x + halfSize; float z2 = z - size;
        float x3 = x;            float z3 = z;

        buffer.vertex(matrix, x1, y, z1).color(r, g, b, a).uv(0.5f, 0.5f).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 1, 0).endVertex();
        buffer.vertex(matrix, x2, y, z2).color(r, g, b, a).uv(0.5f, 0.5f).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 1, 0).endVertex();
        buffer.vertex(matrix, x3, y, z3).color(r, g, b, a).uv(0.5f, 0.5f).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 1, 0).endVertex();
        buffer.vertex(matrix, x3, y, z3).color(r, g, b, a).uv(0.5f, 0.5f).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 1, 0).endVertex();
    }

    // GUI用: 色相環（カラーホイール）を描画
    public static void drawColorWheel(GuiGraphics guiGraphics, int x, int y, float radius) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(x, y, 0);

        // 【修正】マトリックスを取得して頂点座標に適用する
        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        // 中心点
        buffer.vertex(matrix, 0, 0, 0).color(255, 255, 255, 255).endVertex();

        int segments = 60;
        for (int i = 0; i <= segments; i++) {
            float fraction = (float) i / segments;
            float angle = fraction * (float) Math.PI * 2;

            float px = Mth.cos(angle) * radius;
            float py = Mth.sin(angle) * radius;

            int color = java.awt.Color.HSBtoRGB(fraction, 1.0f, 1.0f);
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;

            // 【修正】頂点にmatrixを渡す
            buffer.vertex(matrix, px, py, 0).color(r, g, b, 255).endVertex();
        }

        tesselator.end();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }
}