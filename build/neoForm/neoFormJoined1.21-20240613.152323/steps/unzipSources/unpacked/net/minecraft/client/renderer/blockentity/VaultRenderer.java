package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultClientData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VaultRenderer implements BlockEntityRenderer<VaultBlockEntity> {
    private final ItemRenderer itemRenderer;
    private final RandomSource random = RandomSource.create();

    public VaultRenderer(BlockEntityRendererProvider.Context pContext) {
        this.itemRenderer = pContext.getItemRenderer();
    }

    public void render(VaultBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        if (VaultBlockEntity.Client.shouldDisplayActiveEffects(pBlockEntity.getSharedData())) {
            Level level = pBlockEntity.getLevel();
            if (level != null) {
                ItemStack itemstack = pBlockEntity.getSharedData().getDisplayItem();
                if (!itemstack.isEmpty()) {
                    this.random.setSeed((long)ItemEntityRenderer.getSeedForItemStack(itemstack));
                    VaultClientData vaultclientdata = pBlockEntity.getClientData();
                    renderItemInside(
                        pPartialTick,
                        level,
                        pPoseStack,
                        pBuffer,
                        pPackedLight,
                        itemstack,
                        this.itemRenderer,
                        vaultclientdata.previousSpin(),
                        vaultclientdata.currentSpin(),
                        this.random
                    );
                }
            }
        }
    }

    public static void renderItemInside(
        float pPartialTick,
        Level pLevel,
        PoseStack pPoseStack,
        MultiBufferSource pBuffer,
        int pPackedLight,
        ItemStack pItem,
        ItemRenderer pItemRenderer,
        float pPreviousSpin,
        float pCurrentSpin,
        RandomSource pRandom
    ) {
        pPoseStack.pushPose();
        pPoseStack.translate(0.5F, 0.4F, 0.5F);
        pPoseStack.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(pPartialTick, pPreviousSpin, pCurrentSpin)));
        ItemEntityRenderer.renderMultipleFromCount(pItemRenderer, pPoseStack, pBuffer, pPackedLight, pItem, pRandom, pLevel);
        pPoseStack.popPose();
    }
}
