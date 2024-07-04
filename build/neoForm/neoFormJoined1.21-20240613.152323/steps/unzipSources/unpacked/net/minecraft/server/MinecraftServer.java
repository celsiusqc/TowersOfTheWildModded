package net.minecraft.server;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.Proxy;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.FileUtil;
import net.minecraft.ReportType;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.SystemReport;
import net.minecraft.Util;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.MiscOverworldFeatures;
import net.minecraft.gametest.framework.GameTestTicker;
import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.bossevents.CustomBossEvents;
import net.minecraft.server.level.DemoMode;
import net.minecraft.server.level.PlayerRespawnLogic;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.network.ServerConnectionListener;
import net.minecraft.server.network.TextFilter;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;
import net.minecraft.util.ModCheck;
import net.minecraft.util.Mth;
import net.minecraft.util.NativeModuleLister;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SignatureValidator;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.debugchart.RemoteDebugSampleType;
import net.minecraft.util.debugchart.SampleLogger;
import net.minecraft.util.debugchart.TpsDebugDimensions;
import net.minecraft.util.profiling.EmptyProfileResults;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.ResultField;
import net.minecraft.util.profiling.SingleTickProfiler;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.profiling.jfr.callback.ProfiledDuration;
import net.minecraft.util.profiling.metrics.profiling.ActiveMetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.InactiveMetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.MetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.ServerMetricsSamplersProvider;
import net.minecraft.util.profiling.metrics.storage.MetricsPersister;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.Difficulty;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.entity.ai.village.VillageSiege;
import net.minecraft.world.entity.npc.CatSpawner;
import net.minecraft.world.entity.npc.WanderingTraderSpawner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.storage.ChunkIOErrorReporter;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.PatrolSpawner;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.CommandStorage;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public abstract class MinecraftServer extends ReentrantBlockableEventLoop<TickTask> implements ServerInfo, ChunkIOErrorReporter, CommandSource, AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String VANILLA_BRAND = "vanilla";
    private static final float AVERAGE_TICK_TIME_SMOOTHING = 0.8F;
    private static final int TICK_STATS_SPAN = 100;
    private static final long OVERLOADED_THRESHOLD_NANOS = 20L * TimeUtil.NANOSECONDS_PER_SECOND / 20L;
    private static final int OVERLOADED_TICKS_THRESHOLD = 20;
    private static final long OVERLOADED_WARNING_INTERVAL_NANOS = 10L * TimeUtil.NANOSECONDS_PER_SECOND;
    private static final int OVERLOADED_TICKS_WARNING_INTERVAL = 100;
    private static final long STATUS_EXPIRE_TIME_NANOS = 5L * TimeUtil.NANOSECONDS_PER_SECOND;
    private static final long PREPARE_LEVELS_DEFAULT_DELAY_NANOS = 10L * TimeUtil.NANOSECONDS_PER_MILLISECOND;
    private static final int MAX_STATUS_PLAYER_SAMPLE = 12;
    private static final int SPAWN_POSITION_SEARCH_RADIUS = 5;
    private static final int AUTOSAVE_INTERVAL = 6000;
    private static final int MIMINUM_AUTOSAVE_TICKS = 100;
    private static final int MAX_TICK_LATENCY = 3;
    public static final int ABSOLUTE_MAX_WORLD_SIZE = 29999984;
    public static final LevelSettings DEMO_SETTINGS = new LevelSettings(
        "Demo World", GameType.SURVIVAL, false, Difficulty.NORMAL, false, new GameRules(), WorldDataConfiguration.DEFAULT
    );
    public static final GameProfile ANONYMOUS_PLAYER_PROFILE = new GameProfile(Util.NIL_UUID, "Anonymous Player");
    protected final LevelStorageSource.LevelStorageAccess storageSource;
    protected final PlayerDataStorage playerDataStorage;
    private final List<Runnable> tickables = Lists.newArrayList();
    private MetricsRecorder metricsRecorder = InactiveMetricsRecorder.INSTANCE;
    private ProfilerFiller profiler = this.metricsRecorder.getProfiler();
    private Consumer<ProfileResults> onMetricsRecordingStopped = p_177903_ -> this.stopRecordingMetrics();
    private Consumer<Path> onMetricsRecordingFinished = p_177954_ -> {
    };
    private boolean willStartRecordingMetrics;
    @Nullable
    private MinecraftServer.TimeProfiler debugCommandProfiler;
    private boolean debugCommandProfilerDelayStart;
    private final ServerConnectionListener connection;
    private final ChunkProgressListenerFactory progressListenerFactory;
    @Nullable
    private ServerStatus status;
    @Nullable
    private ServerStatus.Favicon statusIcon;
    private final RandomSource random = RandomSource.create();
    private final DataFixer fixerUpper;
    private String localIp;
    private int port = -1;
    private final LayeredRegistryAccess<RegistryLayer> registries;
    private final Map<ResourceKey<Level>, ServerLevel> levels = Maps.newLinkedHashMap();
    private PlayerList playerList;
    private volatile boolean running = true;
    private boolean stopped;
    private int tickCount;
    private int ticksUntilAutosave = 6000;
    protected final Proxy proxy;
    private boolean onlineMode;
    private boolean preventProxyConnections;
    private boolean pvp;
    private boolean allowFlight;
    @Nullable
    private String motd;
    private int playerIdleTimeout;
    private final long[] tickTimesNanos = new long[100];
    private long aggregatedTickTimesNanos = 0L;
    @Nullable
    private KeyPair keyPair;
    @Nullable
    private GameProfile singleplayerProfile;
    private boolean isDemo;
    private volatile boolean isReady;
    private long lastOverloadWarningNanos;
    protected final Services services;
    private long lastServerStatus;
    private final Thread serverThread;
    private long lastTickNanos = Util.getNanos();
    private long taskExecutionStartNanos = Util.getNanos();
    private long idleTimeNanos;
    protected long nextTickTimeNanos = Util.getNanos();
    private long delayedTasksMaxNextTickTimeNanos;
    private boolean mayHaveDelayedTasks;
    private final PackRepository packRepository;
    private final ServerScoreboard scoreboard = new ServerScoreboard(this);
    @Nullable
    private CommandStorage commandStorage;
    private final CustomBossEvents customBossEvents = new CustomBossEvents();
    private final ServerFunctionManager functionManager;
    private boolean enforceWhitelist;
    private float smoothedTickTimeMillis;
    private final Executor executor;
    @Nullable
    private String serverId;
    private MinecraftServer.ReloadableResources resources;
    private final StructureTemplateManager structureTemplateManager;
    private final ServerTickRateManager tickRateManager;
    protected final WorldData worldData;
    private final PotionBrewing potionBrewing;
    private volatile boolean isSaving;
    private static final AtomicReference<RuntimeException> fatalException = new AtomicReference<>();

    public static <S extends MinecraftServer> S spin(Function<Thread, S> pThreadFunction) {
        AtomicReference<S> atomicreference = new AtomicReference<>();
        Thread thread = new Thread(net.neoforged.fml.util.thread.SidedThreadGroups.SERVER, () -> atomicreference.get().runServer(), "Server thread");
        thread.setUncaughtExceptionHandler((p_177909_, p_177910_) -> LOGGER.error("Uncaught exception in server thread", p_177910_));
        if (Runtime.getRuntime().availableProcessors() > 4) {
            thread.setPriority(8);
        }

        S s = (S)pThreadFunction.apply(thread);
        atomicreference.set(s);
        thread.start();
        return s;
    }

    public MinecraftServer(
        Thread pServerThread,
        LevelStorageSource.LevelStorageAccess pStorageSource,
        PackRepository pPackRepository,
        WorldStem pWorldStem,
        Proxy pProxy,
        DataFixer pFixerUpper,
        Services pServices,
        ChunkProgressListenerFactory pProgressListenerFactory
    ) {
        super("Server");
        this.registries = pWorldStem.registries();
        this.worldData = pWorldStem.worldData();
        if (!this.registries.compositeAccess().registryOrThrow(Registries.LEVEL_STEM).containsKey(LevelStem.OVERWORLD)) {
            throw new IllegalStateException("Missing Overworld dimension data");
        } else {
            this.proxy = pProxy;
            this.packRepository = pPackRepository;
            this.resources = new MinecraftServer.ReloadableResources(pWorldStem.resourceManager(), pWorldStem.dataPackResources());
            this.services = pServices;
            if (pServices.profileCache() != null) {
                pServices.profileCache().setExecutor(this);
            }

            this.connection = new ServerConnectionListener(this);
            this.tickRateManager = new ServerTickRateManager(this);
            this.progressListenerFactory = pProgressListenerFactory;
            this.storageSource = pStorageSource;
            this.playerDataStorage = pStorageSource.createPlayerStorage();
            this.fixerUpper = pFixerUpper;
            this.functionManager = new ServerFunctionManager(this, this.resources.managers.getFunctionLibrary());
            HolderGetter<Block> holdergetter = this.registries
                .compositeAccess()
                .registryOrThrow(Registries.BLOCK)
                .asLookup()
                .filterFeatures(this.worldData.enabledFeatures());
            this.structureTemplateManager = new StructureTemplateManager(pWorldStem.resourceManager(), pStorageSource, pFixerUpper, holdergetter);
            this.serverThread = pServerThread;
            this.executor = Util.backgroundExecutor();
            this.potionBrewing = PotionBrewing.bootstrap(this.worldData.enabledFeatures());
        }
    }

    private void readScoreboard(DimensionDataStorage pDataStorage) {
        pDataStorage.computeIfAbsent(this.getScoreboard().dataFactory(), "scoreboard");
    }

    protected abstract boolean initServer() throws IOException;

    protected void loadLevel() {
        if (!JvmProfiler.INSTANCE.isRunning()) {
        }

        boolean flag = false;
        ProfiledDuration profiledduration = JvmProfiler.INSTANCE.onWorldLoadedStarted();
        this.worldData.setModdedInfo(this.getServerModName(), this.getModdedStatus().shouldReportAsModified());
        ChunkProgressListener chunkprogresslistener = this.progressListenerFactory
            .create(this.worldData.getGameRules().getInt(GameRules.RULE_SPAWN_CHUNK_RADIUS));
        this.createLevels(chunkprogresslistener);
        this.forceDifficulty();
        this.prepareLevels(chunkprogresslistener);
        if (profiledduration != null) {
            profiledduration.finish();
        }

        if (flag) {
            try {
                JvmProfiler.INSTANCE.stop();
            } catch (Throwable throwable) {
                LOGGER.warn("Failed to stop JFR profiling", throwable);
            }
        }
    }

    protected void forceDifficulty() {
    }

    protected void createLevels(ChunkProgressListener pListener) {
        ServerLevelData serverleveldata = this.worldData.overworldData();
        boolean flag = this.worldData.isDebugWorld();
        Registry<LevelStem> registry = this.registries.compositeAccess().registryOrThrow(Registries.LEVEL_STEM);
        WorldOptions worldoptions = this.worldData.worldGenOptions();
        long i = worldoptions.seed();
        long j = BiomeManager.obfuscateSeed(i);
        List<CustomSpawner> list = ImmutableList.of(
            new PhantomSpawner(), new PatrolSpawner(), new CatSpawner(), new VillageSiege(), new WanderingTraderSpawner(serverleveldata)
        );
        LevelStem levelstem = registry.get(LevelStem.OVERWORLD);
        ServerLevel serverlevel = new ServerLevel(
            this, this.executor, this.storageSource, serverleveldata, Level.OVERWORLD, levelstem, pListener, flag, j, list, true, null
        );
        this.levels.put(Level.OVERWORLD, serverlevel);
        DimensionDataStorage dimensiondatastorage = serverlevel.getDataStorage();
        this.readScoreboard(dimensiondatastorage);
        this.commandStorage = new CommandStorage(dimensiondatastorage);
        WorldBorder worldborder = serverlevel.getWorldBorder();
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.level.LevelEvent.Load(levels.get(Level.OVERWORLD)));
        if (!serverleveldata.isInitialized()) {
            try {
                setInitialSpawn(serverlevel, serverleveldata, worldoptions.generateBonusChest(), flag);
                serverleveldata.setInitialized(true);
                if (flag) {
                    this.setupDebugLevel(this.worldData);
                }
            } catch (Throwable throwable1) {
                CrashReport crashreport = CrashReport.forThrowable(throwable1, "Exception initializing level");

                try {
                    serverlevel.fillReportDetails(crashreport);
                } catch (Throwable throwable) {
                }

                throw new ReportedException(crashreport);
            }

            serverleveldata.setInitialized(true);
        }

        this.getPlayerList().addWorldborderListener(serverlevel);
        if (this.worldData.getCustomBossEvents() != null) {
            this.getCustomBossEvents().load(this.worldData.getCustomBossEvents(), this.registryAccess());
        }

        RandomSequences randomsequences = serverlevel.getRandomSequences();

        for (Entry<ResourceKey<LevelStem>, LevelStem> entry : registry.entrySet()) {
            ResourceKey<LevelStem> resourcekey = entry.getKey();
            if (resourcekey != LevelStem.OVERWORLD) {
                ResourceKey<Level> resourcekey1 = ResourceKey.create(Registries.DIMENSION, resourcekey.location());
                DerivedLevelData derivedleveldata = new DerivedLevelData(this.worldData, serverleveldata);
                ServerLevel serverlevel1 = new ServerLevel(
                    this,
                    this.executor,
                    this.storageSource,
                    derivedleveldata,
                    resourcekey1,
                    entry.getValue(),
                    pListener,
                    flag,
                    j,
                    ImmutableList.of(),
                    false,
                    randomsequences
                );
                worldborder.addListener(new BorderChangeListener.DelegateBorderChangeListener(serverlevel1.getWorldBorder()));
                this.levels.put(resourcekey1, serverlevel1);
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.level.LevelEvent.Load(levels.get(resourcekey)));
            }
        }

        worldborder.applySettings(serverleveldata.getWorldBorder());
    }

    private static void setInitialSpawn(ServerLevel pLevel, ServerLevelData pLevelData, boolean pGenerateBonusChest, boolean pDebug) {
        if (pDebug) {
            pLevelData.setSpawn(BlockPos.ZERO.above(80), 0.0F);
        } else {
            ServerChunkCache serverchunkcache = pLevel.getChunkSource();
            if (net.neoforged.neoforge.event.EventHooks.onCreateWorldSpawn(pLevel, pLevelData)) return;
            ChunkPos chunkpos = new ChunkPos(serverchunkcache.randomState().sampler().findSpawnPosition());
            int i = serverchunkcache.getGenerator().getSpawnHeight(pLevel);
            if (i < pLevel.getMinBuildHeight()) {
                BlockPos blockpos = chunkpos.getWorldPosition();
                i = pLevel.getHeight(Heightmap.Types.WORLD_SURFACE, blockpos.getX() + 8, blockpos.getZ() + 8);
            }

            pLevelData.setSpawn(chunkpos.getWorldPosition().offset(8, i, 8), 0.0F);
            int j1 = 0;
            int j = 0;
            int k = 0;
            int l = -1;

            for (int i1 = 0; i1 < Mth.square(11); i1++) {
                if (j1 >= -5 && j1 <= 5 && j >= -5 && j <= 5) {
                    BlockPos blockpos1 = PlayerRespawnLogic.getSpawnPosInChunk(pLevel, new ChunkPos(chunkpos.x + j1, chunkpos.z + j));
                    if (blockpos1 != null) {
                        pLevelData.setSpawn(blockpos1, 0.0F);
                        break;
                    }
                }

                if (j1 == j || j1 < 0 && j1 == -j || j1 > 0 && j1 == 1 - j) {
                    int k1 = k;
                    k = -l;
                    l = k1;
                }

                j1 += k;
                j += l;
            }

            if (pGenerateBonusChest) {
                pLevel.registryAccess()
                    .registry(Registries.CONFIGURED_FEATURE)
                    .flatMap(p_258226_ -> p_258226_.getHolder(MiscOverworldFeatures.BONUS_CHEST))
                    .ifPresent(p_319563_ -> p_319563_.value().place(pLevel, serverchunkcache.getGenerator(), pLevel.random, pLevelData.getSpawnPos()));
            }
        }
    }

    private void setupDebugLevel(WorldData pWorldData) {
        pWorldData.setDifficulty(Difficulty.PEACEFUL);
        pWorldData.setDifficultyLocked(true);
        ServerLevelData serverleveldata = pWorldData.overworldData();
        serverleveldata.setRaining(false);
        serverleveldata.setThundering(false);
        serverleveldata.setClearWeatherTime(1000000000);
        serverleveldata.setDayTime(6000L);
        serverleveldata.setGameType(GameType.SPECTATOR);
    }

    /**
     * Loads the spawn chunks and any forced chunks
     */
    private void prepareLevels(ChunkProgressListener pListener) {
        ServerLevel serverlevel = this.overworld();
        LOGGER.info("Preparing start region for dimension {}", serverlevel.dimension().location());
        BlockPos blockpos = serverlevel.getSharedSpawnPos();
        pListener.updateSpawnPos(new ChunkPos(blockpos));
        ServerChunkCache serverchunkcache = serverlevel.getChunkSource();
        this.nextTickTimeNanos = Util.getNanos();
        serverlevel.setDefaultSpawnPos(blockpos, serverlevel.getSharedSpawnAngle());
        int i = this.getGameRules().getInt(GameRules.RULE_SPAWN_CHUNK_RADIUS);
        int j = i > 0 ? Mth.square(ChunkProgressListener.calculateDiameter(i)) : 0;

        while (serverchunkcache.getTickingGenerated() < j) {
            this.nextTickTimeNanos = Util.getNanos() + PREPARE_LEVELS_DEFAULT_DELAY_NANOS;
            this.waitUntilNextTick();
        }

        this.nextTickTimeNanos = Util.getNanos() + PREPARE_LEVELS_DEFAULT_DELAY_NANOS;
        this.waitUntilNextTick();

        for (ServerLevel serverlevel1 : this.levels.values()) {
            ForcedChunksSavedData forcedchunkssaveddata = serverlevel1.getDataStorage().get(ForcedChunksSavedData.factory(), "chunks");
            if (forcedchunkssaveddata != null) {
                LongIterator longiterator = forcedchunkssaveddata.getChunks().iterator();

                while (longiterator.hasNext()) {
                    long k = longiterator.nextLong();
                    ChunkPos chunkpos = new ChunkPos(k);
                    serverlevel1.getChunkSource().updateChunkForced(chunkpos, true);
                }
                net.neoforged.neoforge.common.world.chunk.ForcedChunkManager.reinstatePersistentChunks(serverlevel1, forcedchunkssaveddata);
            }
        }

        this.nextTickTimeNanos = Util.getNanos() + PREPARE_LEVELS_DEFAULT_DELAY_NANOS;
        this.waitUntilNextTick();
        pListener.stop();
        this.updateMobSpawningFlags();
    }

    public GameType getDefaultGameType() {
        return this.worldData.getGameType();
    }

    public boolean isHardcore() {
        return this.worldData.isHardcore();
    }

    public abstract int getOperatorUserPermissionLevel();

    public abstract int getFunctionCompilationLevel();

    public abstract boolean shouldRconBroadcast();

    public boolean saveAllChunks(boolean pSuppressLog, boolean pFlush, boolean pForced) {
        boolean flag = false;

        for (ServerLevel serverlevel : this.getAllLevels()) {
            if (!pSuppressLog) {
                LOGGER.info("Saving chunks for level '{}'/{}", serverlevel, serverlevel.dimension().location());
            }

            serverlevel.save(null, pFlush, serverlevel.noSave && !pForced);
            flag = true;
        }

        ServerLevel serverlevel2 = this.overworld();
        ServerLevelData serverleveldata = this.worldData.overworldData();
        serverleveldata.setWorldBorder(serverlevel2.getWorldBorder().createSettings());
        this.worldData.setCustomBossEvents(this.getCustomBossEvents().save(this.registryAccess()));
        this.storageSource.saveDataTag(this.registryAccess(), this.worldData, this.getPlayerList().getSingleplayerData());
        if (pFlush) {
            for (ServerLevel serverlevel1 : this.getAllLevels()) {
                LOGGER.info("ThreadedAnvilChunkStorage ({}): All chunks are saved", serverlevel1.getChunkSource().chunkMap.getStorageName());
            }

            LOGGER.info("ThreadedAnvilChunkStorage: All dimensions are saved");
        }

        return flag;
    }

    public boolean saveEverything(boolean pSuppressLog, boolean pFlush, boolean pForced) {
        boolean flag;
        try {
            this.isSaving = true;
            this.getPlayerList().saveAll();
            flag = this.saveAllChunks(pSuppressLog, pFlush, pForced);
        } finally {
            this.isSaving = false;
        }

        return flag;
    }

    @Override
    public void close() {
        this.stopServer();
    }

    public void stopServer() {
        if (this.metricsRecorder.isRecording()) {
            this.cancelRecordingMetrics();
        }

        LOGGER.info("Stopping server");
        this.getConnection().stop();
        this.isSaving = true;
        if (this.playerList != null) {
            LOGGER.info("Saving players");
            this.playerList.saveAll();
            this.playerList.removeAll();
        }

        LOGGER.info("Saving worlds");

        for (ServerLevel serverlevel : this.getAllLevels()) {
            if (serverlevel != null) {
                serverlevel.noSave = false;
            }
        }

        while (this.levels.values().stream().anyMatch(p_202480_ -> p_202480_.getChunkSource().chunkMap.hasWork())) {
            this.nextTickTimeNanos = Util.getNanos() + TimeUtil.NANOSECONDS_PER_MILLISECOND;

            for (ServerLevel serverlevel1 : this.getAllLevels()) {
                serverlevel1.getChunkSource().removeTicketsOnClosing();
                serverlevel1.getChunkSource().tick(() -> true, false);
            }

            this.waitUntilNextTick();
        }

        this.saveAllChunks(false, true, false);

        for (ServerLevel serverlevel2 : this.getAllLevels()) {
            if (serverlevel2 != null) {
                try {
                    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.level.LevelEvent.Unload(serverlevel2));
                    serverlevel2.close();
                } catch (IOException ioexception1) {
                    LOGGER.error("Exception closing the level", (Throwable)ioexception1);
                }
            }
        }

        this.isSaving = false;
        this.resources.close();

        try {
            this.storageSource.close();
        } catch (IOException ioexception) {
            LOGGER.error("Failed to unlock level {}", this.storageSource.getLevelId(), ioexception);
        }
    }

    public String getLocalIp() {
        return this.localIp;
    }

    public void setLocalIp(String pLocalIp) {
        this.localIp = pLocalIp;
    }

    public boolean isRunning() {
        return this.running;
    }

    /**
     * Sets the serverRunning variable to false, in order to get the server to shut down.
     */
    public void halt(boolean pWaitForServer) {
        this.running = false;
        if (pWaitForServer) {
            try {
                this.serverThread.join();
            } catch (InterruptedException interruptedexception) {
                LOGGER.error("Error while shutting down", (Throwable)interruptedexception);
            }
        }
    }

    protected void runServer() {
        try {
            if (!this.initServer()) {
                throw new IllegalStateException("Failed to initialize server");
            }

            net.neoforged.neoforge.server.ServerLifecycleHooks.handleServerStarted(this);
            this.nextTickTimeNanos = Util.getNanos();
            this.statusIcon = this.loadStatusIcon().orElse(null);
            this.status = this.buildServerStatus();
            resetStatusCache(status);

            while (this.running) {
                long i;
                if (!this.isPaused() && this.tickRateManager.isSprinting() && this.tickRateManager.checkShouldSprintThisTick()) {
                    i = 0L;
                    this.nextTickTimeNanos = Util.getNanos();
                    this.lastOverloadWarningNanos = this.nextTickTimeNanos;
                } else {
                    i = this.tickRateManager.nanosecondsPerTick();
                    long k = Util.getNanos() - this.nextTickTimeNanos;
                    if (k > OVERLOADED_THRESHOLD_NANOS + 20L * i
                        && this.nextTickTimeNanos - this.lastOverloadWarningNanos >= OVERLOADED_WARNING_INTERVAL_NANOS + 100L * i) {
                        long j = k / i;
                        LOGGER.warn("Can't keep up! Is the server overloaded? Running {}ms or {} ticks behind", k / TimeUtil.NANOSECONDS_PER_MILLISECOND, j);
                        this.nextTickTimeNanos += j * i;
                        this.lastOverloadWarningNanos = this.nextTickTimeNanos;
                    }
                }

                boolean flag = i == 0L;
                if (this.debugCommandProfilerDelayStart) {
                    this.debugCommandProfilerDelayStart = false;
                    this.debugCommandProfiler = new MinecraftServer.TimeProfiler(Util.getNanos(), this.tickCount);
                }

                this.nextTickTimeNanos += i;
                this.startMetricsRecordingTick();
                this.profiler.push("tick");
                this.tickServer(flag ? () -> false : this::haveTime);
                this.profiler.popPush("nextTickWait");
                this.mayHaveDelayedTasks = true;
                this.delayedTasksMaxNextTickTimeNanos = Math.max(Util.getNanos() + i, this.nextTickTimeNanos);
                this.startMeasuringTaskExecutionTime();
                this.waitUntilNextTick();
                this.finishMeasuringTaskExecutionTime();
                if (flag) {
                    this.tickRateManager.endTickWork();
                }

                this.profiler.pop();
                this.logFullTickTime();
                this.endMetricsRecordingTick();
                this.isReady = true;
                JvmProfiler.INSTANCE.onServerTick(this.smoothedTickTimeMillis);
            }
            net.neoforged.neoforge.server.ServerLifecycleHooks.handleServerStopping(this);
            net.neoforged.neoforge.server.ServerLifecycleHooks.expectServerStopped(); // Forge: Has to come before MinecraftServer#onServerCrash to avoid race conditions
        } catch (Throwable throwable1) {
            LOGGER.error("Encountered an unexpected exception", throwable1);
            CrashReport crashreport = constructOrExtractCrashReport(throwable1);
            this.fillSystemReport(crashreport.getSystemReport());
            Path path = this.getServerDirectory().resolve("crash-reports").resolve("crash-" + Util.getFilenameFormattedDateTime() + "-server.txt");
            if (crashreport.saveToFile(path, ReportType.CRASH)) {
                LOGGER.error("This crash report has been saved to: {}", path.toAbsolutePath());
            } else {
                LOGGER.error("We were unable to save this crash report to disk.");
            }

            net.neoforged.neoforge.server.ServerLifecycleHooks.expectServerStopped(); // Forge: Has to come before MinecraftServer#onServerCrash to avoid race conditions
            this.onServerCrash(crashreport);
        } finally {
            try {
                this.stopped = true;
                this.stopServer();
            } catch (Throwable throwable) {
                LOGGER.error("Exception stopping the server", throwable);
            } finally {
                if (this.services.profileCache() != null) {
                    this.services.profileCache().clearExecutor();
                }

                net.neoforged.neoforge.server.ServerLifecycleHooks.handleServerStopped(this);
                this.onServerExit();
            }
        }
    }

    private void logFullTickTime() {
        long i = Util.getNanos();
        if (this.isTickTimeLoggingEnabled()) {
            this.getTickTimeLogger().logSample(i - this.lastTickNanos);
        }

        this.lastTickNanos = i;
    }

    private void startMeasuringTaskExecutionTime() {
        if (this.isTickTimeLoggingEnabled()) {
            this.taskExecutionStartNanos = Util.getNanos();
            this.idleTimeNanos = 0L;
        }
    }

    private void finishMeasuringTaskExecutionTime() {
        if (this.isTickTimeLoggingEnabled()) {
            SampleLogger samplelogger = this.getTickTimeLogger();
            samplelogger.logPartialSample(Util.getNanos() - this.taskExecutionStartNanos - this.idleTimeNanos, TpsDebugDimensions.SCHEDULED_TASKS.ordinal());
            samplelogger.logPartialSample(this.idleTimeNanos, TpsDebugDimensions.IDLE.ordinal());
        }
    }

    private static CrashReport constructOrExtractCrashReport(Throwable pCause) {
        ReportedException reportedexception = null;

        for (Throwable throwable = pCause; throwable != null; throwable = throwable.getCause()) {
            if (throwable instanceof ReportedException reportedexception1) {
                reportedexception = reportedexception1;
            }
        }

        CrashReport crashreport;
        if (reportedexception != null) {
            crashreport = reportedexception.getReport();
            if (reportedexception != pCause) {
                crashreport.addCategory("Wrapped in").setDetailError("Wrapping exception", pCause);
            }
        } else {
            crashreport = new CrashReport("Exception in server tick loop", pCause);
        }

        return crashreport;
    }

    private boolean haveTime() {
        return this.runningTask() || Util.getNanos() < (this.mayHaveDelayedTasks ? this.delayedTasksMaxNextTickTimeNanos : this.nextTickTimeNanos);
    }

    public static boolean throwIfFatalException() {
        RuntimeException runtimeexception = fatalException.get();
        if (runtimeexception != null) {
            throw runtimeexception;
        } else {
            return true;
        }
    }

    public static void setFatalException(RuntimeException p_347584_) {
        fatalException.compareAndSet(null, p_347584_);
    }

    @Override
    public void managedBlock(BooleanSupplier p_347462_) {
        super.managedBlock(() -> throwIfFatalException() && p_347462_.getAsBoolean());
    }

    protected void waitUntilNextTick() {
        this.runAllTasks();
        this.managedBlock(() -> !this.haveTime());
    }

    @Override
    public void waitForTasks() {
        boolean flag = this.isTickTimeLoggingEnabled();
        long i = flag ? Util.getNanos() : 0L;
        super.waitForTasks();
        if (flag) {
            this.idleTimeNanos = this.idleTimeNanos + (Util.getNanos() - i);
        }
    }

    protected TickTask wrapRunnable(Runnable pRunnable) {
        return new TickTask(this.tickCount, pRunnable);
    }

    protected boolean shouldRun(TickTask pRunnable) {
        return pRunnable.getTick() + 3 < this.tickCount || this.haveTime();
    }

    @Override
    public boolean pollTask() {
        boolean flag = this.pollTaskInternal();
        this.mayHaveDelayedTasks = flag;
        return flag;
    }

    private boolean pollTaskInternal() {
        if (super.pollTask()) {
            return true;
        } else {
            if (this.tickRateManager.isSprinting() || this.haveTime()) {
                for (ServerLevel serverlevel : this.getAllLevels()) {
                    if (serverlevel.getChunkSource().pollTask()) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    public void doRunTask(TickTask pTask) {
        this.getProfiler().incrementCounter("runTask");
        super.doRunTask(pTask);
    }

    private Optional<ServerStatus.Favicon> loadStatusIcon() {
        Optional<Path> optional = Optional.of(this.getFile("server-icon.png"))
            .filter(p_272385_ -> Files.isRegularFile(p_272385_))
            .or(() -> this.storageSource.getIconFile().filter(p_272387_ -> Files.isRegularFile(p_272387_)));
        return optional.flatMap(p_272386_ -> {
            try {
                BufferedImage bufferedimage = ImageIO.read(p_272386_.toFile());
                Preconditions.checkState(bufferedimage.getWidth() == 64, "Must be 64 pixels wide");
                Preconditions.checkState(bufferedimage.getHeight() == 64, "Must be 64 pixels high");
                ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
                ImageIO.write(bufferedimage, "PNG", bytearrayoutputstream);
                return Optional.of(new ServerStatus.Favicon(bytearrayoutputstream.toByteArray()));
            } catch (Exception exception) {
                LOGGER.error("Couldn't load server icon", (Throwable)exception);
                return Optional.empty();
            }
        });
    }

    public Optional<Path> getWorldScreenshotFile() {
        return this.storageSource.getIconFile();
    }

    public Path getServerDirectory() {
        return Path.of("");
    }

    /**
     * Called on exit from the main run() loop.
     */
    public void onServerCrash(CrashReport pReport) {
    }

    public void onServerExit() {
    }

    public boolean isPaused() {
        return false;
    }

    /**
     * Main function called by run() every loop.
     */
    public void tickServer(BooleanSupplier pHasTimeLeft) {
        long i = Util.getNanos();
        this.tickCount++;
        net.neoforged.neoforge.event.EventHooks.fireServerTickPre(pHasTimeLeft, this);
        this.tickRateManager.tick();
        this.tickChildren(pHasTimeLeft);
        if (i - this.lastServerStatus >= STATUS_EXPIRE_TIME_NANOS) {
            this.lastServerStatus = i;
            this.status = this.buildServerStatus();
            resetStatusCache(status);
        }

        this.ticksUntilAutosave--;
        if (this.ticksUntilAutosave <= 0) {
            this.ticksUntilAutosave = this.computeNextAutosaveInterval();
            LOGGER.debug("Autosave started");
            this.profiler.push("save");
            this.saveEverything(true, false, false);
            this.profiler.pop();
            LOGGER.debug("Autosave finished");
        }

        this.profiler.push("tallying");
        long j = Util.getNanos() - i;
        int k = this.tickCount % 100;
        this.aggregatedTickTimesNanos = this.aggregatedTickTimesNanos - this.tickTimesNanos[k];
        this.aggregatedTickTimesNanos += j;
        this.tickTimesNanos[k] = j;
        this.smoothedTickTimeMillis = this.smoothedTickTimeMillis * 0.8F + (float)j / (float)TimeUtil.NANOSECONDS_PER_MILLISECOND * 0.19999999F;
        this.logTickMethodTime(i);
        this.profiler.pop();
        net.neoforged.neoforge.event.EventHooks.fireServerTickPost(pHasTimeLeft, this);
    }

    private void logTickMethodTime(long pStartTime) {
        if (this.isTickTimeLoggingEnabled()) {
            this.getTickTimeLogger().logPartialSample(Util.getNanos() - pStartTime, TpsDebugDimensions.TICK_SERVER_METHOD.ordinal());
        }
    }

    private static final com.google.gson.Gson GSON = new com.google.gson.Gson();
    private String cachedServerStatus; // NEO: cache the server status json in case a client spams requests
    private void resetStatusCache(ServerStatus status) {
        this.cachedServerStatus = GSON.toJson(ServerStatus.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, status)
                .result().orElseThrow());
    }
    public String getStatusJson() {
        return cachedServerStatus;
    }

    private int computeNextAutosaveInterval() {
        float f;
        if (this.tickRateManager.isSprinting()) {
            long i = this.getAverageTickTimeNanos() + 1L;
            f = (float)TimeUtil.NANOSECONDS_PER_SECOND / (float)i;
        } else {
            f = this.tickRateManager.tickrate();
        }

        int j = 300;
        return Math.max(100, (int)(f * 300.0F));
    }

    public void onTickRateChanged() {
        int i = this.computeNextAutosaveInterval();
        if (i < this.ticksUntilAutosave) {
            this.ticksUntilAutosave = i;
        }
    }

    protected abstract SampleLogger getTickTimeLogger();

    public abstract boolean isTickTimeLoggingEnabled();

    private ServerStatus buildServerStatus() {
        ServerStatus.Players serverstatus$players = this.buildPlayerStatus();
        return new ServerStatus(
            Component.nullToEmpty(this.motd),
            Optional.of(serverstatus$players),
            Optional.of(ServerStatus.Version.current()),
            Optional.ofNullable(this.statusIcon),
            this.enforceSecureProfile(),
            true //TODO Neo: Possible build a system which indicates what the status of the modded server is.
        );
    }

    private ServerStatus.Players buildPlayerStatus() {
        List<ServerPlayer> list = this.playerList.getPlayers();
        int i = this.getMaxPlayers();
        if (this.hidesOnlinePlayers()) {
            return new ServerStatus.Players(i, list.size(), List.of());
        } else {
            int j = Math.min(list.size(), 12);
            ObjectArrayList<GameProfile> objectarraylist = new ObjectArrayList<>(j);
            int k = Mth.nextInt(this.random, 0, list.size() - j);

            for (int l = 0; l < j; l++) {
                ServerPlayer serverplayer = list.get(k + l);
                objectarraylist.add(serverplayer.allowsListing() ? serverplayer.getGameProfile() : ANONYMOUS_PLAYER_PROFILE);
            }

            Util.shuffle(objectarraylist, this.random);
            return new ServerStatus.Players(i, list.size(), objectarraylist);
        }
    }

    public void tickChildren(BooleanSupplier pHasTimeLeft) {
        this.getPlayerList().getPlayers().forEach(p_341570_ -> p_341570_.connection.suspendFlushing());
        this.profiler.push("commandFunctions");
        this.getFunctions().tick();
        this.profiler.popPush("levels");

        for(ServerLevel serverlevel : this.getWorldArray()) {
            long tickStart = Util.getNanos();
            this.profiler.push(() -> serverlevel + " " + serverlevel.dimension().location());
            if (this.tickCount % 20 == 0) {
                this.profiler.push("timeSync");
                this.synchronizeTime(serverlevel);
                this.profiler.pop();
            }

            this.profiler.push("tick");
            net.neoforged.neoforge.event.EventHooks.fireLevelTickPre(serverlevel, pHasTimeLeft);

            try {
                serverlevel.tick(pHasTimeLeft);
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.forThrowable(throwable, "Exception ticking world");
                serverlevel.fillReportDetails(crashreport);
                throw new ReportedException(crashreport);
            }
            net.neoforged.neoforge.event.EventHooks.fireLevelTickPost(serverlevel, pHasTimeLeft);

            this.profiler.pop();
            this.profiler.pop();
            perWorldTickTimes.computeIfAbsent(serverlevel.dimension(), k -> new long[100])[this.tickCount % 100] = Util.getNanos() - tickStart;
        }

        this.profiler.popPush("connection");
        this.getConnection().tick();
        this.profiler.popPush("players");
        this.playerList.tick();
        if (net.neoforged.neoforge.gametest.GameTestHooks.isGametestEnabled() && this.tickRateManager.runsNormally()) {
            GameTestTicker.SINGLETON.tick();
        }

        this.profiler.popPush("server gui refresh");

        for (int i = 0; i < this.tickables.size(); i++) {
            this.tickables.get(i).run();
        }

        this.profiler.popPush("send chunks");

        for (ServerPlayer serverplayer : this.playerList.getPlayers()) {
            serverplayer.connection.chunkSender.sendNextChunks(serverplayer);
            serverplayer.connection.resumeFlushing();
        }

        this.profiler.pop();
    }

    private void synchronizeTime(ServerLevel pLevel) {
        this.playerList
            .broadcastAll(
                new ClientboundSetTimePacket(pLevel.getGameTime(), pLevel.getDayTime(), pLevel.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)),
                pLevel.dimension()
            );
    }

    public void forceTimeSynchronization() {
        this.profiler.push("timeSync");

        for (ServerLevel serverlevel : this.getAllLevels()) {
            this.synchronizeTime(serverlevel);
        }

        this.profiler.pop();
    }

    public boolean isLevelEnabled(Level p_350377_) {
        return true;
    }

    public void addTickable(Runnable pTickable) {
        this.tickables.add(pTickable);
    }

    protected void setId(String pServerId) {
        this.serverId = pServerId;
    }

    public boolean isShutdown() {
        return !this.serverThread.isAlive();
    }

    public Path getFile(String p_129972_) {
        return this.getServerDirectory().resolve(p_129972_);
    }

    public final ServerLevel overworld() {
        return this.levels.get(Level.OVERWORLD);
    }

    /**
     * Gets the worldServer by the given dimension.
     */
    @Nullable
    public ServerLevel getLevel(ResourceKey<Level> pDimension) {
        return this.levels.get(pDimension);
    }

    public Set<ResourceKey<Level>> levelKeys() {
        return this.levels.keySet();
    }

    public Iterable<ServerLevel> getAllLevels() {
        return this.levels.values();
    }

    @Override
    public String getServerVersion() {
        return SharedConstants.getCurrentVersion().getName();
    }

    @Override
    public int getPlayerCount() {
        return this.playerList.getPlayerCount();
    }

    @Override
    public int getMaxPlayers() {
        return this.playerList.getMaxPlayers();
    }

    public String[] getPlayerNames() {
        return this.playerList.getPlayerNamesArray();
    }

    @DontObfuscate
    public String getServerModName() {
        return net.neoforged.neoforge.internal.BrandingControl.getServerBranding();
    }

    public SystemReport fillSystemReport(SystemReport pSystemReport) {
        pSystemReport.setDetail("Server Running", () -> Boolean.toString(this.running));
        if (this.playerList != null) {
            pSystemReport.setDetail(
                "Player Count", () -> this.playerList.getPlayerCount() + " / " + this.playerList.getMaxPlayers() + "; " + this.playerList.getPlayers()
            );
        }

        pSystemReport.setDetail("Active Data Packs", () -> PackRepository.displayPackList(this.packRepository.getSelectedPacks()));
        pSystemReport.setDetail("Available Data Packs", () -> PackRepository.displayPackList(this.packRepository.getAvailablePacks()));
        pSystemReport.setDetail(
            "Enabled Feature Flags",
            () -> FeatureFlags.REGISTRY.toNames(this.worldData.enabledFeatures()).stream().map(ResourceLocation::toString).collect(Collectors.joining(", "))
        );
        pSystemReport.setDetail("World Generation", () -> this.worldData.worldGenSettingsLifecycle().toString());
        pSystemReport.setDetail("World Seed", () -> String.valueOf(this.worldData.worldGenOptions().seed()));
        if (this.serverId != null) {
            pSystemReport.setDetail("Server Id", () -> this.serverId);
        }

        return this.fillServerSystemReport(pSystemReport);
    }

    public abstract SystemReport fillServerSystemReport(SystemReport pReport);

    public ModCheck getModdedStatus() {
        return ModCheck.identify("vanilla", this::getServerModName, "Server", MinecraftServer.class);
    }

    @Override
    public void sendSystemMessage(Component pComponent) {
        LOGGER.info(pComponent.getString());
    }

    public KeyPair getKeyPair() {
        return this.keyPair;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int pPort) {
        this.port = pPort;
    }

    @Nullable
    public GameProfile getSingleplayerProfile() {
        return this.singleplayerProfile;
    }

    public void setSingleplayerProfile(@Nullable GameProfile pSingleplayerProfile) {
        this.singleplayerProfile = pSingleplayerProfile;
    }

    public boolean isSingleplayer() {
        return this.singleplayerProfile != null;
    }

    protected void initializeKeyPair() {
        LOGGER.info("Generating keypair");

        try {
            this.keyPair = Crypt.generateKeyPair();
        } catch (CryptException cryptexception) {
            throw new IllegalStateException("Failed to generate key pair", cryptexception);
        }
    }

    public void setDifficulty(Difficulty pDifficulty, boolean pForced) {
        if (pForced || !this.worldData.isDifficultyLocked()) {
            this.worldData.setDifficulty(this.worldData.isHardcore() ? Difficulty.HARD : pDifficulty);
            this.updateMobSpawningFlags();
            this.getPlayerList().getPlayers().forEach(this::sendDifficultyUpdate);
        }
    }

    public int getScaledTrackingDistance(int pTrackingDistance) {
        return pTrackingDistance;
    }

    private void updateMobSpawningFlags() {
        for (ServerLevel serverlevel : this.getAllLevels()) {
            serverlevel.setSpawnSettings(this.isSpawningMonsters(), this.isSpawningAnimals());
        }
    }

    public void setDifficultyLocked(boolean pLocked) {
        this.worldData.setDifficultyLocked(pLocked);
        this.getPlayerList().getPlayers().forEach(this::sendDifficultyUpdate);
    }

    private void sendDifficultyUpdate(ServerPlayer p_129939_) {
        LevelData leveldata = p_129939_.level().getLevelData();
        p_129939_.connection.send(new ClientboundChangeDifficultyPacket(leveldata.getDifficulty(), leveldata.isDifficultyLocked()));
    }

    public boolean isSpawningMonsters() {
        return this.worldData.getDifficulty() != Difficulty.PEACEFUL;
    }

    public boolean isDemo() {
        return this.isDemo;
    }

    /**
     * Sets whether this is a demo or not.
     */
    public void setDemo(boolean pDemo) {
        this.isDemo = pDemo;
    }

    public Optional<MinecraftServer.ServerResourcePackInfo> getServerResourcePack() {
        return Optional.empty();
    }

    public boolean isResourcePackRequired() {
        return this.getServerResourcePack().filter(MinecraftServer.ServerResourcePackInfo::isRequired).isPresent();
    }

    public abstract boolean isDedicatedServer();

    public abstract int getRateLimitPacketsPerSecond();

    public boolean usesAuthentication() {
        return this.onlineMode;
    }

    public void setUsesAuthentication(boolean pOnline) {
        this.onlineMode = pOnline;
    }

    public boolean getPreventProxyConnections() {
        return this.preventProxyConnections;
    }

    public void setPreventProxyConnections(boolean pPreventProxyConnections) {
        this.preventProxyConnections = pPreventProxyConnections;
    }

    public boolean isSpawningAnimals() {
        return true;
    }

    public boolean areNpcsEnabled() {
        return true;
    }

    public abstract boolean isEpollEnabled();

    public boolean isPvpAllowed() {
        return this.pvp;
    }

    public void setPvpAllowed(boolean pAllowPvp) {
        this.pvp = pAllowPvp;
    }

    public boolean isFlightAllowed() {
        return this.allowFlight;
    }

    public void setFlightAllowed(boolean pAllow) {
        this.allowFlight = pAllow;
    }

    public abstract boolean isCommandBlockEnabled();

    @Override
    public String getMotd() {
        return this.motd;
    }

    public void setMotd(String pMotd) {
        this.motd = pMotd;
    }

    public boolean isStopped() {
        return this.stopped;
    }

    public PlayerList getPlayerList() {
        return this.playerList;
    }

    public void setPlayerList(PlayerList pList) {
        this.playerList = pList;
    }

    public abstract boolean isPublished();

    /**
     * Sets the game type for all worlds.
     */
    public void setDefaultGameType(GameType pGameMode) {
        this.worldData.setGameType(pGameMode);
    }

    public ServerConnectionListener getConnection() {
        return this.connection;
    }

    public boolean isReady() {
        return this.isReady;
    }

    public boolean hasGui() {
        return false;
    }

    public boolean publishServer(@Nullable GameType pGameMode, boolean pCommands, int pPort) {
        return false;
    }

    public int getTickCount() {
        return this.tickCount;
    }

    public int getSpawnProtectionRadius() {
        return 16;
    }

    public boolean isUnderSpawnProtection(ServerLevel pLevel, BlockPos pPos, Player pPlayer) {
        return false;
    }

    public boolean repliesToStatus() {
        return true;
    }

    public boolean hidesOnlinePlayers() {
        return false;
    }

    public Proxy getProxy() {
        return this.proxy;
    }

    public int getPlayerIdleTimeout() {
        return this.playerIdleTimeout;
    }

    public void setPlayerIdleTimeout(int pIdleTimeout) {
        this.playerIdleTimeout = pIdleTimeout;
    }

    public MinecraftSessionService getSessionService() {
        return this.services.sessionService();
    }

    @Nullable
    public SignatureValidator getProfileKeySignatureValidator() {
        return this.services.profileKeySignatureValidator();
    }

    public GameProfileRepository getProfileRepository() {
        return this.services.profileRepository();
    }

    @Nullable
    public GameProfileCache getProfileCache() {
        return this.services.profileCache();
    }

    @Nullable
    public ServerStatus getStatus() {
        return this.status;
    }

    public void invalidateStatus() {
        this.lastServerStatus = 0L;
    }

    public int getAbsoluteMaxWorldSize() {
        return 29999984;
    }

    @Override
    public boolean scheduleExecutables() {
        return super.scheduleExecutables() && !this.isStopped();
    }

    @Override
    public void executeIfPossible(Runnable pTask) {
        if (this.isStopped()) {
            throw new RejectedExecutionException("Server already shutting down");
        } else {
            super.executeIfPossible(pTask);
        }
    }

    @Override
    public Thread getRunningThread() {
        return this.serverThread;
    }

    public int getCompressionThreshold() {
        return 256;
    }

    public boolean enforceSecureProfile() {
        return false;
    }

    public long getNextTickTime() {
        return this.nextTickTimeNanos;
    }

    public DataFixer getFixerUpper() {
        return this.fixerUpper;
    }

    public int getSpawnRadius(@Nullable ServerLevel pLevel) {
        return pLevel != null ? pLevel.getGameRules().getInt(GameRules.RULE_SPAWN_RADIUS) : 10;
    }

    public ServerAdvancementManager getAdvancements() {
        return this.resources.managers.getAdvancements();
    }

    public ServerFunctionManager getFunctions() {
        return this.functionManager;
    }

    /**
     * Replaces currently selected list of datapacks, reloads them, and sends new data to players.
     */
    public CompletableFuture<Void> reloadResources(Collection<String> pSelectedIds) {
        CompletableFuture<Void> completablefuture = CompletableFuture.<ImmutableList>supplyAsync(
                () -> this.packRepository.rebuildSelected(pSelectedIds).stream().map(Pack::open).collect(ImmutableList.toImmutableList()),
                this
            )
            .thenCompose(
                p_335201_ -> {
                    CloseableResourceManager closeableresourcemanager = new MultiPackResourceManager(PackType.SERVER_DATA, p_335201_);
                    return ReloadableServerResources.loadResources(
                            closeableresourcemanager,
                            this.registries,
                            this.worldData.enabledFeatures(),
                            this.isDedicatedServer() ? Commands.CommandSelection.DEDICATED : Commands.CommandSelection.INTEGRATED,
                            this.getFunctionCompilationLevel(),
                            this.executor,
                            this
                        )
                        .whenComplete((p_212907_, p_212908_) -> {
                            if (p_212908_ != null) {
                                closeableresourcemanager.close();
                            }
                        })
                        .thenApply(p_212904_ -> new MinecraftServer.ReloadableResources(closeableresourcemanager, p_212904_));
                }
            )
            .thenAcceptAsync(
                p_335203_ -> {
                    this.resources.close();
                    this.resources = p_335203_;
                    this.packRepository.setSelected(pSelectedIds);
                    WorldDataConfiguration worlddataconfiguration = new WorldDataConfiguration(
                        getSelectedPacks(this.packRepository, true), this.worldData.enabledFeatures()
                    );
                    this.worldData.setDataConfiguration(worlddataconfiguration);
                    this.resources.managers.updateRegistryTags();
                    this.getPlayerList().saveAll();
                    this.getPlayerList().reloadResources();
                    this.functionManager.replaceLibrary(this.resources.managers.getFunctionLibrary());
                    this.structureTemplateManager.onResourceManagerReload(this.resources.resourceManager);
                    this.getPlayerList().getPlayers().forEach(this.getPlayerList()::sendPlayerPermissionLevel); //Forge: Fix newly added/modified commands not being sent to the client when commands reload.
                },
                this
            );
        if (this.isSameThread()) {
            this.managedBlock(completablefuture::isDone);
        }

        return completablefuture;
    }

    public static WorldDataConfiguration configurePackRepository(
        PackRepository pPackRepository, WorldDataConfiguration pInitialDataConfig, boolean pInitMode, boolean pSafeMode
    ) {
        DataPackConfig datapackconfig = pInitialDataConfig.dataPacks();
        FeatureFlagSet featureflagset = pInitMode ? FeatureFlagSet.of() : pInitialDataConfig.enabledFeatures();
        FeatureFlagSet featureflagset1 = pInitMode ? FeatureFlags.REGISTRY.allFlags() : pInitialDataConfig.enabledFeatures();
        pPackRepository.reload();
        DataPackConfig.DEFAULT.addModPacks(net.neoforged.neoforge.common.CommonHooks.getModDataPacks());
        datapackconfig.addModPacks(net.neoforged.neoforge.common.CommonHooks.getModDataPacks());
        if (pSafeMode) {
            return configureRepositoryWithSelection(pPackRepository, net.neoforged.neoforge.common.CommonHooks.getModDataPacksWithVanilla(), featureflagset, false);
        } else {
            Set<String> set = Sets.newLinkedHashSet();

            for (String s : datapackconfig.getEnabled()) {
                if (pPackRepository.isAvailable(s)) {
                    set.add(s);
                } else {
                    LOGGER.warn("Missing data pack {}", s);
                }
            }

            for (Pack pack : pPackRepository.getAvailablePacks()) {
                String s1 = pack.getId();
                if (!datapackconfig.getDisabled().contains(s1)) {
                    FeatureFlagSet featureflagset2 = pack.getRequestedFeatures();
                    boolean flag = set.contains(s1);
                    if (!flag && pack.getPackSource().shouldAddAutomatically()) {
                        if (featureflagset2.isSubsetOf(featureflagset1)) {
                            LOGGER.info("Found new data pack {}, loading it automatically", s1);
                            set.add(s1);
                        } else {
                            LOGGER.info(
                                "Found new data pack {}, but can't load it due to missing features {}",
                                s1,
                                FeatureFlags.printMissingFlags(featureflagset1, featureflagset2)
                            );
                        }
                    }

                    if (flag && !featureflagset2.isSubsetOf(featureflagset1)) {
                        LOGGER.warn(
                            "Pack {} requires features {} that are not enabled for this world, disabling pack.",
                            s1,
                            FeatureFlags.printMissingFlags(featureflagset1, featureflagset2)
                        );
                        set.remove(s1);
                    }
                }
            }

            if (set.isEmpty()) {
                LOGGER.info("No datapacks selected, forcing vanilla");
                set.add("vanilla");
            }

            net.neoforged.neoforge.resource.ResourcePackLoader.reorderNewlyDiscoveredPacks(set, datapackconfig.getEnabled(), pPackRepository);

            return configureRepositoryWithSelection(pPackRepository, set, featureflagset, true);
        }
    }

    private static WorldDataConfiguration configureRepositoryWithSelection(
        PackRepository pPackRepository, Collection<String> pSelectedPacks, FeatureFlagSet pEnabledFeatures, boolean pSafeMode
    ) {
        pPackRepository.setSelected(pSelectedPacks);
        enableForcedFeaturePacks(pPackRepository, pEnabledFeatures);
        DataPackConfig datapackconfig = getSelectedPacks(pPackRepository, pSafeMode);
        FeatureFlagSet featureflagset = pPackRepository.getRequestedFeatureFlags().join(pEnabledFeatures);
        return new WorldDataConfiguration(datapackconfig, featureflagset);
    }

    private static void enableForcedFeaturePacks(PackRepository pPackRepository, FeatureFlagSet pEnabledFeatures) {
        FeatureFlagSet featureflagset = pPackRepository.getRequestedFeatureFlags();
        FeatureFlagSet featureflagset1 = pEnabledFeatures.subtract(featureflagset);
        if (!featureflagset1.isEmpty()) {
            Set<String> set = new ObjectArraySet<>(pPackRepository.getSelectedIds());

            for (Pack pack : pPackRepository.getAvailablePacks()) {
                if (featureflagset1.isEmpty()) {
                    break;
                }

                if (pack.getPackSource() == PackSource.FEATURE) {
                    String s = pack.getId();
                    FeatureFlagSet featureflagset2 = pack.getRequestedFeatures();
                    if (!featureflagset2.isEmpty() && featureflagset2.intersects(featureflagset1) && featureflagset2.isSubsetOf(pEnabledFeatures)) {
                        if (!set.add(s)) {
                            throw new IllegalStateException("Tried to force '" + s + "', but it was already enabled");
                        }

                        LOGGER.info("Found feature pack ('{}') for requested feature, forcing to enabled", s);
                        featureflagset1 = featureflagset1.subtract(featureflagset2);
                    }
                }
            }

            pPackRepository.setSelected(set);
        }
    }

    private static DataPackConfig getSelectedPacks(PackRepository pPackRepository, boolean pSafeMode) {
        Collection<String> collection = pPackRepository.getSelectedIds();
        List<String> list = ImmutableList.copyOf(collection);
        List<String> list1 = pSafeMode ? pPackRepository.getAvailableIds().stream().filter(p_212916_ -> !collection.contains(p_212916_)).toList() : List.of();
        return new DataPackConfig(list, list1);
    }

    public void kickUnlistedPlayers(CommandSourceStack pCommandSource) {
        if (this.isEnforceWhitelist()) {
            PlayerList playerlist = pCommandSource.getServer().getPlayerList();
            UserWhiteList userwhitelist = playerlist.getWhiteList();

            for (ServerPlayer serverplayer : Lists.newArrayList(playerlist.getPlayers())) {
                if (!userwhitelist.isWhiteListed(serverplayer.getGameProfile())) {
                    serverplayer.connection.disconnect(Component.translatable("multiplayer.disconnect.not_whitelisted"));
                }
            }
        }
    }

    public PackRepository getPackRepository() {
        return this.packRepository;
    }

    public Commands getCommands() {
        return this.resources.managers.getCommands();
    }

    public CommandSourceStack createCommandSourceStack() {
        ServerLevel serverlevel = this.overworld();
        return new CommandSourceStack(
            this,
            serverlevel == null ? Vec3.ZERO : Vec3.atLowerCornerOf(serverlevel.getSharedSpawnPos()),
            Vec2.ZERO,
            serverlevel,
            4,
            "Server",
            Component.literal("Server"),
            this,
            null
        );
    }

    @Override
    public boolean acceptsSuccess() {
        return true;
    }

    @Override
    public boolean acceptsFailure() {
        return true;
    }

    @Override
    public abstract boolean shouldInformAdmins();

    public RecipeManager getRecipeManager() {
        return this.resources.managers.getRecipeManager();
    }

    public ServerScoreboard getScoreboard() {
        return this.scoreboard;
    }

    public CommandStorage getCommandStorage() {
        if (this.commandStorage == null) {
            throw new NullPointerException("Called before server init");
        } else {
            return this.commandStorage;
        }
    }

    public GameRules getGameRules() {
        return this.overworld().getGameRules();
    }

    public CustomBossEvents getCustomBossEvents() {
        return this.customBossEvents;
    }

    public boolean isEnforceWhitelist() {
        return this.enforceWhitelist;
    }

    public void setEnforceWhitelist(boolean pWhitelistEnabled) {
        this.enforceWhitelist = pWhitelistEnabled;
    }

    public float getCurrentSmoothedTickTime() {
        return this.smoothedTickTimeMillis;
    }

    public ServerTickRateManager tickRateManager() {
        return this.tickRateManager;
    }

    public long getAverageTickTimeNanos() {
        return this.aggregatedTickTimesNanos / (long)Math.min(100, Math.max(this.tickCount, 1));
    }

    public long[] getTickTimesNanos() {
        return this.tickTimesNanos;
    }

    public int getProfilePermissions(GameProfile pProfile) {
        if (this.getPlayerList().isOp(pProfile)) {
            ServerOpListEntry serveroplistentry = this.getPlayerList().getOps().get(pProfile);
            if (serveroplistentry != null) {
                return serveroplistentry.getLevel();
            } else if (this.isSingleplayerOwner(pProfile)) {
                return 4;
            } else if (this.isSingleplayer()) {
                return this.getPlayerList().isAllowCommandsForAllPlayers() ? 4 : 0;
            } else {
                return this.getOperatorUserPermissionLevel();
            }
        } else {
            return 0;
        }
    }

    public ProfilerFiller getProfiler() {
        return this.profiler;
    }

    public abstract boolean isSingleplayerOwner(GameProfile pProfile);

    private Map<ResourceKey<Level>, long[]> perWorldTickTimes = Maps.newIdentityHashMap();
    @Nullable
    public long[] getTickTime(ResourceKey<Level> dim) {
        return perWorldTickTimes.get(dim);
    }

    @Deprecated //Forge Internal use Only, You can screw up a lot of things if you mess with this map.
    public synchronized Map<ResourceKey<Level>, ServerLevel> forgeGetWorldMap() {
        return this.levels;
    }
    private int worldArrayMarker = 0;
    private int worldArrayLast = -1;
    private ServerLevel[] worldArray;
    @Deprecated //Forge Internal use Only, use to protect against concurrent modifications in the world tick loop.
    public synchronized void markWorldsDirty() {
        worldArrayMarker++;
    }
    private ServerLevel[] getWorldArray() {
        if (worldArrayMarker == worldArrayLast && worldArray != null)
            return worldArray;
        worldArray = this.levels.values().stream().toArray(x -> new ServerLevel[x]);
        worldArrayLast = worldArrayMarker;
        return worldArray;
    }

    public void dumpServerProperties(Path pPath) throws IOException {
    }

    private void saveDebugReport(Path pPath) {
        Path path = pPath.resolve("levels");

        try {
            for (Entry<ResourceKey<Level>, ServerLevel> entry : this.levels.entrySet()) {
                ResourceLocation resourcelocation = entry.getKey().location();
                Path path1 = path.resolve(resourcelocation.getNamespace()).resolve(resourcelocation.getPath());
                Files.createDirectories(path1);
                entry.getValue().saveDebugReport(path1);
            }

            this.dumpGameRules(pPath.resolve("gamerules.txt"));
            this.dumpClasspath(pPath.resolve("classpath.txt"));
            this.dumpMiscStats(pPath.resolve("stats.txt"));
            this.dumpThreads(pPath.resolve("threads.txt"));
            this.dumpServerProperties(pPath.resolve("server.properties.txt"));
            this.dumpNativeModules(pPath.resolve("modules.txt"));
        } catch (IOException ioexception) {
            LOGGER.warn("Failed to save debug report", (Throwable)ioexception);
        }
    }

    private void dumpMiscStats(Path pPath) throws IOException {
        try (Writer writer = Files.newBufferedWriter(pPath)) {
            writer.write(String.format(Locale.ROOT, "pending_tasks: %d\n", this.getPendingTasksCount()));
            writer.write(String.format(Locale.ROOT, "average_tick_time: %f\n", this.getCurrentSmoothedTickTime()));
            writer.write(String.format(Locale.ROOT, "tick_times: %s\n", Arrays.toString(this.tickTimesNanos)));
            writer.write(String.format(Locale.ROOT, "queue: %s\n", Util.backgroundExecutor()));
        }
    }

    private void dumpGameRules(Path pPath) throws IOException {
        try (Writer writer = Files.newBufferedWriter(pPath)) {
            final List<String> list = Lists.newArrayList();
            final GameRules gamerules = this.getGameRules();
            GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
                @Override
                public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> p_195531_, GameRules.Type<T> p_195532_) {
                    list.add(String.format(Locale.ROOT, "%s=%s\n", p_195531_.getId(), gamerules.getRule(p_195531_)));
                }
            });

            for (String s : list) {
                writer.write(s);
            }
        }
    }

    private void dumpClasspath(Path pPath) throws IOException {
        try (Writer writer = Files.newBufferedWriter(pPath)) {
            String s = System.getProperty("java.class.path");
            String s1 = System.getProperty("path.separator");

            for (String s2 : Splitter.on(s1).split(s)) {
                writer.write(s2);
                writer.write("\n");
            }
        }
    }

    private void dumpThreads(Path pPath) throws IOException {
        ThreadMXBean threadmxbean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] athreadinfo = threadmxbean.dumpAllThreads(true, true);
        Arrays.sort(athreadinfo, Comparator.comparing(ThreadInfo::getThreadName));

        try (Writer writer = Files.newBufferedWriter(pPath)) {
            for (ThreadInfo threadinfo : athreadinfo) {
                writer.write(threadinfo.toString());
                writer.write(10);
            }
        }
    }

    private void dumpNativeModules(Path pPath) throws IOException {
        try (Writer writer = Files.newBufferedWriter(pPath)) {
            List<NativeModuleLister.NativeModuleInfo> list;
            try {
                list = Lists.newArrayList(NativeModuleLister.listModules());
            } catch (Throwable throwable) {
                LOGGER.warn("Failed to list native modules", throwable);
                return;
            }

            list.sort(Comparator.comparing(p_212910_ -> p_212910_.name));

            for (NativeModuleLister.NativeModuleInfo nativemodulelister$nativemoduleinfo : list) {
                writer.write(nativemodulelister$nativemoduleinfo.toString());
                writer.write(10);
            }
        }
    }

    private void startMetricsRecordingTick() {
        if (this.willStartRecordingMetrics) {
            this.metricsRecorder = ActiveMetricsRecorder.createStarted(
                new ServerMetricsSamplersProvider(Util.timeSource, this.isDedicatedServer()),
                Util.timeSource,
                Util.ioPool(),
                new MetricsPersister("server"),
                this.onMetricsRecordingStopped,
                p_212927_ -> {
                    this.executeBlocking(() -> this.saveDebugReport(p_212927_.resolve("server")));
                    this.onMetricsRecordingFinished.accept(p_212927_);
                }
            );
            this.willStartRecordingMetrics = false;
        }

        this.profiler = SingleTickProfiler.decorateFiller(this.metricsRecorder.getProfiler(), SingleTickProfiler.createTickProfiler("Server"));
        this.metricsRecorder.startTick();
        this.profiler.startTick();
    }

    public void endMetricsRecordingTick() {
        this.profiler.endTick();
        this.metricsRecorder.endTick();
    }

    public boolean isRecordingMetrics() {
        return this.metricsRecorder.isRecording();
    }

    public void startRecordingMetrics(Consumer<ProfileResults> pOutput, Consumer<Path> pOnMetricsRecordingFinished) {
        this.onMetricsRecordingStopped = p_212922_ -> {
            this.stopRecordingMetrics();
            pOutput.accept(p_212922_);
        };
        this.onMetricsRecordingFinished = pOnMetricsRecordingFinished;
        this.willStartRecordingMetrics = true;
    }

    public void stopRecordingMetrics() {
        this.metricsRecorder = InactiveMetricsRecorder.INSTANCE;
    }

    public void finishRecordingMetrics() {
        this.metricsRecorder.end();
    }

    public void cancelRecordingMetrics() {
        this.metricsRecorder.cancel();
        this.profiler = this.metricsRecorder.getProfiler();
    }

    public Path getWorldPath(LevelResource pLevelResource) {
        return this.storageSource.getLevelPath(pLevelResource);
    }

    public boolean forceSynchronousWrites() {
        return true;
    }

    public StructureTemplateManager getStructureManager() {
        return this.structureTemplateManager;
    }

    public WorldData getWorldData() {
        return this.worldData;
    }

    public MinecraftServer.ReloadableResources getServerResources() {
         return resources;
    }

    public RegistryAccess.Frozen registryAccess() {
        return this.registries.compositeAccess();
    }

    public LayeredRegistryAccess<RegistryLayer> registries() {
        return this.registries;
    }

    public ReloadableServerRegistries.Holder reloadableRegistries() {
        return this.resources.managers.fullRegistries();
    }

    public TextFilter createTextFilterForPlayer(ServerPlayer pPlayer) {
        return TextFilter.DUMMY;
    }

    public ServerPlayerGameMode createGameModeForPlayer(ServerPlayer pPlayer) {
        return (ServerPlayerGameMode)(this.isDemo() ? new DemoMode(pPlayer) : new ServerPlayerGameMode(pPlayer));
    }

    @Nullable
    public GameType getForcedGameType() {
        return null;
    }

    public ResourceManager getResourceManager() {
        return this.resources.resourceManager;
    }

    public boolean isCurrentlySaving() {
        return this.isSaving;
    }

    public boolean isTimeProfilerRunning() {
        return this.debugCommandProfilerDelayStart || this.debugCommandProfiler != null;
    }

    public void startTimeProfiler() {
        this.debugCommandProfilerDelayStart = true;
    }

    public ProfileResults stopTimeProfiler() {
        if (this.debugCommandProfiler == null) {
            return EmptyProfileResults.EMPTY;
        } else {
            ProfileResults profileresults = this.debugCommandProfiler.stop(Util.getNanos(), this.tickCount);
            this.debugCommandProfiler = null;
            return profileresults;
        }
    }

    public int getMaxChainedNeighborUpdates() {
        return 1000000;
    }

    public void logChatMessage(Component pContent, ChatType.Bound pBoundChatType, @Nullable String pHeader) {
        String s = pBoundChatType.decorate(pContent).getString();
        if (pHeader != null) {
            LOGGER.info("[{}] {}", pHeader, s);
        } else {
            LOGGER.info("{}", s);
        }
    }

    public ChatDecorator getChatDecorator() {
        return ChatDecorator.PLAIN;
    }

    public boolean logIPs() {
        return true;
    }

    public void subscribeToDebugSample(ServerPlayer pPlayer, RemoteDebugSampleType pSampleType) {
    }

    public boolean acceptsTransfers() {
        return false;
    }

    private void storeChunkIoError(CrashReport p_352397_, ChunkPos p_352348_, RegionStorageInfo p_352231_) {
        Util.ioPool().execute(() -> {
            try {
                Path path = this.getFile("debug");
                FileUtil.createDirectoriesSafe(path);
                String s = FileUtil.sanitizeName(p_352231_.level());
                Path path1 = path.resolve("chunk-" + s + "-" + Util.getFilenameFormattedDateTime() + "-server.txt");
                FileStore filestore = Files.getFileStore(path);
                long i = filestore.getUsableSpace();
                if (i < 8192L) {
                    LOGGER.warn("Not storing chunk IO report due to low space on drive {}", filestore.name());
                    return;
                }

                CrashReportCategory crashreportcategory = p_352397_.addCategory("Chunk Info");
                crashreportcategory.setDetail("Level", p_352231_::level);
                crashreportcategory.setDetail("Dimension", () -> p_352231_.dimension().location().toString());
                crashreportcategory.setDetail("Storage", p_352231_::type);
                crashreportcategory.setDetail("Position", p_352348_::toString);
                p_352397_.saveToFile(path1, ReportType.CHUNK_IO_ERROR);
                LOGGER.info("Saved details to {}", p_352397_.getSaveFile());
            } catch (Exception exception) {
                LOGGER.warn("Failed to store chunk IO exception", (Throwable)exception);
            }
        });
    }

    @Override
    public void reportChunkLoadFailure(Throwable p_352289_, RegionStorageInfo p_352335_, ChunkPos p_330507_) {
        LOGGER.error("Failed to load chunk {},{}", p_330507_.x, p_330507_.z, p_352289_);
        this.storeChunkIoError(CrashReport.forThrowable(p_352289_, "Chunk load failure"), p_330507_, p_352335_);
    }

    @Override
    public void reportChunkSaveFailure(Throwable p_352232_, RegionStorageInfo p_352253_, ChunkPos p_331741_) {
        LOGGER.error("Failed to save chunk {},{}", p_331741_.x, p_331741_.z, p_352232_);
        this.storeChunkIoError(CrashReport.forThrowable(p_352232_, "Chunk save failure"), p_331741_, p_352253_);
    }

    public PotionBrewing potionBrewing() {
        return this.potionBrewing;
    }

    public ServerLinks serverLinks() {
        return ServerLinks.EMPTY;
    }

    public static record ReloadableResources(CloseableResourceManager resourceManager, ReloadableServerResources managers) implements AutoCloseable {
        @Override
        public void close() {
            this.resourceManager.close();
        }
    }

    public static record ServerResourcePackInfo(UUID id, String url, String hash, boolean isRequired, @Nullable Component prompt) {
    }

    static class TimeProfiler {
        final long startNanos;
        final int startTick;

        TimeProfiler(long pStartNanos, int pStartTick) {
            this.startNanos = pStartNanos;
            this.startTick = pStartTick;
        }

        ProfileResults stop(final long pEndTimeNano, final int pEndTimeTicks) {
            return new ProfileResults() {
                @Override
                public List<ResultField> getTimes(String p_177972_) {
                    return Collections.emptyList();
                }

                @Override
                public boolean saveResults(Path p_177974_) {
                    return false;
                }

                @Override
                public long getStartTimeNano() {
                    return TimeProfiler.this.startNanos;
                }

                @Override
                public int getStartTimeTicks() {
                    return TimeProfiler.this.startTick;
                }

                @Override
                public long getEndTimeNano() {
                    return pEndTimeNano;
                }

                @Override
                public int getEndTimeTicks() {
                    return pEndTimeTicks;
                }

                @Override
                public String getProfilerResults() {
                    return "";
                }
            };
        }
    }
}
