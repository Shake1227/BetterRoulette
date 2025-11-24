package shake1227.betterroulette.client.renderer.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

public class RenderUtil {

    public static void drawDisk(PoseStack poseStack, VertexConsumer buffer, float radius, int segments, float r, float g, float b, float a, int packedLight) {
        drawArc(poseStack, buffer, 0, 360, radius, segments, r, g, b, a, packedLight);
    }

    public static void drawArc(PoseStack poseStack, VertexConsumer buffer, float startAngle, float endAngle, float radius, int segments, float r, float g, float b, float a, int packedLight) {
        Matrix4f matrix = poseStack.last().pose();
        float y = 0.01f;

        float totalAngle = endAngle - startAngle;
        int numSegments = Math.max(1, (int) (segments * (totalAngle / 360.0f)));
        float angleStep = (float) Math.toRadians(totalAngle / numSegments);
        float startAngleRad = (float) Math.toRadians(startAngle);

        for (int i = 0; i < numSegments; i++) {
            float angle1 = startAngleRad + i * angleStep;
            float angle2 = startAngleRad + (i + 1) * angleStep;

            float x1 = radius * (float) Math.cos(angle1);
            float z1 = radius * (float) Math.sin(angle1);
            float x2 = radius * (float) Math.cos(angle2);
            float z2 = radius * (float) Math.sin(angle2);

            buffer.vertex(matrix, x1, y, z1).color(r, g, b, a).uv(0, 0).uv2(packedLight).normal(0, 1, 0).endVertex();
            buffer.vertex(matrix, x2, y, z2).color(r, g, b, a).uv(0, 0).uv2(packedLight).normal(0, 1, 0).endVertex();
            buffer.vertex(matrix, 0, y, 0).color(r, g, b, a).uv(0, 0).uv2(packedLight).normal(0, 1, 0).endVertex();
        }
    }
}