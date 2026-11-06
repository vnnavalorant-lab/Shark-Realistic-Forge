package com.sharkmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sharkmod.entity.SharkEntity;
import com.sharkmod.model.SharkModelData;
import com.sharkmod.model.SharkSkinner;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Renderer próprio: o modelo vem do FBX original (malha com esqueleto de 31 ossos), então a malha é
 * deformada por CPU a cada frame e desenhada triângulo a triângulo (cada triângulo vira um quad degenerado,
 * porque os RenderTypes de entidade usam GL_QUADS).
 */
public class SharkRenderer extends EntityRenderer<SharkEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("sharkmod", "textures/entity/shark.png");

    /** 1 unidade do modelo ≈ 1 cm. 0.008 => tubarão adulto com ~3,7 blocos de comprimento. */
    private static final float MODEL_SCALE = 0.008f;

    private final float sizeFactor;
    private SharkModelData data;
    private SharkSkinner skinner;
    private float[] pose;

    public SharkRenderer(EntityRendererProvider.Context ctx, float sizeFactor) {
        super(ctx);
        this.sizeFactor = sizeFactor;
        this.shadowRadius = 0.7f * sizeFactor;
    }

    private void ensureLoaded() {
        if (data == null) {
            data = SharkModelData.get();
            skinner = new SharkSkinner(data);
            pose = skinner.newPose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(SharkEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(SharkEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int light) {
        ensureLoaded();

        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        // pose do esqueleto (swim / circle / bite) + skinning
        entity.animator.evaluate(data, skinner, partialTick, pose);
        skinner.solve(pose);
        skinner.skinMesh();

        poseStack.pushPose();
        poseStack.translate(0.0, entity.getBbHeight() * 0.5, 0.0);
        // o modelo olha para +Z (sul) com yaw 0
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        if (entity.deathTime > 0) {
            float f = ((float) entity.deathTime + partialTick - 1.0f) / 20.0f * 1.6f;
            f = Mth.sqrt(f);
            if (f > 1.0f) f = 1.0f;
            poseStack.mulPose(Axis.ZP.rotationDegrees(f * 180.0f)); // vira de barriga para cima
        }
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        float s = MODEL_SCALE * sizeFactor;
        poseStack.scale(s, s, s);
        poseStack.translate(-data.center[0], -data.center[1], -data.center[2]);

        PoseStack.Pose entry = poseStack.last();
        Matrix4f posMatrix = entry.pose();
        Matrix3f normalMatrix = entry.normal();
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0f);

        int[] idx = data.indices;
        for (int t = 0; t < data.triangleCount; t++) {
            int a = idx[t * 3], b = idx[t * 3 + 1], c = idx[t * 3 + 2];
            vertex(vc, posMatrix, normalMatrix, a, overlay, light);
            vertex(vc, posMatrix, normalMatrix, b, overlay, light);
            vertex(vc, posMatrix, normalMatrix, c, overlay, light);
            vertex(vc, posMatrix, normalMatrix, c, overlay, light); // 4º vértice repetido (quad degenerado)
        }

        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, light); // nome (name tag)
    }

    private void vertex(VertexConsumer vc, Matrix4f pm, Matrix3f nm, int i, int overlay, int light) {
        float[] p = skinner.outPos;
        float[] n = skinner.outNormal;
        vc.vertex(pm, p[i * 3], p[i * 3 + 1], p[i * 3 + 2])
                .color(255, 255, 255, 255)
                .uv(data.uv[i * 2], data.uv[i * 2 + 1])
                .overlayCoords(overlay)
                .uv2(light)
                .normal(nm, n[i * 3], n[i * 3 + 1], n[i * 3 + 2])
                .endVertex();
    }
}
