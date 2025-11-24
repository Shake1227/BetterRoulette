package shake1227.betterroulette.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import shake1227.betterroulette.BetterRoulette;
import shake1227.betterroulette.client.renderer.util.RenderUtil;
import shake1227.betterroulette.common.data.RouletteEntry;
import shake1227.betterroulette.common.entity.RouletteEntity;

import java.util.List;

public class RouletteRenderer extends EntityRenderer<RouletteEntity> {
    private static final ResourceLocation DUMMY_TEXTURE = ResourceLocation.fromNamespaceAndPath(BetterRoulette.MOD_ID, "textures/entity/roulette.png");

    public RouletteRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(RouletteEntity entity) {
        return DUMMY_TEXTURE;
    }

    @Override
    public void render(RouletteEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));

        float renderRotation = entity.getRenderRotation();
        poseStack.mulPose(Axis.YP.rotationDegrees(-renderRotation));

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity)));

        List<RouletteEntry> entries = entity.getEntries();
        if (!entries.isEmpty()) {
            float angleStep = 360.0f / entries.size();
            for (int i = 0; i < entries.size(); i++) {
                RouletteEntry entry = entries.get(i);
                int color = entry.getColor();
                float r = ((color >> 16) & 0xFF) / 255.0f;
                float g = ((color >> 8) & 0xFF) / 255.0f;
                float b = (color & 0xFF) / 255.0f;

                float startAngle = i * angleStep;
                float endAngle = (i + 1) * angleStep;

                RenderUtil.drawArc(poseStack, vertexConsumer, startAngle, endAngle, 0.95f, 32, r, g, b, 0.9f, packedLight);
            }
        }

        RenderUtil.drawDisk(poseStack, vertexConsumer, 1.0f, 64, 0.1f, 0.1f, 0.1f, 0.8f, packedLight);
        RenderUtil.drawDisk(poseStack, vertexConsumer, 0.2f, 32, 0.2f, 0.2f, 0.2f, 0.9f, packedLight);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(270));

        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer lineBuffer = buffer.getBuffer(RenderType.lines());

        lineBuffer.vertex(matrix, 0.8f, 0.02f, 0).color(1f, 0f, 0f, 1f).normal(0,1,0).endVertex();
        lineBuffer.vertex(matrix, 1.1f, 0.02f, 0).color(1f, 0f, 0f, 1f).normal(0,1,0).endVertex();

        VertexConsumer triBuffer = buffer.getBuffer(RenderType.entitySolid(getTextureLocation(entity)));
        triBuffer.vertex(matrix, 1.1f, 0.02f, -0.05f).color(1f,0,0,1f).uv(0,0).uv2(packedLight).normal(0,1,0).endVertex();
        triBuffer.vertex(matrix, 1.1f, 0.02f, 0.05f).color(1f,0,0,1f).uv(0,0).uv2(packedLight).normal(0,1,0).endVertex();
        triBuffer.vertex(matrix, 1.2f, 0.02f, 0f).color(1f,0,0,1f).uv(0,0).uv2(packedLight).normal(0,1,0).endVertex();

        poseStack.popPose();

        poseStack.popPose();
    }
}