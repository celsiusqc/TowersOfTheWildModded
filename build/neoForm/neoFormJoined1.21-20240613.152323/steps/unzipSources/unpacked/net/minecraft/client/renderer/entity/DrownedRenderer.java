package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.DrownedModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.DrownedOuterLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DrownedRenderer extends AbstractZombieRenderer<Drowned, DrownedModel<Drowned>> {
    private static final ResourceLocation DROWNED_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/zombie/drowned.png");

    public DrownedRenderer(EntityRendererProvider.Context p_173964_) {
        super(
            p_173964_,
            new DrownedModel<>(p_173964_.bakeLayer(ModelLayers.DROWNED)),
            new DrownedModel<>(p_173964_.bakeLayer(ModelLayers.DROWNED_INNER_ARMOR)),
            new DrownedModel<>(p_173964_.bakeLayer(ModelLayers.DROWNED_OUTER_ARMOR))
        );
        this.addLayer(new DrownedOuterLayer<>(this, p_173964_.getModelSet()));
    }

    /**
     * Returns the location of an entity's texture.
     */
    @Override
    public ResourceLocation getTextureLocation(Zombie pEntity) {
        return DROWNED_LOCATION;
    }

    protected void setupRotations(Drowned pEntity, PoseStack pPoseStack, float pBob, float pYBodyRot, float pPartialTick, float pScale) {
        super.setupRotations(pEntity, pPoseStack, pBob, pYBodyRot, pPartialTick, pScale);
        float f = pEntity.getSwimAmount(pPartialTick);
        if (f > 0.0F) {
            float f1 = -10.0F - pEntity.getXRot();
            float f2 = Mth.lerp(f, 0.0F, f1);
            pPoseStack.rotateAround(Axis.XP.rotationDegrees(f2), 0.0F, pEntity.getBbHeight() / 2.0F / pScale, 0.0F);
        }
    }
}
