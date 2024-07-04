package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.BreezeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.BreezeEyesLayer;
import net.minecraft.client.renderer.entity.layers.BreezeWindLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BreezeRenderer extends MobRenderer<Breeze, BreezeModel<Breeze>> {
    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/breeze/breeze.png");

    public BreezeRenderer(EntityRendererProvider.Context p_312679_) {
        super(p_312679_, new BreezeModel<>(p_312679_.bakeLayer(ModelLayers.BREEZE)), 0.5F);
        this.addLayer(new BreezeWindLayer(p_312679_, this));
        this.addLayer(new BreezeEyesLayer(this));
    }

    public void render(Breeze pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
        BreezeModel<Breeze> breezemodel = this.getModel();
        enable(breezemodel, breezemodel.head(), breezemodel.rods());
        super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
    }

    /**
     * Returns the location of an entity's texture.
     */
    public ResourceLocation getTextureLocation(Breeze pEntity) {
        return TEXTURE_LOCATION;
    }

    public static BreezeModel<Breeze> enable(BreezeModel<Breeze> pModel, ModelPart... pParts) {
        pModel.head().visible = false;
        pModel.eyes().visible = false;
        pModel.rods().visible = false;
        pModel.wind().visible = false;

        for (ModelPart modelpart : pParts) {
            modelpart.visible = true;
        }

        return pModel;
    }
}
