package net.minecraft.world.entity;

import java.util.List;
import net.minecraft.world.phys.Vec3;

public enum EntityAttachment {
    PASSENGER(EntityAttachment.Fallback.AT_HEIGHT),
    VEHICLE(EntityAttachment.Fallback.AT_FEET),
    NAME_TAG(EntityAttachment.Fallback.AT_HEIGHT),
    WARDEN_CHEST(EntityAttachment.Fallback.AT_CENTER);

    private final EntityAttachment.Fallback fallback;

    private EntityAttachment(EntityAttachment.Fallback pFallback) {
        this.fallback = pFallback;
    }

    public List<Vec3> createFallbackPoints(float pWidth, float pHeight) {
        return this.fallback.create(pWidth, pHeight);
    }

    public interface Fallback {
        List<Vec3> ZERO = List.of(Vec3.ZERO);
        EntityAttachment.Fallback AT_FEET = (p_316289_, p_316334_) -> ZERO;
        EntityAttachment.Fallback AT_HEIGHT = (p_316259_, p_316219_) -> List.of(new Vec3(0.0, (double)p_316219_, 0.0));
        EntityAttachment.Fallback AT_CENTER = (p_319580_, p_319581_) -> List.of(new Vec3(0.0, (double)p_319581_ / 2.0, 0.0));

        List<Vec3> create(float pWidth, float pHeight);
    }
}
