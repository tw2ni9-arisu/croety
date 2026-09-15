package com.croety.client;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import com.croety.content.orb.SoulOrb;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class SoulOrbRenderer extends EntityRenderer<SoulOrb> {
   private static final ResourceLocation EXPERIENCE_ORB_LOCATION = new ResourceLocation("textures/entity/experience_orb.png");
   private static final RenderType RENDER_TYPE = RenderType.itemEntityTranslucentCull(EXPERIENCE_ORB_LOCATION);

   public SoulOrbRenderer(EntityRendererProvider.Context pContext) {
      super(pContext);
      this.shadowRadius = 0.15F;
      this.shadowStrength = 0.75F;
   }

   protected int getBlockLightLevel(SoulOrb pEntity, BlockPos pPos) {
      return Mth.clamp(super.getBlockLightLevel(pEntity, pPos) + 7, 0, 15);
   }

   public void render(SoulOrb pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
      pPoseStack.pushPose();
      int $$6 = pEntity.getIcon();
      float $$7 = (float)($$6 % 4 * 16 + 0) / 64.0F;
      float $$8 = (float)($$6 % 4 * 16 + 16) / 64.0F;
      float $$9 = (float)($$6 / 4 * 16 + 0) / 64.0F;
      float $$10 = (float)($$6 / 4 * 16 + 16) / 64.0F;
      float $$15 = ((float)pEntity.tickCount + pPartialTicks) / 2.0F;
      float blend = (Mth.sin($$15) + 1.0F) * .5F;
      int $$16 = 0x24;
      int $$17 = (int) Mth.lerp(blend, 0xEB, 0xC0);
      int $$18 = (int) Mth.lerp(blend, 0xB9, 0xEB);
      pPoseStack.translate(0.0F, 0.1F, 0.0F);
      pPoseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
      pPoseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
      pPoseStack.scale(0.3F, 0.3F, 0.3F);
      VertexConsumer $$20 = pBuffer.getBuffer(RENDER_TYPE);
      PoseStack.Pose $$21 = pPoseStack.last();
      Matrix4f $$22 = $$21.pose();
      Matrix3f $$23 = $$21.normal();
      vertex($$20, $$22, $$23, -0.5F, -0.25F, $$16, $$17, $$18, $$7, $$10, pPackedLight);
      vertex($$20, $$22, $$23, 0.5F, -0.25F, $$16, $$17, $$18, $$8, $$10, pPackedLight);
      vertex($$20, $$22, $$23, 0.5F, 0.75F, $$16, $$17, $$18, $$8, $$9, pPackedLight);
      vertex($$20, $$22, $$23, -0.5F, 0.75F, $$16, $$17, $$18, $$7, $$9, pPackedLight);
      pPoseStack.popPose();
      super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
   }

   private static void vertex(VertexConsumer pConsumer, Matrix4f pMatrix, Matrix3f pMatrixNormal, float pX, float pY, int pRed, int pGreen, int pBlue, float pTexU, float pTexV, int pPackedLight) {
      pConsumer.vertex(pMatrix, pX, pY, 0.0F).color(pRed, pGreen, pBlue, 128).uv(pTexU, pTexV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(pPackedLight).normal(pMatrixNormal, 0.0F, 1.0F, 0.0F).endVertex();
   }

   public ResourceLocation getTextureLocation(SoulOrb pEntity) {
      return EXPERIENCE_ORB_LOCATION;
   }
}
