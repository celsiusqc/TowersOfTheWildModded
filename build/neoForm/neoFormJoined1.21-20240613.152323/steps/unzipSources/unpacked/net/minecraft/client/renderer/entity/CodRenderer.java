package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.CodModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Cod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CodRenderer extends MobRenderer<Cod, CodModel<Cod>> {
    private static final ResourceLocation COD_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/fish/cod.png");

    public CodRenderer(EntityRendererProvider.Context p_173954_) {
        super(p_173954_, new CodModel<>(p_173954_.bakeLayer(ModelLayers.COD)), 0.3F);
    }

    /**
     * Returns the location of an entity's texture.
     */
    public ResourceLocation getTextureLocation(Cod pEntity) {
        return COD_LOCATION;
    }

    protected void setupRotations(Cod pEntity, PoseStack pPoseStack, float pBob, float pYBodyRot, float pPartialTick, float pScale) {
        super.setupRotations(pEntity, pPoseStack, pBob, pYBodyRot, pPartialTick, pScale);
        float f = 4.3F * Mth.sin(0.6F * pBob);
        pPoseStack.mulPose(Axis.YP.rotationDegrees(f));
        if (!pEntity.isInWater()) {
            pPoseStack.translate(0.1F, 0.1F, -0.1F);
            pPoseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        }
    }
}
