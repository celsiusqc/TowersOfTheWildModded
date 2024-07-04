package net.minecraft.world.level.storage;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.DataResult.Error;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReportCategory;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.timers.TimerCallbacks;
import net.minecraft.world.level.timers.TimerQueue;
import org.slf4j.Logger;

public class PrimaryLevelData implements ServerLevelData, WorldData {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String LEVEL_NAME = "LevelName";
    protected static final String PLAYER = "Player";
    protected static final String WORLD_GEN_SETTINGS = "WorldGenSettings";
    private LevelSettings settings;
    private final WorldOptions worldOptions;
    private final PrimaryLevelData.SpecialWorldProperty specialWorldProperty;
    private final Lifecycle worldGenSettingsLifecycle;
    private BlockPos spawnPos;
    private float spawnAngle;
    private long gameTime;
    private long dayTime;
    @Nullable
    private final CompoundTag loadedPlayerTag;
    private final int version;
    private int clearWeatherTime;
    private boolean raining;
    private int rainTime;
    private boolean thundering;
    private int thunderTime;
    private boolean initialized;
    private boolean difficultyLocked;
    private WorldBorder.Settings worldBorder;
    private EndDragonFight.Data endDragonFightData;
    @Nullable
    private CompoundTag customBossEvents;
    private int wanderingTraderSpawnDelay;
    private int wanderingTraderSpawnChance;
    @Nullable
    private UUID wanderingTraderId;
    private final Set<String> knownServerBrands;
    private boolean wasModded;
    private final Set<String> removedFeatureFlags;
    private final TimerQueue<MinecraftServer> scheduledEvents;
    private boolean confirmedExperimentalWarning = false;

    private PrimaryLevelData(
        @Nullable CompoundTag pLoadedPlayerTag,
        boolean pWasModded,
        BlockPos pSpawnPos,
        float pSpawnAngle,
        long pGameTime,
        long pDayTime,
        int pVersion,
        int pClearWeatherTime,
        int pRainTime,
        boolean pRaining,
        int pThunderTime,
        boolean pThundering,
        boolean pInitialized,
        boolean pDifficultyLocked,
        WorldBorder.Settings pWorldBorder,
        int pWanderingTraderSpawnDelay,
        int pWanderingTraderSpawnChance,
        @Nullable UUID pWanderingTraderId,
        Set<String> pKnownServerBrands,
        Set<String> pRemovedFeatureFlags,
        TimerQueue<MinecraftServer> pScheduledEvents,
        @Nullable CompoundTag pCustomBossEvents,
        EndDragonFight.Data pEndDragonFightData,
        LevelSettings pSettings,
        WorldOptions pWorldOptions,
        PrimaryLevelData.SpecialWorldProperty pSpecialWorldProperty,
        Lifecycle pWorldGenSettingsLifecycle
    ) {
        this.wasModded = pWasModded;
        this.spawnPos = pSpawnPos;
        this.spawnAngle = pSpawnAngle;
        this.gameTime = pGameTime;
        this.dayTime = pDayTime;
        this.version = pVersion;
        this.clearWeatherTime = pClearWeatherTime;
        this.rainTime = pRainTime;
        this.raining = pRaining;
        this.thunderTime = pThunderTime;
        this.thundering = pThundering;
        this.initialized = pInitialized;
        this.difficultyLocked = pDifficultyLocked;
        this.worldBorder = pWorldBorder;
        this.wanderingTraderSpawnDelay = pWanderingTraderSpawnDelay;
        this.wanderingTraderSpawnChance = pWanderingTraderSpawnChance;
        this.wanderingTraderId = pWanderingTraderId;
        this.knownServerBrands = pKnownServerBrands;
        this.removedFeatureFlags = pRemovedFeatureFlags;
        this.loadedPlayerTag = pLoadedPlayerTag;
        this.scheduledEvents = pScheduledEvents;
        this.customBossEvents = pCustomBossEvents;
        this.endDragonFightData = pEndDragonFightData;
        this.settings = pSettings;
        this.worldOptions = pWorldOptions;
        this.specialWorldProperty = pSpecialWorldProperty;
        this.worldGenSettingsLifecycle = pWorldGenSettingsLifecycle;
    }

    public PrimaryLevelData(LevelSettings pSettings, WorldOptions pWorldOptions, PrimaryLevelData.SpecialWorldProperty pSpecialWorldProperty, Lifecycle pWorldGenSettingsLifecycle) {
        this(
            null,
            false,
            BlockPos.ZERO,
            0.0F,
            0L,
            0L,
            19133,
            0,
            0,
            false,
            0,
            false,
            false,
            false,
            WorldBorder.DEFAULT_SETTINGS,
            0,
            0,
            null,
            Sets.newLinkedHashSet(),
            new HashSet<>(),
            new TimerQueue<>(TimerCallbacks.SERVER_CALLBACKS),
            null,
            EndDragonFight.Data.DEFAULT,
            pSettings.copy(),
            pWorldOptions,
            pSpecialWorldProperty,
            pWorldGenSettingsLifecycle
        );
    }

    public static <T> PrimaryLevelData parse(
        Dynamic<T> pTag, LevelSettings pLevelSettings, PrimaryLevelData.SpecialWorldProperty pSpecialWorldProperty, WorldOptions pWorldOptions, Lifecycle pWorldGenSettingsLifecycle
    ) {
        long i = pTag.get("Time").asLong(0L);
        return new PrimaryLevelData(
            pTag.get("Player").flatMap(CompoundTag.CODEC::parse).result().orElse(null),
            pTag.get("WasModded").asBoolean(false),
            new BlockPos(pTag.get("SpawnX").asInt(0), pTag.get("SpawnY").asInt(0), pTag.get("SpawnZ").asInt(0)),
            pTag.get("SpawnAngle").asFloat(0.0F),
            i,
            pTag.get("DayTime").asLong(i),
            LevelVersion.parse(pTag).levelDataVersion(),
            pTag.get("clearWeatherTime").asInt(0),
            pTag.get("rainTime").asInt(0),
            pTag.get("raining").asBoolean(false),
            pTag.get("thunderTime").asInt(0),
            pTag.get("thundering").asBoolean(false),
            pTag.get("initialized").asBoolean(true),
            pTag.get("DifficultyLocked").asBoolean(false),
            WorldBorder.Settings.read(pTag, WorldBorder.DEFAULT_SETTINGS),
            pTag.get("WanderingTraderSpawnDelay").asInt(0),
            pTag.get("WanderingTraderSpawnChance").asInt(0),
            pTag.get("WanderingTraderId").read(UUIDUtil.CODEC).result().orElse(null),
            pTag.get("ServerBrands")
                .asStream()
                .flatMap(p_338118_ -> p_338118_.asString().result().stream())
                .collect(Collectors.toCollection(Sets::newLinkedHashSet)),
            pTag.get("removed_features").asStream().flatMap(p_338117_ -> p_338117_.asString().result().stream()).collect(Collectors.toSet()),
            new TimerQueue<>(TimerCallbacks.SERVER_CALLBACKS, pTag.get("ScheduledEvents").asStream()),
            (CompoundTag)pTag.get("CustomBossEvents").orElseEmptyMap().getValue(),
            pTag.get("DragonFight").read(EndDragonFight.Data.CODEC).resultOrPartial(LOGGER::error).orElse(EndDragonFight.Data.DEFAULT),
            pLevelSettings,
            pWorldOptions,
            pSpecialWorldProperty,
            pWorldGenSettingsLifecycle
        ).withConfirmedWarning(pWorldGenSettingsLifecycle != Lifecycle.stable() && pTag.get("confirmedExperimentalSettings").asBoolean(false));
    }

    @Override
    public CompoundTag createTag(RegistryAccess pRegistries, @Nullable CompoundTag pHostPlayerNBT) {
        if (pHostPlayerNBT == null) {
            pHostPlayerNBT = this.loadedPlayerTag;
        }

        CompoundTag compoundtag = new CompoundTag();
        this.setTagData(pRegistries, compoundtag, pHostPlayerNBT);
        return compoundtag;
    }

    private void setTagData(RegistryAccess pRegistry, CompoundTag pNbt, @Nullable CompoundTag pPlayerNBT) {
        pNbt.put("ServerBrands", stringCollectionToTag(this.knownServerBrands));
        pNbt.putBoolean("WasModded", this.wasModded);
        if (!this.removedFeatureFlags.isEmpty()) {
            pNbt.put("removed_features", stringCollectionToTag(this.removedFeatureFlags));
        }

        CompoundTag compoundtag = new CompoundTag();
        compoundtag.putString("Name", SharedConstants.getCurrentVersion().getName());
        compoundtag.putInt("Id", SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        compoundtag.putBoolean("Snapshot", !SharedConstants.getCurrentVersion().isStable());
        compoundtag.putString("Series", SharedConstants.getCurrentVersion().getDataVersion().getSeries());
        pNbt.put("Version", compoundtag);
        NbtUtils.addCurrentDataVersion(pNbt);
        DynamicOps<Tag> dynamicops = pRegistry.createSerializationContext(NbtOps.INSTANCE);
        WorldGenSettings.encode(dynamicops, this.worldOptions, pRegistry)
            .resultOrPartial(Util.prefix("WorldGenSettings: ", LOGGER::error))
            .ifPresent(p_78574_ -> pNbt.put("WorldGenSettings", p_78574_));
        pNbt.putInt("GameType", this.settings.gameType().getId());
        pNbt.putInt("SpawnX", this.spawnPos.getX());
        pNbt.putInt("SpawnY", this.spawnPos.getY());
        pNbt.putInt("SpawnZ", this.spawnPos.getZ());
        pNbt.putFloat("SpawnAngle", this.spawnAngle);
        pNbt.putLong("Time", this.gameTime);
        pNbt.putLong("DayTime", this.dayTime);
        pNbt.putLong("LastPlayed", Util.getEpochMillis());
        pNbt.putString("LevelName", this.settings.levelName());
        pNbt.putInt("version", 19133);
        pNbt.putInt("clearWeatherTime", this.clearWeatherTime);
        pNbt.putInt("rainTime", this.rainTime);
        pNbt.putBoolean("raining", this.raining);
        pNbt.putInt("thunderTime", this.thunderTime);
        pNbt.putBoolean("thundering", this.thundering);
        pNbt.putBoolean("hardcore", this.settings.hardcore());
        pNbt.putBoolean("allowCommands", this.settings.allowCommands());
        pNbt.putBoolean("initialized", this.initialized);
        this.worldBorder.write(pNbt);
        pNbt.putByte("Difficulty", (byte)this.settings.difficulty().getId());
        pNbt.putBoolean("DifficultyLocked", this.difficultyLocked);
        pNbt.put("GameRules", this.settings.gameRules().createTag());
        pNbt.put("DragonFight", EndDragonFight.Data.CODEC.encodeStart(NbtOps.INSTANCE, this.endDragonFightData).getOrThrow());
        if (pPlayerNBT != null) {
            pNbt.put("Player", pPlayerNBT);
        }

        WorldDataConfiguration.CODEC
            .encodeStart(NbtOps.INSTANCE, this.settings.getDataConfiguration())
            .ifSuccess(p_248505_ -> pNbt.merge((CompoundTag)p_248505_))
            .ifError(p_338116_ -> LOGGER.warn("Failed to encode configuration {}", p_338116_.message()));
        if (this.customBossEvents != null) {
            pNbt.put("CustomBossEvents", this.customBossEvents);
        }

        pNbt.put("ScheduledEvents", this.scheduledEvents.store());
        pNbt.putInt("WanderingTraderSpawnDelay", this.wanderingTraderSpawnDelay);
        pNbt.putInt("WanderingTraderSpawnChance", this.wanderingTraderSpawnChance);
        if (this.wanderingTraderId != null) {
            pNbt.putUUID("WanderingTraderId", this.wanderingTraderId);
        }
        pNbt.putString("forgeLifecycle", net.neoforged.neoforge.common.CommonHooks.encodeLifecycle(this.settings.getLifecycle()));
        pNbt.putBoolean("confirmedExperimentalSettings", this.confirmedExperimentalWarning);
    }

    private static ListTag stringCollectionToTag(Set<String> pStringCollection) {
        ListTag listtag = new ListTag();
        pStringCollection.stream().map(StringTag::valueOf).forEach(listtag::add);
        return listtag;
    }

    @Override
    public BlockPos getSpawnPos() {
        return this.spawnPos;
    }

    @Override
    public float getSpawnAngle() {
        return this.spawnAngle;
    }

    @Override
    public long getGameTime() {
        return this.gameTime;
    }

    @Override
    public long getDayTime() {
        return this.dayTime;
    }

    @Nullable
    @Override
    public CompoundTag getLoadedPlayerTag() {
        return this.loadedPlayerTag;
    }

    @Override
    public void setGameTime(long pTime) {
        this.gameTime = pTime;
    }

    /**
     * Set current world time
     */
    @Override
    public void setDayTime(long pTime) {
        this.dayTime = pTime;
    }

    @Override
    public void setSpawn(BlockPos pSpawnPoint, float pAngle) {
        this.spawnPos = pSpawnPoint.immutable();
        this.spawnAngle = pAngle;
    }

    @Override
    public String getLevelName() {
        return this.settings.levelName();
    }

    @Override
    public int getVersion() {
        return this.version;
    }

    @Override
    public int getClearWeatherTime() {
        return this.clearWeatherTime;
    }

    @Override
    public void setClearWeatherTime(int pTime) {
        this.clearWeatherTime = pTime;
    }

    @Override
    public boolean isThundering() {
        return this.thundering;
    }

    /**
     * Sets whether it is thundering or not.
     */
    @Override
    public void setThundering(boolean pThundering) {
        this.thundering = pThundering;
    }

    @Override
    public int getThunderTime() {
        return this.thunderTime;
    }

    /**
     * Defines the number of ticks until next thunderbolt.
     */
    @Override
    public void setThunderTime(int pTime) {
        this.thunderTime = pTime;
    }

    @Override
    public boolean isRaining() {
        return this.raining;
    }

    /**
     * Sets whether it is raining or not.
     */
    @Override
    public void setRaining(boolean pIsRaining) {
        this.raining = pIsRaining;
    }

    @Override
    public int getRainTime() {
        return this.rainTime;
    }

    /**
     * Sets the number of ticks until rain.
     */
    @Override
    public void setRainTime(int pTime) {
        this.rainTime = pTime;
    }

    @Override
    public GameType getGameType() {
        return this.settings.gameType();
    }

    @Override
    public void setGameType(GameType pType) {
        this.settings = this.settings.withGameType(pType);
    }

    @Override
    public boolean isHardcore() {
        return this.settings.hardcore();
    }

    @Override
    public boolean isAllowCommands() {
        return this.settings.allowCommands();
    }

    @Override
    public boolean isInitialized() {
        return this.initialized;
    }

    /**
     * Sets the initialization status of the World.
     */
    @Override
    public void setInitialized(boolean pInitialized) {
        this.initialized = pInitialized;
    }

    @Override
    public GameRules getGameRules() {
        return this.settings.gameRules();
    }

    @Override
    public WorldBorder.Settings getWorldBorder() {
        return this.worldBorder;
    }

    @Override
    public void setWorldBorder(WorldBorder.Settings pSerializer) {
        this.worldBorder = pSerializer;
    }

    @Override
    public Difficulty getDifficulty() {
        return this.settings.difficulty();
    }

    @Override
    public void setDifficulty(Difficulty pDifficulty) {
        this.settings = this.settings.withDifficulty(pDifficulty);
    }

    @Override
    public boolean isDifficultyLocked() {
        return this.difficultyLocked;
    }

    @Override
    public void setDifficultyLocked(boolean pLocked) {
        this.difficultyLocked = pLocked;
    }

    @Override
    public TimerQueue<MinecraftServer> getScheduledEvents() {
        return this.scheduledEvents;
    }

    @Override
    public void fillCrashReportCategory(CrashReportCategory pCrashReportCategory, LevelHeightAccessor pLevel) {
        ServerLevelData.super.fillCrashReportCategory(pCrashReportCategory, pLevel);
        WorldData.super.fillCrashReportCategory(pCrashReportCategory);
    }

    @Override
    public WorldOptions worldGenOptions() {
        return this.worldOptions;
    }

    @Override
    public boolean isFlatWorld() {
        return this.specialWorldProperty == PrimaryLevelData.SpecialWorldProperty.FLAT;
    }

    @Override
    public boolean isDebugWorld() {
        return this.specialWorldProperty == PrimaryLevelData.SpecialWorldProperty.DEBUG;
    }

    @Override
    public Lifecycle worldGenSettingsLifecycle() {
        return this.worldGenSettingsLifecycle;
    }

    @Override
    public EndDragonFight.Data endDragonFightData() {
        return this.endDragonFightData;
    }

    @Override
    public void setEndDragonFightData(EndDragonFight.Data pEndDragonFightData) {
        this.endDragonFightData = pEndDragonFightData;
    }

    @Override
    public WorldDataConfiguration getDataConfiguration() {
        return this.settings.getDataConfiguration();
    }

    @Override
    public void setDataConfiguration(WorldDataConfiguration pDataConfiguration) {
        this.settings = this.settings.withDataConfiguration(pDataConfiguration);
    }

    @Nullable
    @Override
    public CompoundTag getCustomBossEvents() {
        return this.customBossEvents;
    }

    @Override
    public void setCustomBossEvents(@Nullable CompoundTag pNbt) {
        this.customBossEvents = pNbt;
    }

    @Override
    public int getWanderingTraderSpawnDelay() {
        return this.wanderingTraderSpawnDelay;
    }

    @Override
    public void setWanderingTraderSpawnDelay(int pDelay) {
        this.wanderingTraderSpawnDelay = pDelay;
    }

    @Override
    public int getWanderingTraderSpawnChance() {
        return this.wanderingTraderSpawnChance;
    }

    @Override
    public void setWanderingTraderSpawnChance(int pChance) {
        this.wanderingTraderSpawnChance = pChance;
    }

    @Nullable
    @Override
    public UUID getWanderingTraderId() {
        return this.wanderingTraderId;
    }

    @Override
    public void setWanderingTraderId(UUID pId) {
        this.wanderingTraderId = pId;
    }

    @Override
    public void setModdedInfo(String pName, boolean pIsModded) {
        this.knownServerBrands.add(pName);
        this.wasModded |= pIsModded;
    }

    @Override
    public boolean wasModded() {
        return this.wasModded;
    }

    @Override
    public Set<String> getKnownServerBrands() {
        return ImmutableSet.copyOf(this.knownServerBrands);
    }

    @Override
    public Set<String> getRemovedFeatureFlags() {
        return Set.copyOf(this.removedFeatureFlags);
    }

    @Override
    public ServerLevelData overworldData() {
        return this;
    }

    @Override
    public LevelSettings getLevelSettings() {
        return this.settings.copy();
    }

    public boolean hasConfirmedExperimentalWarning() {
        return this.confirmedExperimentalWarning;
    }

    public PrimaryLevelData withConfirmedWarning(boolean confirmedWarning) { // Builder-like to not patch ctor
        this.confirmedExperimentalWarning = confirmedWarning;
        return this;
    }

    @Deprecated
    public static enum SpecialWorldProperty {
        NONE,
        FLAT,
        DEBUG;
    }
}
