package net.minecraft.world.level.block.entity.trialspawner;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OminousItemSpawner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public enum TrialSpawnerState implements StringRepresentable {
    INACTIVE("inactive", 0, TrialSpawnerState.ParticleEmission.NONE, -1.0, false),
    WAITING_FOR_PLAYERS("waiting_for_players", 4, TrialSpawnerState.ParticleEmission.SMALL_FLAMES, 200.0, true),
    ACTIVE("active", 8, TrialSpawnerState.ParticleEmission.FLAMES_AND_SMOKE, 1000.0, true),
    WAITING_FOR_REWARD_EJECTION("waiting_for_reward_ejection", 8, TrialSpawnerState.ParticleEmission.SMALL_FLAMES, -1.0, false),
    EJECTING_REWARD("ejecting_reward", 8, TrialSpawnerState.ParticleEmission.SMALL_FLAMES, -1.0, false),
    COOLDOWN("cooldown", 0, TrialSpawnerState.ParticleEmission.SMOKE_INSIDE_AND_TOP_FACE, -1.0, false);

    private static final float DELAY_BEFORE_EJECT_AFTER_KILLING_LAST_MOB = 40.0F;
    private static final int TIME_BETWEEN_EACH_EJECTION = Mth.floor(30.0F);
    private final String name;
    private final int lightLevel;
    private final double spinningMobSpeed;
    private final TrialSpawnerState.ParticleEmission particleEmission;
    private final boolean isCapableOfSpawning;

    private TrialSpawnerState(String pName, int pLightLevel, TrialSpawnerState.ParticleEmission pParticleEmission, double pSpinningMobSpeed, boolean pIsCapableOfSpawning) {
        this.name = pName;
        this.lightLevel = pLightLevel;
        this.particleEmission = pParticleEmission;
        this.spinningMobSpeed = pSpinningMobSpeed;
        this.isCapableOfSpawning = pIsCapableOfSpawning;
    }

    TrialSpawnerState tickAndGetNext(BlockPos pPos, TrialSpawner pSpawner, ServerLevel pLevel) {
        TrialSpawnerData trialspawnerdata = pSpawner.getData();
        TrialSpawnerConfig trialspawnerconfig = pSpawner.getConfig();

        return switch (this) {
            case INACTIVE -> trialspawnerdata.getOrCreateDisplayEntity(pSpawner, pLevel, WAITING_FOR_PLAYERS) == null ? this : WAITING_FOR_PLAYERS;
            case WAITING_FOR_PLAYERS -> {
                if (!pSpawner.canSpawnInLevel(pLevel)) {
                    trialspawnerdata.reset();
                    yield this;
                } else if (!trialspawnerdata.hasMobToSpawn(pSpawner, pLevel.random)) {
                    yield INACTIVE;
                } else {
                    trialspawnerdata.tryDetectPlayers(pLevel, pPos, pSpawner);
                    yield trialspawnerdata.detectedPlayers.isEmpty() ? this : ACTIVE;
                }
            }
            case ACTIVE -> {
                if (!pSpawner.canSpawnInLevel(pLevel)) {
                    trialspawnerdata.reset();
                    yield WAITING_FOR_PLAYERS;
                } else if (!trialspawnerdata.hasMobToSpawn(pSpawner, pLevel.random)) {
                    yield INACTIVE;
                } else {
                    int i = trialspawnerdata.countAdditionalPlayers(pPos);
                    trialspawnerdata.tryDetectPlayers(pLevel, pPos, pSpawner);
                    if (pSpawner.isOminous()) {
                        this.spawnOminousOminousItemSpawner(pLevel, pPos, pSpawner);
                    }

                    if (trialspawnerdata.hasFinishedSpawningAllMobs(trialspawnerconfig, i)) {
                        if (trialspawnerdata.haveAllCurrentMobsDied()) {
                            trialspawnerdata.cooldownEndsAt = pLevel.getGameTime() + (long)pSpawner.getTargetCooldownLength();
                            trialspawnerdata.totalMobsSpawned = 0;
                            trialspawnerdata.nextMobSpawnsAt = 0L;
                            yield WAITING_FOR_REWARD_EJECTION;
                        }
                    } else if (trialspawnerdata.isReadyToSpawnNextMob(pLevel, trialspawnerconfig, i)) {
                        pSpawner.spawnMob(pLevel, pPos).ifPresent(p_340800_ -> {
                            trialspawnerdata.currentMobs.add(p_340800_);
                            trialspawnerdata.totalMobsSpawned++;
                            trialspawnerdata.nextMobSpawnsAt = pLevel.getGameTime() + (long)trialspawnerconfig.ticksBetweenSpawn();
                            trialspawnerconfig.spawnPotentialsDefinition().getRandom(pLevel.getRandom()).ifPresent(p_338048_ -> {
                                trialspawnerdata.nextSpawnData = Optional.of(p_338048_.data());
                                pSpawner.markUpdated();
                            });
                        });
                    }

                    yield this;
                }
            }
            case WAITING_FOR_REWARD_EJECTION -> {
                if (trialspawnerdata.isReadyToOpenShutter(pLevel, 40.0F, pSpawner.getTargetCooldownLength())) {
                    pLevel.playSound(null, pPos, SoundEvents.TRIAL_SPAWNER_OPEN_SHUTTER, SoundSource.BLOCKS);
                    yield EJECTING_REWARD;
                } else {
                    yield this;
                }
            }
            case EJECTING_REWARD -> {
                if (!trialspawnerdata.isReadyToEjectItems(pLevel, (float)TIME_BETWEEN_EACH_EJECTION, pSpawner.getTargetCooldownLength())) {
                    yield this;
                } else if (trialspawnerdata.detectedPlayers.isEmpty()) {
                    pLevel.playSound(null, pPos, SoundEvents.TRIAL_SPAWNER_CLOSE_SHUTTER, SoundSource.BLOCKS);
                    trialspawnerdata.ejectingLootTable = Optional.empty();
                    yield COOLDOWN;
                } else {
                    if (trialspawnerdata.ejectingLootTable.isEmpty()) {
                        trialspawnerdata.ejectingLootTable = trialspawnerconfig.lootTablesToEject().getRandomValue(pLevel.getRandom());
                    }

                    trialspawnerdata.ejectingLootTable.ifPresent(p_335304_ -> pSpawner.ejectReward(pLevel, pPos, (ResourceKey<LootTable>)p_335304_));
                    trialspawnerdata.detectedPlayers.remove(trialspawnerdata.detectedPlayers.iterator().next());
                    yield this;
                }
            }
            case COOLDOWN -> {
                trialspawnerdata.tryDetectPlayers(pLevel, pPos, pSpawner);
                if (!trialspawnerdata.detectedPlayers.isEmpty()) {
                    trialspawnerdata.totalMobsSpawned = 0;
                    trialspawnerdata.nextMobSpawnsAt = 0L;
                    yield ACTIVE;
                } else if (trialspawnerdata.isCooldownFinished(pLevel)) {
                    pSpawner.removeOminous(pLevel, pPos);
                    trialspawnerdata.reset();
                    yield WAITING_FOR_PLAYERS;
                } else {
                    yield this;
                }
            }
        };
    }

    private void spawnOminousOminousItemSpawner(ServerLevel pLevel, BlockPos pPos, TrialSpawner pSpawner) {
        TrialSpawnerData trialspawnerdata = pSpawner.getData();
        TrialSpawnerConfig trialspawnerconfig = pSpawner.getConfig();
        ItemStack itemstack = trialspawnerdata.getDispensingItems(pLevel, trialspawnerconfig, pPos)
            .getRandomValue(pLevel.random)
            .orElse(ItemStack.EMPTY);
        if (!itemstack.isEmpty()) {
            if (this.timeToSpawnItemSpawner(pLevel, trialspawnerdata)) {
                calculatePositionToSpawnSpawner(pLevel, pPos, pSpawner, trialspawnerdata).ifPresent(p_338064_ -> {
                    OminousItemSpawner ominousitemspawner = OminousItemSpawner.create(pLevel, itemstack);
                    ominousitemspawner.moveTo(p_338064_);
                    pLevel.addFreshEntity(ominousitemspawner);
                    float f = (pLevel.getRandom().nextFloat() - pLevel.getRandom().nextFloat()) * 0.2F + 1.0F;
                    pLevel.playSound(null, BlockPos.containing(p_338064_), SoundEvents.TRIAL_SPAWNER_SPAWN_ITEM_BEGIN, SoundSource.BLOCKS, 1.0F, f);
                    trialspawnerdata.cooldownEndsAt = pLevel.getGameTime() + pSpawner.getOminousConfig().ticksBetweenItemSpawners();
                });
            }
        }
    }

    private static Optional<Vec3> calculatePositionToSpawnSpawner(ServerLevel pLevel, BlockPos pPos, TrialSpawner pSpawner, TrialSpawnerData pSpawnerData) {
        List<Player> list = pSpawnerData.detectedPlayers
            .stream()
            .map(pLevel::getPlayerByUUID)
            .filter(Objects::nonNull)
            .filter(
                p_350236_ -> !p_350236_.isCreative()
                        && !p_350236_.isSpectator()
                        && p_350236_.isAlive()
                        && p_350236_.distanceToSqr(pPos.getCenter()) <= (double)Mth.square(pSpawner.getRequiredPlayerRange())
            )
            .toList();
        if (list.isEmpty()) {
            return Optional.empty();
        } else {
            Entity entity = selectEntityToSpawnItemAbove(list, pSpawnerData.currentMobs, pSpawner, pPos, pLevel);
            return entity == null ? Optional.empty() : calculatePositionAbove(entity, pLevel);
        }
    }

    private static Optional<Vec3> calculatePositionAbove(Entity pEntity, ServerLevel pLevel) {
        Vec3 vec3 = pEntity.position();
        Vec3 vec31 = vec3.relative(Direction.UP, (double)(pEntity.getBbHeight() + 2.0F + (float)pLevel.random.nextInt(4)));
        BlockHitResult blockhitresult = pLevel.clip(new ClipContext(vec3, vec31, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty()));
        Vec3 vec32 = blockhitresult.getBlockPos().getCenter().relative(Direction.DOWN, 1.0);
        BlockPos blockpos = BlockPos.containing(vec32);
        return !pLevel.getBlockState(blockpos).getCollisionShape(pLevel, blockpos).isEmpty() ? Optional.empty() : Optional.of(vec32);
    }

    @Nullable
    private static Entity selectEntityToSpawnItemAbove(
        List<Player> pPlayer, Set<UUID> pCurrentMobs, TrialSpawner pSpawner, BlockPos pPos, ServerLevel pLevel
    ) {
        Stream<Entity> stream = pCurrentMobs.stream()
            .map(pLevel::getEntity)
            .filter(Objects::nonNull)
            .filter(
                p_338051_ -> p_338051_.isAlive() && p_338051_.distanceToSqr(pPos.getCenter()) <= (double)Mth.square(pSpawner.getRequiredPlayerRange())
            );
        List<? extends Entity> list = pLevel.random.nextBoolean() ? stream.toList() : pPlayer;
        if (list.isEmpty()) {
            return null;
        } else {
            return list.size() == 1 ? list.getFirst() : Util.getRandom(list, pLevel.random);
        }
    }

    private boolean timeToSpawnItemSpawner(ServerLevel pLevel, TrialSpawnerData pSpawnerData) {
        return pLevel.getGameTime() >= pSpawnerData.cooldownEndsAt;
    }

    public int lightLevel() {
        return this.lightLevel;
    }

    public double spinningMobSpeed() {
        return this.spinningMobSpeed;
    }

    public boolean hasSpinningMob() {
        return this.spinningMobSpeed >= 0.0;
    }

    public boolean isCapableOfSpawning() {
        return this.isCapableOfSpawning;
    }

    public void emitParticles(Level pLevel, BlockPos pPos, boolean pIsOminous) {
        this.particleEmission.emit(pLevel, pLevel.getRandom(), pPos, pIsOminous);
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    static class LightLevel {
        private static final int UNLIT = 0;
        private static final int HALF_LIT = 4;
        private static final int LIT = 8;

        private LightLevel() {
        }
    }

    interface ParticleEmission {
        TrialSpawnerState.ParticleEmission NONE = (p_311998_, p_311983_, p_312351_, p_338371_) -> {
        };
        TrialSpawnerState.ParticleEmission SMALL_FLAMES = (p_338069_, p_338070_, p_338071_, p_338072_) -> {
            if (p_338070_.nextInt(2) == 0) {
                Vec3 vec3 = p_338071_.getCenter().offsetRandom(p_338070_, 0.9F);
                addParticle(p_338072_ ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SMALL_FLAME, vec3, p_338069_);
            }
        };
        TrialSpawnerState.ParticleEmission FLAMES_AND_SMOKE = (p_338065_, p_338066_, p_338067_, p_338068_) -> {
            Vec3 vec3 = p_338067_.getCenter().offsetRandom(p_338066_, 1.0F);
            addParticle(ParticleTypes.SMOKE, vec3, p_338065_);
            addParticle(p_338068_ ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, vec3, p_338065_);
        };
        TrialSpawnerState.ParticleEmission SMOKE_INSIDE_AND_TOP_FACE = (p_311899_, p_311762_, p_312096_, p_338301_) -> {
            Vec3 vec3 = p_312096_.getCenter().offsetRandom(p_311762_, 0.9F);
            if (p_311762_.nextInt(3) == 0) {
                addParticle(ParticleTypes.SMOKE, vec3, p_311899_);
            }

            if (p_311899_.getGameTime() % 20L == 0L) {
                Vec3 vec31 = p_312096_.getCenter().add(0.0, 0.5, 0.0);
                int i = p_311899_.getRandom().nextInt(4) + 20;

                for (int j = 0; j < i; j++) {
                    addParticle(ParticleTypes.SMOKE, vec31, p_311899_);
                }
            }
        };

        private static void addParticle(SimpleParticleType pParticleType, Vec3 pPos, Level pLevel) {
            pLevel.addParticle(pParticleType, pPos.x(), pPos.y(), pPos.z(), 0.0, 0.0, 0.0);
        }

        void emit(Level pLevel, RandomSource pRandom, BlockPos pPos, boolean pIsOminous);
    }

    static class SpinningMob {
        private static final double NONE = -1.0;
        private static final double SLOW = 200.0;
        private static final double FAST = 1000.0;

        private SpinningMob() {
        }
    }
}
