package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ArrowLayer<T extends LivingEntity, M extends PlayerModel<T>> extends StuckInBodyLayer<T, M> {
    private final EntityRenderDispatcher dispatcher;

    public ArrowLayer(EntityRendererProvider.Context pContext, LivingEntityRenderer<T, M> pRenderer) {
        super(pRenderer);
        this.dispatcher = pContext.getEntityRenderDispatcher();
    }

    @Override
    protected int numStuck(T pEntity) {
        return pEntity.getArrowCount();
    }

    @Override
    protected void renderStuckItem(
        PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, Entity pEntity, float pX, float pY, float pZ, float pPartialTick
    ) {
        float f = Mth.sqrt(pX * pX + pZ * pZ);
        Arrow arrow = new Arrow(pEntity.level(), pEntity.getX(), pEntity.getY(), pEntity.getZ(), ItemStack.EMPTY, null);
        arrow.setYRot((float)(Math.atan2((double)pX, (double)pZ) * 180.0F / (float)Math.PI));
        arrow.setXRot((float)(Math.atan2((double)pY, (double)f) * 180.0F / (float)Math.PI));
        arrow.yRotO = arrow.getYRot();
        arrow.xRotO = arrow.getXRot();
        this.dispatcher.render(arrow, 0.0, 0.0, 0.0, 0.0F, pPartialTick, pPoseStack, pBuffer, pPackedLight);
    }
}
