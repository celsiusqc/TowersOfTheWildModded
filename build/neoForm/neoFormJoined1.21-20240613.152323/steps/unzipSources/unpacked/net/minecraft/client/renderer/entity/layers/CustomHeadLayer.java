package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CustomHeadLayer<T extends LivingEntity, M extends EntityModel<T> & HeadedModel> extends RenderLayer<T, M> {
    private final float scaleX;
    private final float scaleY;
    private final float scaleZ;
    private final Map<SkullBlock.Type, SkullModelBase> skullModels;
    private final ItemInHandRenderer itemInHandRenderer;

    public CustomHeadLayer(RenderLayerParent<T, M> pRenderer, EntityModelSet pModelSet, ItemInHandRenderer pItemInHandRenderer) {
        this(pRenderer, pModelSet, 1.0F, 1.0F, 1.0F, pItemInHandRenderer);
    }

    public CustomHeadLayer(
        RenderLayerParent<T, M> pRenderer, EntityModelSet pModelSet, float pScaleX, float pScaleY, float pScaleZ, ItemInHandRenderer pItemInHandRenderer
    ) {
        super(pRenderer);
        this.scaleX = pScaleX;
        this.scaleY = pScaleY;
        this.scaleZ = pScaleZ;
        this.skullModels = SkullBlockRenderer.createSkullRenderers(pModelSet);
        this.itemInHandRenderer = pItemInHandRenderer;
    }

    public void render(
        PoseStack pPoseStack,
        MultiBufferSource pBuffer,
        int pPackedLight,
        T pLivingEntity,
        float pLimbSwing,
        float pLimbSwingAmount,
        float pPartialTicks,
        float pAgeInTicks,
        float pNetHeadYaw,
        float pHeadPitch
    ) {
        ItemStack itemstack = pLivingEntity.getItemBySlot(EquipmentSlot.HEAD);
        if (!itemstack.isEmpty()) {
            Item item = itemstack.getItem();
            pPoseStack.pushPose();
            pPoseStack.scale(this.scaleX, this.scaleY, this.scaleZ);
            boolean flag = pLivingEntity instanceof Villager || pLivingEntity instanceof ZombieVillager;
            if (pLivingEntity.isBaby() && !(pLivingEntity instanceof Villager)) {
                float f = 2.0F;
                float f1 = 1.4F;
                pPoseStack.translate(0.0F, 0.03125F, 0.0F);
                pPoseStack.scale(0.7F, 0.7F, 0.7F);
                pPoseStack.translate(0.0F, 1.0F, 0.0F);
            }

            this.getParentModel().getHead().translateAndRotate(pPoseStack);
            if (item instanceof BlockItem && ((BlockItem)item).getBlock() instanceof AbstractSkullBlock) {
                float f2 = 1.1875F;
                pPoseStack.scale(1.1875F, -1.1875F, -1.1875F);
                if (flag) {
                    pPoseStack.translate(0.0F, 0.0625F, 0.0F);
                }

                ResolvableProfile resolvableprofile = itemstack.get(DataComponents.PROFILE);
                pPoseStack.translate(-0.5, 0.0, -0.5);
                SkullBlock.Type skullblock$type = ((AbstractSkullBlock)((BlockItem)item).getBlock()).getType();
                SkullModelBase skullmodelbase = this.skullModels.get(skullblock$type);
                RenderType rendertype = SkullBlockRenderer.getRenderType(skullblock$type, resolvableprofile);
                WalkAnimationState walkanimationstate;
                if (pLivingEntity.getVehicle() instanceof LivingEntity livingentity) {
                    walkanimationstate = livingentity.walkAnimation;
                } else {
                    walkanimationstate = pLivingEntity.walkAnimation;
                }

                float f3 = walkanimationstate.position(pPartialTicks);
                SkullBlockRenderer.renderSkull(null, 180.0F, f3, pPoseStack, pBuffer, pPackedLight, skullmodelbase, rendertype);
            } else if (!(item instanceof ArmorItem armoritem) || armoritem.getEquipmentSlot() != EquipmentSlot.HEAD) {
                translateToHead(pPoseStack, flag);
                this.itemInHandRenderer.renderItem(pLivingEntity, itemstack, ItemDisplayContext.HEAD, false, pPoseStack, pBuffer, pPackedLight);
            }

            pPoseStack.popPose();
        }
    }

    public static void translateToHead(PoseStack pPoseStack, boolean pIsVillager) {
        float f = 0.625F;
        pPoseStack.translate(0.0F, -0.25F, 0.0F);
        pPoseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        pPoseStack.scale(0.625F, -0.625F, -0.625F);
        if (pIsVillager) {
            pPoseStack.translate(0.0F, 0.1875F, 0.0F);
        }
    }
}
