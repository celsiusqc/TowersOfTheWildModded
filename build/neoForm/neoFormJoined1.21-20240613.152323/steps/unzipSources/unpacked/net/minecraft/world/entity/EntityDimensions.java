package net.minecraft.world.entity;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record EntityDimensions(float width, float height, float eyeHeight, EntityAttachments attachments, boolean fixed) {
    private EntityDimensions(float p_20381_, float p_20382_, boolean p_20383_) {
        this(p_20381_, p_20382_, defaultEyeHeight(p_20382_), EntityAttachments.createDefault(p_20381_, p_20382_), p_20383_);
    }

    private static float defaultEyeHeight(float pHeight) {
        return pHeight * 0.85F;
    }

    public AABB makeBoundingBox(Vec3 pPos) {
        return this.makeBoundingBox(pPos.x, pPos.y, pPos.z);
    }

    public AABB makeBoundingBox(double pX, double pY, double pZ) {
        float f = this.width / 2.0F;
        float f1 = this.height;
        return new AABB(pX - (double)f, pY, pZ - (double)f, pX + (double)f, pY + (double)f1, pZ + (double)f);
    }

    public EntityDimensions scale(float pFactor) {
        return this.scale(pFactor, pFactor);
    }

    public EntityDimensions scale(float pWidthFactor, float pHeightFactor) {
        return !this.fixed && (pWidthFactor != 1.0F || pHeightFactor != 1.0F)
            ? new EntityDimensions(
                this.width * pWidthFactor, this.height * pHeightFactor, this.eyeHeight * pHeightFactor, this.attachments.scale(pWidthFactor, pHeightFactor, pWidthFactor), false
            )
            : this;
    }

    public static EntityDimensions scalable(float pWidth, float pHeight) {
        return new EntityDimensions(pWidth, pHeight, false);
    }

    public static EntityDimensions fixed(float pWidth, float pHeight) {
        return new EntityDimensions(pWidth, pHeight, true);
    }

    public EntityDimensions withEyeHeight(float pEyeHeight) {
        return new EntityDimensions(this.width, this.height, pEyeHeight, this.attachments, this.fixed);
    }

    public EntityDimensions withAttachments(EntityAttachments.Builder pAttachments) {
        return new EntityDimensions(this.width, this.height, this.eyeHeight, pAttachments.build(this.width, this.height), this.fixed);
    }
}
