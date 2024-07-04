package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SkeletonClothingLayer<T extends Mob & RangedAttackMob, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private final SkeletonModel<T> layerModel;
    private final ResourceLocation clothesLocation;

    public SkeletonClothingLayer(RenderLayerParent<T, M> pRenderer, EntityModelSet pModels, ModelLayerLocation pModelLayerLocation, ResourceLocation pClothesLocation) {
        super(pRenderer);
        this.clothesLocation = pClothesLocation;
        this.layerModel = new SkeletonModel<>(pModels.bakeLayer(pModelLayerLocation));
    }

    public void render(
        PoseStack pPoseStack,
        MultiBufferSource pBuffer,
        int pPackedLight,
        T pLivingEntity,
        float pLimbSwing,
        float pLimbSwingAmount,
        float pPartialTick,
        float pAgeInTicks,
        float pNetHeadYaw,
        float pHeadPitch
    ) {
        coloredCutoutModelCopyLayerRender(
            this.getParentModel(),
            this.layerModel,
            this.clothesLocation,
            pPoseStack,
            pBuffer,
            pPackedLight,
            pLivingEntity,
            pLimbSwing,
            pLimbSwingAmount,
            pAgeInTicks,
            pNetHeadYaw,
            pHeadPitch,
            pPartialTick,
            -1
        );
    }
}
