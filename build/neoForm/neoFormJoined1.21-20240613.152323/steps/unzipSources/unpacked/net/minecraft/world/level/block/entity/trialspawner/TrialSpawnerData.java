package net.minecraft.world.level.block.entity.trialspawner;

import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public class TrialSpawnerData {
    public static final String TAG_SPAWN_DATA = "spawn_data";
    private static final String TAG_NEXT_MOB_SPAWNS_AT = "next_mob_spawns_at";
    private static final int DELAY_BETWEEN_PLAYER_SCANS = 20;
    private static final int TRIAL_OMEN_PER_BAD_OMEN_LEVEL = 18000;
    public static MapCodec<TrialSpawnerData> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_312830_ -> p_312830_.group(
                    UUIDUtil.CODEC_SET.lenientOptionalFieldOf("registered_players", Sets.newHashSet()).forGetter(p_312495_ -> p_312495_.detectedPlayers),
                    UUIDUtil.CODEC_SET.lenientOptionalFieldOf("current_mobs", Sets.newHashSet()).forGetter(p_312798_ -> p_312798_.currentMobs),
                    Codec.LONG.lenientOptionalFieldOf("cooldown_ends_at", Long.valueOf(0L)).forGetter(p_312792_ -> p_312792_.cooldownEndsAt),
                    Codec.LONG.lenientOptionalFieldOf("next_mob_spawns_at", Long.valueOf(0L)).forGetter(p_311772_ -> p_311772_.nextMobSpawnsAt),
                    Codec.intRange(0, Integer.MAX_VALUE).lenientOptionalFieldOf("total_mobs_spawned", 0).forGetter(p_312862_ -> p_312862_.totalMobsSpawned),
                    SpawnData.CODEC.lenientOptionalFieldOf("spawn_data").forGetter(p_312634_ -> p_312634_.nextSpawnData),
                    ResourceKey.codec(Registries.LOOT_TABLE).lenientOptionalFieldOf("ejecting_loot_table").forGetter(p_312388_ -> p_312388_.ejectingLootTable)
                )
                .apply(p_312830_, TrialSpawnerData::new)
    );
    protected final Set<UUID> detectedPlayers = new HashSet<>();
    protected final Set<UUID> currentMobs = new HashSet<>();
    protected long cooldownEndsAt;
    protected long nextMobSpawnsAt;
    protected int totalMobsSpawned;
    protected Optional<SpawnData> nextSpawnData;
    protected Optional<ResourceKey<LootTable>> ejectingLootTable;
    @Nullable
    protected Entity displayEntity;
    @Nullable
    private SimpleWeightedRandomList<ItemStack> dispensing;
    protected double spin;
    protected double oSpin;

    public TrialSpawnerData() {
        this(Collections.emptySet(), Collections.emptySet(), 0L, 0L, 0, Optional.empty(), Optional.empty());
    }

    public TrialSpawnerData(
        Set<UUID> p_312283_,
        Set<UUID> p_312919_,
        long p_312537_,
        long p_311955_,
        int p_312227_,
        Optional<SpawnData> p_312562_,
        Optional<ResourceKey<LootTable>> p_312406_
    ) {
        this.detectedPlayers.addAll(p_312283_);
        this.currentMobs.addAll(p_312919_);
        this.cooldownEndsAt = p_312537_;
        this.nextMobSpawnsAt = p_311955_;
        this.totalMobsSpawned = p_312227_;
        this.nextSpawnData = p_312562_;
        this.ejectingLootTable = p_312406_;
    }

    public void reset() {
        this.detectedPlayers.clear();
        this.totalMobsSpawned = 0;
        this.nextMobSpawnsAt = 0L;
        this.cooldownEndsAt = 0L;
        this.currentMobs.clear();
        this.nextSpawnData = Optional.empty();
    }

    public boolean hasMobToSpawn(TrialSpawner pTrialSpawner, RandomSource pRandom) {
        boolean flag = this.getOrCreateNextSpawnData(pTrialSpawner, pRandom).getEntityToSpawn().contains("id", 8);
        return flag || !pTrialSpawner.getConfig().spawnPotentialsDefinition().isEmpty();
    }

    public boolean hasFinishedSpawningAllMobs(TrialSpawnerConfig pConfig, int pPlayers) {
        return this.totalMobsSpawned >= pConfig.calculateTargetTotalMobs(pPlayers);
    }

    public boolean haveAllCurrentMobsDied() {
        return this.currentMobs.isEmpty();
    }

    public boolean isReadyToSpawnNextMob(ServerLevel pLevel, TrialSpawnerConfig pConfig, int pPlayers) {
        return pLevel.getGameTime() >= this.nextMobSpawnsAt && this.currentMobs.size() < pConfig.calculateTargetSimultaneousMobs(pPlayers);
    }

    public int countAdditionalPlayers(BlockPos pPos) {
        if (this.detectedPlayers.isEmpty()) {
            Util.logAndPauseIfInIde("Trial Spawner at " + pPos + " has no detected players");
        }

        return Math.max(0, this.detectedPlayers.size() - 1);
    }

    public void tryDetectPlayers(ServerLevel pLevel, BlockPos pPos, TrialSpawner pSpawner) {
        boolean flag = (pPos.asLong() + pLevel.getGameTime()) % 20L != 0L;
        if (!flag) {
            if (!pSpawner.getState().equals(TrialSpawnerState.COOLDOWN) || !pSpawner.isOminous()) {
                List<UUID> list = pSpawner.getPlayerDetector()
                    .detect(pLevel, pSpawner.getEntitySelector(), pPos, (double)pSpawner.getRequiredPlayerRange(), true);
                boolean flag1;
                if (!pSpawner.isOminous() && !list.isEmpty()) {
                    Optional<Pair<Player, Holder<MobEffect>>> optional = findPlayerWithOminousEffect(pLevel, list);
                    optional.ifPresent(p_350233_ -> {
                        Player player = p_350233_.getFirst();
                        if (p_350233_.getSecond() == MobEffects.BAD_OMEN) {
                            transformBadOmenIntoTrialOmen(player);
                        }

                        pLevel.levelEvent(3020, BlockPos.containing(player.getEyePosition()), 0);
                        pSpawner.applyOminous(pLevel, pPos);
                    });
                    flag1 = optional.isPresent();
                } else {
                    flag1 = false;
                }

                if (!pSpawner.getState().equals(TrialSpawnerState.COOLDOWN) || flag1) {
                    boolean flag2 = pSpawner.getData().detectedPlayers.isEmpty();
                    List<UUID> list1 = flag2
                        ? list
                        : pSpawner.getPlayerDetector()
                            .detect(pLevel, pSpawner.getEntitySelector(), pPos, (double)pSpawner.getRequiredPlayerRange(), false);
                    if (this.detectedPlayers.addAll(list1)) {
                        this.nextMobSpawnsAt = Math.max(pLevel.getGameTime() + 40L, this.nextMobSpawnsAt);
                        if (!flag1) {
                            int i = pSpawner.isOminous() ? 3019 : 3013;
                            pLevel.levelEvent(i, pPos, this.detectedPlayers.size());
                        }
                    }
                }
            }
        }
    }

    private static Optional<Pair<Player, Holder<MobEffect>>> findPlayerWithOminousEffect(ServerLevel p_350655_, List<UUID> p_350735_) {
        Player player = null;

        for (UUID uuid : p_350735_) {
            Player player1 = p_350655_.getPlayerByUUID(uuid);
            if (player1 != null) {
                Holder<MobEffect> holder = MobEffects.TRIAL_OMEN;
                if (player1.hasEffect(holder)) {
                    return Optional.of(Pair.of(player1, holder));
                }

                if (player1.hasEffect(MobEffects.BAD_OMEN)) {
                    player = player1;
                }
            }
        }

        return Optional.ofNullable(player).map(p_350229_ -> Pair.of(p_350229_, MobEffects.BAD_OMEN));
    }

    public void resetAfterBecomingOminous(TrialSpawner pSpawner, ServerLevel pLevel) {
        this.currentMobs.stream().map(pLevel::getEntity).forEach(p_351984_ -> {
            if (p_351984_ != null) {
                pLevel.levelEvent(3012, p_351984_.blockPosition(), TrialSpawner.FlameParticle.NORMAL.encode());
                if (p_351984_ instanceof Mob mob) {
                    mob.dropPreservedEquipment();
                }

                p_351984_.remove(Entity.RemovalReason.DISCARDED);
            }
        });
        if (!pSpawner.getOminousConfig().spawnPotentialsDefinition().isEmpty()) {
            this.nextSpawnData = Optional.empty();
        }

        this.totalMobsSpawned = 0;
        this.currentMobs.clear();
        this.nextMobSpawnsAt = pLevel.getGameTime() + (long)pSpawner.getOminousConfig().ticksBetweenSpawn();
        pSpawner.markUpdated();
        this.cooldownEndsAt = pLevel.getGameTime() + pSpawner.getOminousConfig().ticksBetweenItemSpawners();
    }

    private static void transformBadOmenIntoTrialOmen(Player p_338616_) {
        MobEffectInstance mobeffectinstance = p_338616_.getEffect(MobEffects.BAD_OMEN);
        if (mobeffectinstance != null) {
            int i = mobeffectinstance.getAmplifier() + 1;
            int j = 18000 * i;
            p_338616_.removeEffect(MobEffects.BAD_OMEN);
            p_338616_.addEffect(new MobEffectInstance(MobEffects.TRIAL_OMEN, j, 0));
        }
    }

    public boolean isReadyToOpenShutter(ServerLevel pLevel, float pDelay, int pTargetCooldownLength) {
        long i = this.cooldownEndsAt - (long)pTargetCooldownLength;
        return (float)pLevel.getGameTime() >= (float)i + pDelay;
    }

    public boolean isReadyToEjectItems(ServerLevel pLevel, float pDelay, int pTargetCooldownLength) {
        long i = this.cooldownEndsAt - (long)pTargetCooldownLength;
        return (float)(pLevel.getGameTime() - i) % pDelay == 0.0F;
    }

    public boolean isCooldownFinished(ServerLevel pLevel) {
        return pLevel.getGameTime() >= this.cooldownEndsAt;
    }

    public void setEntityId(TrialSpawner pSpawner, RandomSource pRandom, EntityType<?> pEntityType) {
        this.getOrCreateNextSpawnData(pSpawner, pRandom).getEntityToSpawn().putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(pEntityType).toString());
    }

    protected SpawnData getOrCreateNextSpawnData(TrialSpawner pSpawner, RandomSource pRandom) {
        if (this.nextSpawnData.isPresent()) {
            return this.nextSpawnData.get();
        } else {
            SimpleWeightedRandomList<SpawnData> simpleweightedrandomlist = pSpawner.getConfig().spawnPotentialsDefinition();
            Optional<SpawnData> optional = simpleweightedrandomlist.isEmpty()
                ? this.nextSpawnData
                : simpleweightedrandomlist.getRandom(pRandom).map(WeightedEntry.Wrapper::data);
            this.nextSpawnData = Optional.of(optional.orElseGet(SpawnData::new));
            pSpawner.markUpdated();
            return this.nextSpawnData.get();
        }
    }

    @Nullable
    public Entity getOrCreateDisplayEntity(TrialSpawner pSpawner, Level pLevel, TrialSpawnerState pSpawnerState) {
        if (!pSpawnerState.hasSpinningMob()) {
            return null;
        } else {
            if (this.displayEntity == null) {
                CompoundTag compoundtag = this.getOrCreateNextSpawnData(pSpawner, pLevel.getRandom()).getEntityToSpawn();
                if (compoundtag.contains("id", 8)) {
                    this.displayEntity = EntityType.loadEntityRecursive(compoundtag, pLevel, Function.identity());
                }
            }

            return this.displayEntity;
        }
    }

    public CompoundTag getUpdateTag(TrialSpawnerState pSpawnerState) {
        CompoundTag compoundtag = new CompoundTag();
        if (pSpawnerState == TrialSpawnerState.ACTIVE) {
            compoundtag.putLong("next_mob_spawns_at", this.nextMobSpawnsAt);
        }

        this.nextSpawnData
            .ifPresent(
                p_338045_ -> compoundtag.put(
                        "spawn_data",
                        SpawnData.CODEC.encodeStart(NbtOps.INSTANCE, p_338045_).result().orElseThrow(() -> new IllegalStateException("Invalid SpawnData"))
                    )
            );
        return compoundtag;
    }

    public double getSpin() {
        return this.spin;
    }

    public double getOSpin() {
        return this.oSpin;
    }

    SimpleWeightedRandomList<ItemStack> getDispensingItems(ServerLevel pLevel, TrialSpawnerConfig pConfig, BlockPos pPos) {
        if (this.dispensing != null) {
            return this.dispensing;
        } else {
            LootTable loottable = pLevel.getServer().reloadableRegistries().getLootTable(pConfig.itemsToDropWhenOminous());
            LootParams lootparams = new LootParams.Builder(pLevel).create(LootContextParamSets.EMPTY);
            long i = lowResolutionPosition(pLevel, pPos);
            ObjectArrayList<ItemStack> objectarraylist = loottable.getRandomItems(lootparams, i);
            if (objectarraylist.isEmpty()) {
                return SimpleWeightedRandomList.empty();
            } else {
                SimpleWeightedRandomList.Builder<ItemStack> builder = new SimpleWeightedRandomList.Builder<>();

                for (ItemStack itemstack : objectarraylist) {
                    builder.add(itemstack.copyWithCount(1), itemstack.getCount());
                }

                this.dispensing = builder.build();
                return this.dispensing;
            }
        }
    }

    private static long lowResolutionPosition(ServerLevel pLevel, BlockPos pPos) {
        BlockPos blockpos = new BlockPos(
            Mth.floor((float)pPos.getX() / 30.0F), Mth.floor((float)pPos.getY() / 20.0F), Mth.floor((float)pPos.getZ() / 30.0F)
        );
        return pLevel.getSeed() + blockpos.asLong();
    }
}
