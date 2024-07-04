package net.minecraft.client.player;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RemotePlayer extends AbstractClientPlayer {
    private Vec3 lerpDeltaMovement = Vec3.ZERO;
    private int lerpDeltaMovementSteps;

    public RemotePlayer(ClientLevel pClientLevel, GameProfile pGameProfile) {
        super(pClientLevel, pGameProfile);
        this.noPhysics = true;
    }

    /**
     * Checks if the entity is in range to render.
     */
    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        double d0 = this.getBoundingBox().getSize() * 10.0;
        if (Double.isNaN(d0)) {
            d0 = 1.0;
        }

        d0 *= 64.0 * getViewScale();
        return pDistance < d0 * d0;
    }

    /**
     * Called when the entity is attacked.
     */
    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        net.neoforged.neoforge.common.CommonHooks.onPlayerAttack(this, pSource, pAmount);
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        this.calculateEntityAnimation(false);
    }

    @Override
    public void aiStep() {
        if (this.lerpSteps > 0) {
            this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.lerpYRot, this.lerpXRot);
            this.lerpSteps--;
        }

        if (this.lerpHeadSteps > 0) {
            this.lerpHeadRotationStep(this.lerpHeadSteps, this.lerpYHeadRot);
            this.lerpHeadSteps--;
        }

        if (this.lerpDeltaMovementSteps > 0) {
            this.addDeltaMovement(
                new Vec3(
                    (this.lerpDeltaMovement.x - this.getDeltaMovement().x) / (double)this.lerpDeltaMovementSteps,
                    (this.lerpDeltaMovement.y - this.getDeltaMovement().y) / (double)this.lerpDeltaMovementSteps,
                    (this.lerpDeltaMovement.z - this.getDeltaMovement().z) / (double)this.lerpDeltaMovementSteps
                )
            );
            this.lerpDeltaMovementSteps--;
        }

        this.oBob = this.bob;
        this.updateSwingTime();
        float f;
        if (this.onGround() && !this.isDeadOrDying()) {
            f = (float)Math.min(0.1, this.getDeltaMovement().horizontalDistance());
        } else {
            f = 0.0F;
        }

        this.bob = this.bob + (f - this.bob) * 0.4F;
        this.level().getProfiler().push("push");
        this.pushEntities();
        this.level().getProfiler().pop();
    }

    /**
     * Updates the entity motion clientside, called by packets from the server
     */
    @Override
    public void lerpMotion(double pX, double pY, double pZ) {
        this.lerpDeltaMovement = new Vec3(pX, pY, pZ);
        this.lerpDeltaMovementSteps = this.getType().updateInterval() + 1;
    }

    @Override
    protected void updatePlayerPose() {
    }

    @Override
    public void sendSystemMessage(Component pComponent) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.gui.getChat().addMessage(pComponent);
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket pPacket) {
        super.recreateFromPacket(pPacket);
        this.setOldPosAndRot();
    }
}
