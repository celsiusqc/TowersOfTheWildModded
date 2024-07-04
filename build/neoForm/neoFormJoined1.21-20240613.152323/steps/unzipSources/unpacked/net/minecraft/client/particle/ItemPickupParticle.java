package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemPickupParticle extends Particle {
    private static final int LIFE_TIME = 3;
    private final RenderBuffers renderBuffers;
    private final Entity itemEntity;
    private final Entity target;
    private int life;
    private final EntityRenderDispatcher entityRenderDispatcher;
    private double targetX;
    private double targetY;
    private double targetZ;
    private double targetXOld;
    private double targetYOld;
    private double targetZOld;

    public ItemPickupParticle(EntityRenderDispatcher pEntityRenderDispatcher, RenderBuffers pBuffers, ClientLevel pLevel, Entity pItemEntity, Entity pTarget) {
        this(pEntityRenderDispatcher, pBuffers, pLevel, pItemEntity, pTarget, pItemEntity.getDeltaMovement());
    }

    private ItemPickupParticle(
        EntityRenderDispatcher pEntityRenderDispatcher, RenderBuffers pBuffers, ClientLevel pLevel, Entity pItemEntity, Entity pTarget, Vec3 pSpeedVector
    ) {
        super(pLevel, pItemEntity.getX(), pItemEntity.getY(), pItemEntity.getZ(), pSpeedVector.x, pSpeedVector.y, pSpeedVector.z);
        this.renderBuffers = pBuffers;
        this.itemEntity = this.getSafeCopy(pItemEntity);
        this.target = pTarget;
        this.entityRenderDispatcher = pEntityRenderDispatcher;
        this.updatePosition();
        this.saveOldPosition();
    }

    private Entity getSafeCopy(Entity pEntity) {
        return (Entity)(!(pEntity instanceof ItemEntity) ? pEntity : ((ItemEntity)pEntity).copy());
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }

    @Override
    public void render(VertexConsumer pBuffer, Camera pRenderInfo, float pPartialTicks) {
        float f = ((float)this.life + pPartialTicks) / 3.0F;
        f *= f;
        double d0 = Mth.lerp((double)pPartialTicks, this.targetXOld, this.targetX);
        double d1 = Mth.lerp((double)pPartialTicks, this.targetYOld, this.targetY);
        double d2 = Mth.lerp((double)pPartialTicks, this.targetZOld, this.targetZ);
        double d3 = Mth.lerp((double)f, this.itemEntity.getX(), d0);
        double d4 = Mth.lerp((double)f, this.itemEntity.getY(), d1);
        double d5 = Mth.lerp((double)f, this.itemEntity.getZ(), d2);
        MultiBufferSource.BufferSource multibuffersource$buffersource = this.renderBuffers.bufferSource();
        Vec3 vec3 = pRenderInfo.getPosition();
        this.entityRenderDispatcher
            .render(
                this.itemEntity,
                d3 - vec3.x(),
                d4 - vec3.y(),
                d5 - vec3.z(),
                this.itemEntity.getYRot(),
                pPartialTicks,
                new PoseStack(),
                multibuffersource$buffersource,
                this.entityRenderDispatcher.getPackedLightCoords(this.itemEntity, pPartialTicks)
            );
        multibuffersource$buffersource.endBatch();
    }

    @Override
    public void tick() {
        this.life++;
        if (this.life == 3) {
            this.remove();
        }

        this.saveOldPosition();
        this.updatePosition();
    }

    private void updatePosition() {
        this.targetX = this.target.getX();
        this.targetY = (this.target.getY() + this.target.getEyeY()) / 2.0;
        this.targetZ = this.target.getZ();
    }

    private void saveOldPosition() {
        this.targetXOld = this.targetX;
        this.targetYOld = this.targetY;
        this.targetZOld = this.targetZ;
    }

    // Neo: Computing a bounding box for the pickup animation is annoying to patch in, and probably slower than
    // always rendering it
    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(float partialTicks) {
        return net.minecraft.world.phys.AABB.INFINITE;
    }
}
