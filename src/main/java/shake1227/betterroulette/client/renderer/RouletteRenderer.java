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
    // バニラの白コンクリートテクスチャ（キャンバスとして使用）
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

        // 1. エンティティの向き
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));

        // 2. 縦向きにする（立てる）
        poseStack.translate(0, 1.25, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees(90));

        // テクスチャあり、裏面描画ありのバッファを使用
        VertexConsumer solidBuffer = buffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));

        // --- 1. ルーレット盤 ---
        poseStack.pushPose();
        float renderRotation = entity.getRenderRotation();
        poseStack.mulPose(Axis.YP.rotationDegrees(-renderRotation));

        List<RouletteEntry> entries = entity.getEntries();

        // 【修正】円を一周(360度)細かく分割して描画する
        // これにより隙間なく塗りつぶします
        int segments = 64; // 円の滑らかさ
        float radius = 1.0f;
        float totalAngle = 360.0f;
        float angleStep = totalAngle / segments;

        // 中心点の頂点 (共通)
        float centerX = 0;
        float centerZ = 0;
        float y = 0.0f;

        for (int i = 0; i < segments; i++) {
            float startDeg = i * angleStep;
            float endDeg = (i + 1) * angleStep;

            // 現在の角度が「どのエントリー（項目）」に属するかを判定して色を決める
            int color = 0xCCCCCC; // デフォルト色（グレー）
            if (!entries.isEmpty()) {
                // 角度(0-360)をエントリー数で割ってインデックスを算出
                float entryAngleSize = 360.0f / entries.size();
                // 中間角度で判定
                float midAngle = startDeg + (angleStep / 2.0f);
                int entryIndex = (int) (midAngle / entryAngleSize);
                // インデックスが範囲外にならないよう調整
                entryIndex = Mth.clamp(entryIndex, 0, entries.size() - 1);

                color = entries.get(entryIndex).getColor();
            }

            // 色成分の抽出
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            float a = 1.0f;

            // 座標計算
            float rad1 = (float) Math.toRadians(startDeg);
            float rad2 = (float) Math.toRadians(endDeg);

            float x1 = radius * Mth.cos(rad1);
            float z1 = radius * Mth.sin(rad1);
            float x2 = radius * Mth.cos(rad2);
            float z2 = radius * Mth.sin(rad2);

            // 三角形を描画 (中心 -> 点1 -> 点2)
            // UVは(0.5, 0.5)固定で白テクスチャの中心を使用
            PoseStack.Pose pose = poseStack.last();
            org.joml.Matrix4f matrix = pose.pose();

            solidBuffer.vertex(matrix, centerX, y, centerZ).color(r, g, b, a).uv(0.5f, 0.5f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
            solidBuffer.vertex(matrix, x1, y, z1).color(r, g, b, a).uv(0.5f, 0.5f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
            solidBuffer.vertex(matrix, x2, y, z2).color(r, g, b, a).uv(0.5f, 0.5f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
        }

        poseStack.popPose();

        // --- 2. 中央のハブ ---
        // 盤面より少し手前(-0.01f)に描画
        float hubY = -0.01f;
        RenderUtil.drawDisk(poseStack, solidBuffer, 0.2f, 32, 0xFFFFFF, packedLight, OverlayTexture.NO_OVERLAY, hubY); // 白
        RenderUtil.drawDisk(poseStack, solidBuffer, 0.1f, 16, 0x333333, packedLight, OverlayTexture.NO_OVERLAY, hubY - 0.001f); // 穴（グレー）

        // --- 3. ポインター（一番手前） ---
        poseStack.pushPose();
        float pointerZOffset = -0.05f; // さらに手前
        float pointerPosY = -1.05f;    // 円の上部

        // 矢印本体（真っ黒）
        RenderUtil.drawTriangle(poseStack, solidBuffer, 0, pointerPosY, 0.2f, 0x000000, packedLight, OverlayTexture.NO_OVERLAY, pointerZOffset);
        poseStack.popPose();

        poseStack.popPose(); // 全体終了
    }
}