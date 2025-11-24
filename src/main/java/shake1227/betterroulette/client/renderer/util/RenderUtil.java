package shake1227.betterroulette.client.renderer.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class RenderUtil {

    // 円盤描画 (中央のハブ用)
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

            // UVは(0.5, 0.5)で白テクスチャを使用
            buffer.vertex(matrix, 0, y, 0).color(r, g, b, a).uv(0.5f, 0.5f).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 1, 0).endVertex();
            buffer.vertex(matrix, x1, y, z1).color(r, g, b, a).uv(0.5f, 0.5f).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 1, 0).endVertex();
            buffer.vertex(matrix, x2, y, z2).color(r, g, b, a).uv(0.5f, 0.5f).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 1, 0).endVertex();
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
        // 逆三角形 (▼)
        float x1 = x - halfSize; float z1 = z - size; // 左上
        float x2 = x + halfSize; float z2 = z - size; // 右上
        float x3 = x;            float z3 = z;        // 下（先端）

        buffer.vertex(matrix, x1, y, z1).color(r, g, b, a).uv(0.5f, 0.5f).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 1, 0).endVertex();
        buffer.vertex(matrix, x2, y, z2).color(r, g, b, a).uv(0.5f, 0.5f).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 1, 0).endVertex();
        buffer.vertex(matrix, x3, y, z3).color(r, g, b, a).uv(0.5f, 0.5f).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 1, 0).endVertex();
    }
}