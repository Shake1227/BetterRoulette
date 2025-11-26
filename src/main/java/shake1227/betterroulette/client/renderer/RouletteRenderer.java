package shake1227.betterroulette.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import shake1227.betterroulette.client.renderer.util.RenderUtil;
import shake1227.betterroulette.common.data.RouletteEntry;
import shake1227.betterroulette.common.entity.RouletteEntity;

import java.util.List;

public class RouletteRenderer extends EntityRenderer<RouletteEntity> {
    private static final ResourceLocation WHITE_CONCRETE_TEXTURE = new ResourceLocation("minecraft", "textures/block/white_concrete.png");

    public RouletteRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(RouletteEntity entity) {
        return WHITE_CONCRETE_TEXTURE;
    }

    @Override
    public void render(RouletteEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.translate(0, 1.25, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees(90));

        VertexConsumer solidBuffer = buffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));

        poseStack.pushPose();
        float renderRotation = entity.getRenderRotation();
        poseStack.mulPose(Axis.YP.rotationDegrees(-renderRotation));

        List<RouletteEntry> entries = entity.getEntries();
        float radius = 1.0f;

        if (!entries.isEmpty()) {
            if (entity.isMixMode()) {
                // Mixモード: 100分割されたスロットを描画
                List<Integer> mixIndices = entity.getMixModeIndices();
                float anglePerSlot = 360.0f / mixIndices.size();

                for (int i = 0; i < mixIndices.size(); i++) {
                    int entryIndex = mixIndices.get(i);
                    if (entryIndex >= 0 && entryIndex < entries.size()) {
                        RouletteEntry entry = entries.get(entryIndex);
                        int color = entry.getColor();
                        float r = ((color >> 16) & 0xFF) / 255.0f;
                        float g = ((color >> 8) & 0xFF) / 255.0f;
                        float b = (color & 0xFF) / 255.0f;

                        float startAngle = i * anglePerSlot;
                        float endAngle = (i + 1) * anglePerSlot;

                        drawSector(poseStack, solidBuffer, radius, startAngle, endAngle, r, g, b, packedLight);

                        // 【追加】境界線（各セルの開始位置に黒い線を引く）
                        drawRadialBorder(poseStack, solidBuffer, radius, startAngle, packedLight);
                    }
                }
            } else {
                // 通常モード
                int totalWeight = entries.stream().mapToInt(RouletteEntry::getWeight).sum();
                if (totalWeight <= 0) totalWeight = 1;

                float currentAngle = 0;
                for (RouletteEntry entry : entries) {
                    float angleSize = (float) entry.getWeight() / totalWeight * 360.0f;
                    int color = entry.getColor();
                    float r = ((color >> 16) & 0xFF) / 255.0f;
                    float g = ((color >> 8) & 0xFF) / 255.0f;
                    float b = (color & 0xFF) / 255.0f;

                    int segments = Math.max(2, (int)(angleSize / 5));
                    for (int j = 0; j < segments; j++) {
                        float sDeg = currentAngle + (angleSize * j / segments);
                        float eDeg = currentAngle + (angleSize * (j + 1) / segments);
                        drawSector(poseStack, solidBuffer, radius, sDeg, eDeg, r, g, b, packedLight);
                    }
                    currentAngle += angleSize;
                }
            }
        } else {
            RenderUtil.drawDisk(poseStack, solidBuffer, radius, 64, 0xCCCCCC, packedLight, OverlayTexture.NO_OVERLAY, 0);
        }

        // 外枠リング
        float ringWidth = 0.05f;
        RenderUtil.drawRing(poseStack, solidBuffer, radius, radius + ringWidth, 64, 0x000000, packedLight, OverlayTexture.NO_OVERLAY, -0.005f);

        poseStack.popPose();

        // ハブ
        float hubY = -0.01f;
        RenderUtil.drawDisk(poseStack, solidBuffer, 0.25f, 32, 0x000000, packedLight, OverlayTexture.NO_OVERLAY, hubY);
        RenderUtil.drawDisk(poseStack, solidBuffer, 0.22f, 32, 0xFFFFFF, packedLight, OverlayTexture.NO_OVERLAY, hubY - 0.001f);

        // ポインター
        poseStack.pushPose();
        float pointerZOffset = -0.05f;
        float pointerPosY = -1.05f - ringWidth;

        RenderUtil.drawTriangle(poseStack, solidBuffer, 0, pointerPosY, 0.35f, 0x000000, packedLight, OverlayTexture.NO_OVERLAY, pointerZOffset);
        RenderUtil.drawTriangle(poseStack, solidBuffer, 0, pointerPosY - 0.03f, 0.29f, 0xFFFFFF, packedLight, OverlayTexture.NO_OVERLAY, pointerZOffset - 0.001f);

        poseStack.popPose();
        poseStack.popPose();
    }

    private void drawSector(PoseStack poseStack, VertexConsumer buffer, float radius, float startDeg, float endDeg, float r, float g, float b, int packedLight) {
        float rad1 = (float) Math.toRadians(startDeg);
        float rad2 = (float) Math.toRadians(endDeg);

        float x1 = radius * Mth.cos(rad1);
        float z1 = radius * Mth.sin(rad1);
        float x2 = radius * Mth.cos(rad2);
        float z2 = radius * Mth.sin(rad2);

        org.joml.Matrix4f matrix = poseStack.last().pose();

        // 4頂点で閉じる
        buffer.vertex(matrix, 0, 0, 0).color(r, g, b, 1.0f).uv(0.5f, 0.5f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
        buffer.vertex(matrix, x1, 0, z1).color(r, g, b, 1.0f).uv(0.5f, 0.5f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
        buffer.vertex(matrix, x2, 0, z2).color(r, g, b, 1.0f).uv(0.5f, 0.5f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
        buffer.vertex(matrix, x2, 0, z2).color(r, g, b, 1.0f).uv(0.5f, 0.5f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
    }

    // 境界線（放射状の線）を描画するメソッド
    private void drawRadialBorder(PoseStack poseStack, VertexConsumer buffer, float radius, float angleDeg, int packedLight) {
        float rad = (float) Math.toRadians(angleDeg);
        float lineWidth = 0.008f; // 線の太さ

        // 半径方向のベクトル (cos, sin)
        float dx = Mth.cos(rad);
        float dz = Mth.sin(rad);

        // 法線ベクトル (-sin, cos)
        float nx = -dz;
        float nz = dx;

        // 中心点付近 (細く始める)
        float cX1 = nx * (lineWidth * 0.5f);
        float cZ1 = nz * (lineWidth * 0.5f);
        float cX2 = -nx * (lineWidth * 0.5f);
        float cZ2 = -nz * (lineWidth * 0.5f);

        // 外周点 (半径Rの位置)
        float edgeX = radius * dx;
        float edgeZ = radius * dz;

        float eX1 = edgeX + (nx * lineWidth * 0.5f);
        float eZ1 = edgeZ + (nz * lineWidth * 0.5f);
        float eX2 = edgeX - (nx * lineWidth * 0.5f);
        float eZ2 = edgeZ - (nz * lineWidth * 0.5f);

        org.joml.Matrix4f matrix = poseStack.last().pose();
        // 少し浮かせて描画 (-0.001f)
        float y = -0.001f;

        // 黒い長方形を描画
        buffer.vertex(matrix, cX1, y, cZ1).color(0, 0, 0, 255).uv(0.5f, 0.5f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
        buffer.vertex(matrix, eX1, y, eZ1).color(0, 0, 0, 255).uv(0.5f, 0.5f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
        buffer.vertex(matrix, eX2, y, eZ2).color(0, 0, 0, 255).uv(0.5f, 0.5f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
        buffer.vertex(matrix, cX2, y, cZ2).color(0, 0, 0, 255).uv(0.5f, 0.5f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
    }
}