package net.minecraft.world.level;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class Explosion {
    private static final ExplosionDamageCalculator EXPLOSION_DAMAGE_CALCULATOR = new ExplosionDamageCalculator();
    private static final int MAX_DROPS_PER_COMBINED_STACK = 16;
    private final boolean fire;
    private final Explosion.BlockInteraction blockInteraction;
    private final RandomSource random = RandomSource.create();
    private final Level level;
    private final double x;
    private final double y;
    private final double z;
    @Nullable
    private final Entity source;
    private final float radius;
    private final DamageSource damageSource;
    private final ExplosionDamageCalculator damageCalculator;
    private final ParticleOptions smallExplosionParticles;
    private final ParticleOptions largeExplosionParticles;
    private final Holder<SoundEvent> explosionSound;
    private final ObjectArrayList<BlockPos> toBlow = new ObjectArrayList<>();
    private final Map<Player, Vec3> hitPlayers = Maps.newHashMap();

    public static DamageSource getDefaultDamageSource(Level pLevel, @Nullable Entity pSource) {
        return pLevel.damageSources().explosion(pSource, getIndirectSourceEntityInternal(pSource));
    }

    public Explosion(
        Level pLevel,
        @Nullable Entity pSource,
        double pX,
        double pY,
        double pZ,
        float pRadius,
        List<BlockPos> pToBlow,
        Explosion.BlockInteraction pBlockInteraction,
        ParticleOptions pSmallExplosionParticles,
        ParticleOptions pLargeExplosionParticles,
        Holder<SoundEvent> pExplosionSound
    ) {
        this(
            pLevel,
            pSource,
            getDefaultDamageSource(pLevel, pSource),
            null,
            pX,
            pY,
            pZ,
            pRadius,
            false,
            pBlockInteraction,
            pSmallExplosionParticles,
            pLargeExplosionParticles,
            pExplosionSound
        );
        this.toBlow.addAll(pToBlow);
    }

    public Explosion(
        Level pLevel,
        @Nullable Entity pSource,
        double pX,
        double pY,
        double pZ,
        float pRadius,
        boolean pFire,
        Explosion.BlockInteraction pBlockInteraction,
        List<BlockPos> pPositions
    ) {
        this(pLevel, pSource, pX, pY, pZ, pRadius, pFire, pBlockInteraction);
        this.toBlow.addAll(pPositions);
    }

    public Explosion(
        Level pLevel,
        @Nullable Entity pSource,
        double pX,
        double pY,
        double pZ,
        float pRadius,
        boolean pFire,
        Explosion.BlockInteraction pBlockInteraction
    ) {
        this(
            pLevel,
            pSource,
            getDefaultDamageSource(pLevel, pSource),
            null,
            pX,
            pY,
            pZ,
            pRadius,
            pFire,
            pBlockInteraction,
            ParticleTypes.EXPLOSION,
            ParticleTypes.EXPLOSION_EMITTER,
            SoundEvents.GENERIC_EXPLODE
        );
    }

    public Explosion(
        Level pLevel,
        @Nullable Entity pSource,
        @Nullable DamageSource pDamageSource,
        @Nullable ExplosionDamageCalculator pDamageCalculator,
        double pX,
        double pY,
        double pZ,
        float pRadius,
        boolean pFire,
        Explosion.BlockInteraction pBlockInteraction,
        ParticleOptions pSmallExplosionParticles,
        ParticleOptions pLargeExplosionParticles,
        Holder<SoundEvent> pExplosionSound
    ) {
        this.level = pLevel;
        this.source = pSource;
        this.radius = pRadius;
        this.x = pX;
        this.y = pY;
        this.z = pZ;
        this.fire = pFire;
        this.blockInteraction = pBlockInteraction;
        this.damageSource = pDamageSource == null ? pLevel.damageSources().explosion(this) : pDamageSource;
        this.damageCalculator = pDamageCalculator == null ? this.makeDamageCalculator(pSource) : pDamageCalculator;
        this.smallExplosionParticles = pSmallExplosionParticles;
        this.largeExplosionParticles = pLargeExplosionParticles;
        this.explosionSound = pExplosionSound;
    }

    private ExplosionDamageCalculator makeDamageCalculator(@Nullable Entity pEntity) {
        return (ExplosionDamageCalculator)(pEntity == null ? EXPLOSION_DAMAGE_CALCULATOR : new EntityBasedExplosionDamageCalculator(pEntity));
    }

    public static float getSeenPercent(Vec3 pExplosionVector, Entity pEntity) {
        AABB aabb = pEntity.getBoundingBox();
        double d0 = 1.0 / ((aabb.maxX - aabb.minX) * 2.0 + 1.0);
        double d1 = 1.0 / ((aabb.maxY - aabb.minY) * 2.0 + 1.0);
        double d2 = 1.0 / ((aabb.maxZ - aabb.minZ) * 2.0 + 1.0);
        double d3 = (1.0 - Math.floor(1.0 / d0) * d0) / 2.0;
        double d4 = (1.0 - Math.floor(1.0 / d2) * d2) / 2.0;
        if (!(d0 < 0.0) && !(d1 < 0.0) && !(d2 < 0.0)) {
            int i = 0;
            int j = 0;

            for (double d5 = 0.0; d5 <= 1.0; d5 += d0) {
                for (double d6 = 0.0; d6 <= 1.0; d6 += d1) {
                    for (double d7 = 0.0; d7 <= 1.0; d7 += d2) {
                        double d8 = Mth.lerp(d5, aabb.minX, aabb.maxX);
                        double d9 = Mth.lerp(d6, aabb.minY, aabb.maxY);
                        double d10 = Mth.lerp(d7, aabb.minZ, aabb.maxZ);
                        Vec3 vec3 = new Vec3(d8 + d3, d9, d10 + d4);
                        if (pEntity.level().clip(new ClipContext(vec3, pExplosionVector, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, pEntity)).getType()
                            == HitResult.Type.MISS) {
                            i++;
                        }

                        j++;
                    }
                }
            }

            return (float)i / (float)j;
        } else {
            return 0.0F;
        }
    }

    public float radius() {
        return this.radius;
    }

    public Vec3 center() {
        return new Vec3(this.x, this.y, this.z);
    }

    public void explode() {
        this.level.gameEvent(this.source, GameEvent.EXPLODE, new Vec3(this.x, this.y, this.z));
        Set<BlockPos> set = Sets.newHashSet();
        int i = 16;

        for (int j = 0; j < 16; j++) {
            for (int k = 0; k < 16; k++) {
                for (int l = 0; l < 16; l++) {
                    if (j == 0 || j == 15 || k == 0 || k == 15 || l == 0 || l == 15) {
                        double d0 = (double)((float)j / 15.0F * 2.0F - 1.0F);
                        double d1 = (double)((float)k / 15.0F * 2.0F - 1.0F);
                        double d2 = (double)((float)l / 15.0F * 2.0F - 1.0F);
                        double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                        d0 /= d3;
                        d1 /= d3;
                        d2 /= d3;
                        float f = this.radius * (0.7F + this.level.random.nextFloat() * 0.6F);
                        double d4 = this.x;
                        double d6 = this.y;
                        double d8 = this.z;

                        for (float f1 = 0.3F; f > 0.0F; f -= 0.22500001F) {
                            BlockPos blockpos = BlockPos.containing(d4, d6, d8);
                            BlockState blockstate = this.level.getBlockState(blockpos);
                            FluidState fluidstate = this.level.getFluidState(blockpos);
                            if (!this.level.isInWorldBounds(blockpos)) {
                                break;
                            }

                            Optional<Float> optional = this.damageCalculator.getBlockExplosionResistance(this, this.level, blockpos, blockstate, fluidstate);
                            if (optional.isPresent()) {
                                f -= (optional.get() + 0.3F) * 0.3F;
                            }

                            if (f > 0.0F && this.damageCalculator.shouldBlockExplode(this, this.level, blockpos, blockstate, f)) {
                                set.add(blockpos);
                            }

                            d4 += d0 * 0.3F;
                            d6 += d1 * 0.3F;
                            d8 += d2 * 0.3F;
                        }
                    }
                }
            }
        }

        this.toBlow.addAll(set);
        float f2 = this.radius * 2.0F;
        int k1 = Mth.floor(this.x - (double)f2 - 1.0);
        int l1 = Mth.floor(this.x + (double)f2 + 1.0);
        int i2 = Mth.floor(this.y - (double)f2 - 1.0);
        int i1 = Mth.floor(this.y + (double)f2 + 1.0);
        int j2 = Mth.floor(this.z - (double)f2 - 1.0);
        int j1 = Mth.floor(this.z + (double)f2 + 1.0);
        List<Entity> list = this.level.getEntities(this.source, new AABB((double)k1, (double)i2, (double)j2, (double)l1, (double)i1, (double)j1));
        net.neoforged.neoforge.event.EventHooks.onExplosionDetonate(this.level, this, list, f2);
        Vec3 vec3 = new Vec3(this.x, this.y, this.z);

        for (Entity entity : list) {
            if (!entity.ignoreExplosion(this)) {
                double d11 = Math.sqrt(entity.distanceToSqr(vec3)) / (double)f2;
                if (d11 <= 1.0) {
                    double d5 = entity.getX() - this.x;
                    double d7 = (entity instanceof PrimedTnt ? entity.getY() : entity.getEyeY()) - this.y;
                    double d9 = entity.getZ() - this.z;
                    double d12 = Math.sqrt(d5 * d5 + d7 * d7 + d9 * d9);
                    if (d12 != 0.0) {
                        d5 /= d12;
                        d7 /= d12;
                        d9 /= d12;
                        if (this.damageCalculator.shouldDamageEntity(this, entity)) {
                            entity.hurt(this.damageSource, this.damageCalculator.getEntityDamageAmount(this, entity));
                        }

                        double d13 = (1.0 - d11) * (double)getSeenPercent(vec3, entity) * (double)this.damageCalculator.getKnockbackMultiplier(entity);
                        double d10;
                        if (entity instanceof LivingEntity livingentity) {
                            d10 = d13 * (1.0 - livingentity.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE));
                        } else {
                            d10 = d13;
                        }

                        d5 *= d10;
                        d7 *= d10;
                        d9 *= d10;
                        Vec3 vec31 = new Vec3(d5, d7, d9);
                        vec31 = net.neoforged.neoforge.event.EventHooks.getExplosionKnockback(this.level, this, entity, vec31);
                        entity.setDeltaMovement(entity.getDeltaMovement().add(vec31));
                        if (entity instanceof Player) {
                            Player player = (Player)entity;
                            if (!player.isSpectator() && (!player.isCreative() || !player.getAbilities().flying)) {
                                this.hitPlayers.put(player, vec31);
                            }
                        }

                        entity.onExplosionHit(this.source);
                    }
                }
            }
        }
    }

    /**
     * Does the second part of the explosion (sound, particles, drop spawn)
     */
    public void finalizeExplosion(boolean pSpawnParticles) {
        if (this.level.isClientSide) {
            this.level
                .playLocalSound(
                    this.x,
                    this.y,
                    this.z,
                    this.explosionSound.value(),
                    SoundSource.BLOCKS,
                    4.0F,
                    (1.0F + (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.2F) * 0.7F,
                    false
                );
        }

        boolean flag = this.interactsWithBlocks();
        if (pSpawnParticles) {
            ParticleOptions particleoptions;
            if (!(this.radius < 2.0F) && flag) {
                particleoptions = this.largeExplosionParticles;
            } else {
                particleoptions = this.smallExplosionParticles;
            }

            this.level.addParticle(particleoptions, this.x, this.y, this.z, 1.0, 0.0, 0.0);
        }

        if (flag) {
            this.level.getProfiler().push("explosion_blocks");
            List<Pair<ItemStack, BlockPos>> list = new ArrayList<>();
            Util.shuffle(this.toBlow, this.level.random);

            for (BlockPos blockpos : this.toBlow) {
                this.level
                    .getBlockState(blockpos)
                    .onExplosionHit(this.level, blockpos, this, (p_311741_, p_311742_) -> addOrAppendStack(list, p_311741_, p_311742_));
            }

            for (Pair<ItemStack, BlockPos> pair : list) {
                Block.popResource(this.level, pair.getSecond(), pair.getFirst());
            }

            this.level.getProfiler().pop();
        }

        if (this.fire) {
            for (BlockPos blockpos1 : this.toBlow) {
                if (this.random.nextInt(3) == 0
                    && this.level.getBlockState(blockpos1).isAir()
                    && this.level.getBlockState(blockpos1.below()).isSolidRender(this.level, blockpos1.below())) {
                    this.level.setBlockAndUpdate(blockpos1, BaseFireBlock.getState(this.level, blockpos1));
                }
            }
        }
    }

    private static void addOrAppendStack(List<Pair<ItemStack, BlockPos>> pDrops, ItemStack pStack, BlockPos pPos) {
        for (int i = 0; i < pDrops.size(); i++) {
            Pair<ItemStack, BlockPos> pair = pDrops.get(i);
            ItemStack itemstack = pair.getFirst();
            if (ItemEntity.areMergable(itemstack, pStack)) {
                pDrops.set(i, Pair.of(ItemEntity.merge(itemstack, pStack, 16), pair.getSecond()));
                if (pStack.isEmpty()) {
                    return;
                }
            }
        }

        pDrops.add(Pair.of(pStack, pPos));
    }

    public boolean interactsWithBlocks() {
        return this.blockInteraction != Explosion.BlockInteraction.KEEP;
    }

    public Map<Player, Vec3> getHitPlayers() {
        return this.hitPlayers;
    }

    @Nullable
    private static LivingEntity getIndirectSourceEntityInternal(@Nullable Entity pSource) {
        if (pSource == null) {
            return null;
        } else if (pSource instanceof PrimedTnt primedtnt) {
            return primedtnt.getOwner();
        } else if (pSource instanceof LivingEntity) {
            return (LivingEntity)pSource;
        } else {
            if (pSource instanceof Projectile projectile) {
                Entity entity = projectile.getOwner();
                if (entity instanceof LivingEntity) {
                    return (LivingEntity)entity;
                }
            }

            return null;
        }
    }

    @Nullable
    public LivingEntity getIndirectSourceEntity() {
        return getIndirectSourceEntityInternal(this.source);
    }

    @Nullable
    public Entity getDirectSourceEntity() {
        return this.source;
    }

    public void clearToBlow() {
        this.toBlow.clear();
    }

    public List<BlockPos> getToBlow() {
        return this.toBlow;
    }

    public Explosion.BlockInteraction getBlockInteraction() {
        return this.blockInteraction;
    }

    public ParticleOptions getSmallExplosionParticles() {
        return this.smallExplosionParticles;
    }

    public ParticleOptions getLargeExplosionParticles() {
        return this.largeExplosionParticles;
    }

    public Holder<SoundEvent> getExplosionSound() {
        return this.explosionSound;
    }

    public boolean canTriggerBlocks() {
        if (this.blockInteraction == Explosion.BlockInteraction.TRIGGER_BLOCK && !this.level.isClientSide()) {
            return this.source != null && this.source.getType() == EntityType.BREEZE_WIND_CHARGE
                ? this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                : true;
        } else {
            return false;
        }
    }

    public static enum BlockInteraction {
        KEEP,
        DESTROY,
        DESTROY_WITH_DECAY,
        TRIGGER_BLOCK;
    }
}
