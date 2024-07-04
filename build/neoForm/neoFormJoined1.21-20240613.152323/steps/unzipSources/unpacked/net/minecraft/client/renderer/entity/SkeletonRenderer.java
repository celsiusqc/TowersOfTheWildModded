package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SkeletonRenderer<T extends AbstractSkeleton> extends HumanoidMobRenderer<T, SkeletonModel<T>> {
    private static final ResourceLocation SKELETON_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/skeleton/skeleton.png");

    public SkeletonRenderer(EntityRendererProvider.Context p_174380_) {
        this(p_174380_, ModelLayers.SKELETON, ModelLayers.SKELETON_INNER_ARMOR, ModelLayers.SKELETON_OUTER_ARMOR);
    }

    public SkeletonRenderer(EntityRendererProvider.Context pContext, ModelLayerLocation pSkeletonLayer, ModelLayerLocation pInnerModelLayer, ModelLayerLocation pOuterModelLayer) {
        this(pContext, pInnerModelLayer, pOuterModelLayer, new SkeletonModel<>(pContext.bakeLayer(pSkeletonLayer)));
    }

    public SkeletonRenderer(EntityRendererProvider.Context pContext, ModelLayerLocation pSkeletonLayer, ModelLayerLocation pInnerModelLayer, SkeletonModel<T> pModel) {
        super(pContext, pModel, 0.5F);
        this.addLayer(
            new HumanoidArmorLayer<>(
                this, new SkeletonModel(pContext.bakeLayer(pSkeletonLayer)), new SkeletonModel(pContext.bakeLayer(pInnerModelLayer)), pContext.getModelManager()
            )
        );
    }

    /**
     * Returns the location of an entity's texture.
     */
    public ResourceLocation getTextureLocation(T pEntity) {
        return SKELETON_LOCATION;
    }

    protected boolean isShaking(T pEntity) {
        return pEntity.isShaking();
    }
}
