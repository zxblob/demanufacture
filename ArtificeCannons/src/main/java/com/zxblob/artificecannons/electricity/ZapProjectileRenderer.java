package com.zxblob.artificecannons.electricity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.zxblob.artificecannons.ArtificeCannons;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** Four crossed thin quads, full bright, like the original bolt. */
public class ZapProjectileRenderer extends EntityRenderer<ZapProjectileEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ArtificeCannons.MOD_ID, "textures/entity/zap_projectile.png");

    public ZapProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ZapProjectileEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        VertexConsumer buffer = buffers.getBuffer(RenderType.entitySolid(TEXTURE));
        pose.pushPose();
        pose.translate(0.0F, 0.125F, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        pose.mulPose(Axis.XP.rotationDegrees(-entity.getViewXRot(partialTick)));
        final float h = 0.03125F, len = 0.15625F;
        int full = 15728880;
        for (int i = 0; i < 4; i++) {
            pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
            PoseStack.Pose last = pose.last();
            Matrix4f m = last.pose();
            vertex(buffer, last, m, -h, -h, -len, 0.0F, 0.0F, full);
            vertex(buffer, last, m, -h, -h, len, 0.3125F, 0.0F, full);
            vertex(buffer, last, m, h, h, len, 0.3125F, 0.0625F, full);
            vertex(buffer, last, m, h, h, -len, 0.0F, 0.0625F, full);
        }
        pose.popPose();
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, Matrix4f m, float x, float y, float z, float u, float v, int light) {
        buffer.addVertex(m, x, y, z).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setUv2(light & 65535, light >> 16 & 65535).setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(ZapProjectileEntity entity) {
        return TEXTURE;
    }
}
