package net.minecraft.client.renderer.entity;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemEntityRenderer extends EntityRenderer<ItemEntity> {
    private static final float ITEM_BUNDLE_OFFSET_SCALE = 0.15F;
    private static final float FLAT_ITEM_BUNDLE_OFFSET_X = 0.0F;
    private static final float FLAT_ITEM_BUNDLE_OFFSET_Y = 0.0F;
    private static final float FLAT_ITEM_BUNDLE_OFFSET_Z = 0.09375F;
    private final ItemRenderer itemRenderer;
    private final RandomSource random = RandomSource.create();

    public ItemEntityRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
        this.itemRenderer = pContext.getItemRenderer();
        this.shadowRadius = 0.15F;
        this.shadowStrength = 0.75F;
    }

    /**
     * Returns the location of an entity's texture.
     */
    public ResourceLocation getTextureLocation(ItemEntity pEntity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    public void render(ItemEntity pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
        pPoseStack.pushPose();
        ItemStack itemstack = pEntity.getItem();
        this.random.setSeed((long)getSeedForItemStack(itemstack));
        BakedModel bakedmodel = this.itemRenderer.getModel(itemstack, pEntity.level(), null, pEntity.getId());
        boolean flag = bakedmodel.isGui3d();
        float f = 0.25F;
        boolean shouldBob = net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(itemstack).shouldBobAsEntity(itemstack);
        float f1 = shouldBob ? Mth.sin(((float)pEntity.getAge() + pPartialTicks) / 10.0F + pEntity.bobOffs) * 0.1F + 0.1F : 0;
        float f2 = bakedmodel.getTransforms().getTransform(ItemDisplayContext.GROUND).scale.y();
        pPoseStack.translate(0.0F, f1 + 0.25F * f2, 0.0F);
        float f3 = pEntity.getSpin(pPartialTicks);
        pPoseStack.mulPose(Axis.YP.rotation(f3));
        renderMultipleFromCount(this.itemRenderer, pPoseStack, pBuffer, pPackedLight, itemstack, bakedmodel, flag, this.random);
        pPoseStack.popPose();
        super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
    }

    public static int getSeedForItemStack(ItemStack pStack) {
        return pStack.isEmpty() ? 187 : Item.getId(pStack.getItem()) + pStack.getDamageValue();
    }

    @VisibleForTesting
    static int getRenderedAmount(int pCount) {
        if (pCount <= 1) {
            return 1;
        } else if (pCount <= 16) {
            return 2;
        } else if (pCount <= 32) {
            return 3;
        } else {
            return pCount <= 48 ? 4 : 5;
        }
    }

    public static void renderMultipleFromCount(
        ItemRenderer pItemRenderer, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, ItemStack pItem, RandomSource pRandom, Level pLevel
    ) {
        BakedModel bakedmodel = pItemRenderer.getModel(pItem, pLevel, null, 0);
        renderMultipleFromCount(pItemRenderer, pPoseStack, pBuffer, pPackedLight, pItem, bakedmodel, bakedmodel.isGui3d(), pRandom);
    }

    public static void renderMultipleFromCount(
        ItemRenderer pItemRenderer,
        PoseStack pPoseStack,
        MultiBufferSource pBuffer,
        int pPackedLight,
        ItemStack pItem,
        BakedModel pModel,
        boolean pIsGui3d,
        RandomSource pRandom
    ) {
        int i = getRenderedAmount(pItem.getCount());
        float f = pModel.getTransforms().ground.scale.x();
        float f1 = pModel.getTransforms().ground.scale.y();
        float f2 = pModel.getTransforms().ground.scale.z();
        if (!pIsGui3d) {
            float f3 = -0.0F * (float)(i - 1) * 0.5F * f;
            float f4 = -0.0F * (float)(i - 1) * 0.5F * f1;
            float f5 = -0.09375F * (float)(i - 1) * 0.5F * f2;
            pPoseStack.translate(f3, f4, f5);
        }

        boolean shouldSpread = net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(pItem).shouldSpreadAsEntity(pItem);
        for (int j = 0; j < i; j++) {
            pPoseStack.pushPose();
            if (j > 0 && shouldSpread) {
                if (pIsGui3d) {
                    float f7 = (pRandom.nextFloat() * 2.0F - 1.0F) * 0.15F;
                    float f9 = (pRandom.nextFloat() * 2.0F - 1.0F) * 0.15F;
                    float f6 = (pRandom.nextFloat() * 2.0F - 1.0F) * 0.15F;
                    pPoseStack.translate(f7, f9, f6);
                } else {
                    float f8 = (pRandom.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                    float f10 = (pRandom.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                    pPoseStack.translate(f8, f10, 0.0F);
                }
            }

            pItemRenderer.render(pItem, ItemDisplayContext.GROUND, false, pPoseStack, pBuffer, pPackedLight, OverlayTexture.NO_OVERLAY, pModel);
            pPoseStack.popPose();
            if (!pIsGui3d) {
                pPoseStack.translate(0.0F * f, 0.0F * f1, 0.09375F * f2);
            }
        }
    }
}
