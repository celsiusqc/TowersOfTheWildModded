package net.minecraft.world.entity;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import org.slf4j.Logger;

public class AreaEffectCloud extends Entity implements TraceableEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int TIME_BETWEEN_APPLICATIONS = 5;
    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(AreaEffectCloud.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_WAITING = SynchedEntityData.defineId(AreaEffectCloud.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<ParticleOptions> DATA_PARTICLE = SynchedEntityData.defineId(AreaEffectCloud.class, EntityDataSerializers.PARTICLE);
    private static final float MAX_RADIUS = 32.0F;
    private static final float MINIMAL_RADIUS = 0.5F;
    private static final float DEFAULT_RADIUS = 3.0F;
    public static final float DEFAULT_WIDTH = 6.0F;
    public static final float HEIGHT = 0.5F;
    private PotionContents potionContents = PotionContents.EMPTY;
    private final Map<Entity, Integer> victims = Maps.newHashMap();
    private int duration = 600;
    private int waitTime = 20;
    private int reapplicationDelay = 20;
    private int durationOnUse;
    private float radiusOnUse;
    private float radiusPerTick;
    @Nullable
    private LivingEntity owner;
    @Nullable
    private UUID ownerUUID;

    public AreaEffectCloud(EntityType<? extends AreaEffectCloud> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.noPhysics = true;
    }

    public AreaEffectCloud(Level pLevel, double pX, double pY, double pZ) {
        this(EntityType.AREA_EFFECT_CLOUD, pLevel);
        this.setPos(pX, pY, pZ);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        pBuilder.define(DATA_RADIUS, 3.0F);
        pBuilder.define(DATA_WAITING, false);
        pBuilder.define(DATA_PARTICLE, ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, -1));
    }

    public void setRadius(float pRadius) {
        if (!this.level().isClientSide) {
            this.getEntityData().set(DATA_RADIUS, Mth.clamp(pRadius, 0.0F, 32.0F));
        }
    }

    @Override
    public void refreshDimensions() {
        double d0 = this.getX();
        double d1 = this.getY();
        double d2 = this.getZ();
        super.refreshDimensions();
        this.setPos(d0, d1, d2);
    }

    public float getRadius() {
        return this.getEntityData().get(DATA_RADIUS);
    }

    public void setPotionContents(PotionContents p_330869_) {
        this.potionContents = p_330869_;
        this.updateColor();
    }

    private void updateColor() {
        ParticleOptions particleoptions = this.entityData.get(DATA_PARTICLE);
        if (particleoptions instanceof ColorParticleOption colorparticleoption) {
            int i = this.potionContents.equals(PotionContents.EMPTY) ? 0 : this.potionContents.getColor();
            this.entityData.set(DATA_PARTICLE, ColorParticleOption.create(colorparticleoption.getType(), FastColor.ARGB32.opaque(i)));
        }
    }

    public void addEffect(MobEffectInstance pEffectInstance) {
        this.setPotionContents(this.potionContents.withEffectAdded(pEffectInstance));
    }

    public ParticleOptions getParticle() {
        return this.getEntityData().get(DATA_PARTICLE);
    }

    public void setParticle(ParticleOptions p_19725_) {
        this.getEntityData().set(DATA_PARTICLE, p_19725_);
    }

    /**
     * Sets if the cloud is waiting. While waiting, the radius is ignored and the cloud shows fewer particles in its area.
     */
    protected void setWaiting(boolean pWaiting) {
        this.getEntityData().set(DATA_WAITING, pWaiting);
    }

    public boolean isWaiting() {
        return this.getEntityData().get(DATA_WAITING);
    }

    public int getDuration() {
        return this.duration;
    }

    public void setDuration(int pDuration) {
        this.duration = pDuration;
    }

    @Override
    public void tick() {
        super.tick();
        boolean flag = this.isWaiting();
        float f = this.getRadius();
        if (this.level().isClientSide) {
            if (flag && this.random.nextBoolean()) {
                return;
            }

            ParticleOptions particleoptions = this.getParticle();
            int i;
            float f1;
            if (flag) {
                i = 2;
                f1 = 0.2F;
            } else {
                i = Mth.ceil((float) Math.PI * f * f);
                f1 = f;
            }

            for (int j = 0; j < i; j++) {
                float f2 = this.random.nextFloat() * (float) (Math.PI * 2);
                float f3 = Mth.sqrt(this.random.nextFloat()) * f1;
                double d0 = this.getX() + (double)(Mth.cos(f2) * f3);
                double d2 = this.getY();
                double d4 = this.getZ() + (double)(Mth.sin(f2) * f3);
                if (particleoptions.getType() == ParticleTypes.ENTITY_EFFECT) {
                    if (flag && this.random.nextBoolean()) {
                        this.level().addAlwaysVisibleParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, -1), d0, d2, d4, 0.0, 0.0, 0.0);
                    } else {
                        this.level().addAlwaysVisibleParticle(particleoptions, d0, d2, d4, 0.0, 0.0, 0.0);
                    }
                } else if (flag) {
                    this.level().addAlwaysVisibleParticle(particleoptions, d0, d2, d4, 0.0, 0.0, 0.0);
                } else {
                    this.level()
                        .addAlwaysVisibleParticle(
                            particleoptions, d0, d2, d4, (0.5 - this.random.nextDouble()) * 0.15, 0.01F, (0.5 - this.random.nextDouble()) * 0.15
                        );
                }
            }
        } else {
            if (this.tickCount >= this.waitTime + this.duration) {
                this.discard();
                return;
            }

            boolean flag1 = this.tickCount < this.waitTime;
            if (flag != flag1) {
                this.setWaiting(flag1);
            }

            if (flag1) {
                return;
            }

            if (this.radiusPerTick != 0.0F) {
                f += this.radiusPerTick;
                if (f < 0.5F) {
                    this.discard();
                    return;
                }

                this.setRadius(f);
            }

            if (this.tickCount % 5 == 0) {
                this.victims.entrySet().removeIf(p_287380_ -> this.tickCount >= p_287380_.getValue());
                if (!this.potionContents.hasEffects()) {
                    this.victims.clear();
                } else {
                    List<MobEffectInstance> list = Lists.newArrayList();
                    if (this.potionContents.potion().isPresent()) {
                        for (MobEffectInstance mobeffectinstance1 : this.potionContents.potion().get().value().getEffects()) {
                            list.add(
                                new MobEffectInstance(
                                    mobeffectinstance1.getEffect(),
                                    mobeffectinstance1.mapDuration(p_267926_ -> p_267926_ / 4),
                                    mobeffectinstance1.getAmplifier(),
                                    mobeffectinstance1.isAmbient(),
                                    mobeffectinstance1.isVisible()
                                )
                            );
                        }
                    }

                    list.addAll(this.potionContents.customEffects());
                    List<LivingEntity> list1 = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox());
                    if (!list1.isEmpty()) {
                        for (LivingEntity livingentity : list1) {
                            if (!this.victims.containsKey(livingentity)
                                && livingentity.isAffectedByPotions()
                                && !list.stream().noneMatch(livingentity::canBeAffected)) {
                                double d5 = livingentity.getX() - this.getX();
                                double d1 = livingentity.getZ() - this.getZ();
                                double d3 = d5 * d5 + d1 * d1;
                                if (d3 <= (double)(f * f)) {
                                    this.victims.put(livingentity, this.tickCount + this.reapplicationDelay);

                                    for (MobEffectInstance mobeffectinstance : list) {
                                        if (mobeffectinstance.getEffect().value().isInstantenous()) {
                                            mobeffectinstance.getEffect()
                                                .value()
                                                .applyInstantenousEffect(this, this.getOwner(), livingentity, mobeffectinstance.getAmplifier(), 0.5);
                                        } else {
                                            livingentity.addEffect(new MobEffectInstance(mobeffectinstance), this);
                                        }
                                    }

                                    if (this.radiusOnUse != 0.0F) {
                                        f += this.radiusOnUse;
                                        if (f < 0.5F) {
                                            this.discard();
                                            return;
                                        }

                                        this.setRadius(f);
                                    }

                                    if (this.durationOnUse != 0) {
                                        this.duration = this.duration + this.durationOnUse;
                                        if (this.duration <= 0) {
                                            this.discard();
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public float getRadiusOnUse() {
        return this.radiusOnUse;
    }

    public void setRadiusOnUse(float pRadiusOnUse) {
        this.radiusOnUse = pRadiusOnUse;
    }

    public float getRadiusPerTick() {
        return this.radiusPerTick;
    }

    public void setRadiusPerTick(float pRadiusPerTick) {
        this.radiusPerTick = pRadiusPerTick;
    }

    public int getDurationOnUse() {
        return this.durationOnUse;
    }

    public void setDurationOnUse(int pDurationOnUse) {
        this.durationOnUse = pDurationOnUse;
    }

    public int getWaitTime() {
        return this.waitTime;
    }

    public void setWaitTime(int pWaitTime) {
        this.waitTime = pWaitTime;
    }

    public void setOwner(@Nullable LivingEntity pOwner) {
        this.owner = pOwner;
        this.ownerUUID = pOwner == null ? null : pOwner.getUUID();
    }

    @Nullable
    public LivingEntity getOwner() {
        if (this.owner == null && this.ownerUUID != null && this.level() instanceof ServerLevel) {
            Entity entity = ((ServerLevel)this.level()).getEntity(this.ownerUUID);
            if (entity instanceof LivingEntity) {
                this.owner = (LivingEntity)entity;
            }
        }

        return this.owner;
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        this.tickCount = pCompound.getInt("Age");
        this.duration = pCompound.getInt("Duration");
        this.waitTime = pCompound.getInt("WaitTime");
        this.reapplicationDelay = pCompound.getInt("ReapplicationDelay");
        this.durationOnUse = pCompound.getInt("DurationOnUse");
        this.radiusOnUse = pCompound.getFloat("RadiusOnUse");
        this.radiusPerTick = pCompound.getFloat("RadiusPerTick");
        this.setRadius(pCompound.getFloat("Radius"));
        if (pCompound.hasUUID("Owner")) {
            this.ownerUUID = pCompound.getUUID("Owner");
        }

        RegistryOps<Tag> registryops = this.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        if (pCompound.contains("Particle", 10)) {
            ParticleTypes.CODEC
                .parse(registryops, pCompound.get("Particle"))
                .resultOrPartial(p_329991_ -> LOGGER.warn("Failed to parse area effect cloud particle options: '{}'", p_329991_))
                .ifPresent(this::setParticle);
        }

        if (pCompound.contains("potion_contents")) {
            PotionContents.CODEC
                .parse(registryops, pCompound.get("potion_contents"))
                .resultOrPartial(p_340707_ -> LOGGER.warn("Failed to parse area effect cloud potions: '{}'", p_340707_))
                .ifPresent(this::setPotionContents);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        pCompound.putInt("Age", this.tickCount);
        pCompound.putInt("Duration", this.duration);
        pCompound.putInt("WaitTime", this.waitTime);
        pCompound.putInt("ReapplicationDelay", this.reapplicationDelay);
        pCompound.putInt("DurationOnUse", this.durationOnUse);
        pCompound.putFloat("RadiusOnUse", this.radiusOnUse);
        pCompound.putFloat("RadiusPerTick", this.radiusPerTick);
        pCompound.putFloat("Radius", this.getRadius());
        RegistryOps<Tag> registryops = this.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        pCompound.put("Particle", ParticleTypes.CODEC.encodeStart(registryops, this.getParticle()).getOrThrow());
        if (this.ownerUUID != null) {
            pCompound.putUUID("Owner", this.ownerUUID);
        }

        if (!this.potionContents.equals(PotionContents.EMPTY)) {
            Tag tag = PotionContents.CODEC.encodeStart(registryops, this.potionContents).getOrThrow();
            pCompound.put("potion_contents", tag);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        if (DATA_RADIUS.equals(pKey)) {
            this.refreshDimensions();
        }

        super.onSyncedDataUpdated(pKey);
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public EntityDimensions getDimensions(Pose pPose) {
        return EntityDimensions.scalable(this.getRadius() * 2.0F, 0.5F);
    }
}
