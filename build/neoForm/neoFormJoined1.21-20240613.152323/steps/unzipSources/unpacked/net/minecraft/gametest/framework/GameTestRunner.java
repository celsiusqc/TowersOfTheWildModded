package net.minecraft.gametest.framework;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

public class GameTestRunner {
    public static final int DEFAULT_TESTS_PER_ROW = 8;
    private static final Logger LOGGER = LogUtils.getLogger();
    final ServerLevel level;
    private final GameTestTicker testTicker;
    private final List<GameTestInfo> allTestInfos;
    private ImmutableList<GameTestBatch> batches;
    final List<GameTestBatchListener> batchListeners = Lists.newArrayList();
    private final List<GameTestInfo> scheduledForRerun = Lists.newArrayList();
    private final GameTestRunner.GameTestBatcher testBatcher;
    private boolean stopped = true;
    @Nullable
    GameTestBatch currentBatch;
    private final GameTestRunner.StructureSpawner existingStructureSpawner;
    private final GameTestRunner.StructureSpawner newStructureSpawner;
    final boolean haltOnError;

    protected GameTestRunner(
        GameTestRunner.GameTestBatcher pTestBatcher,
        Collection<GameTestBatch> pBatches,
        ServerLevel pLevel,
        GameTestTicker pTestTicker,
        GameTestRunner.StructureSpawner pExistingStructureSpawner,
        GameTestRunner.StructureSpawner pNewStructureSpawner,
        boolean p_352218_
    ) {
        this.level = pLevel;
        this.testTicker = pTestTicker;
        this.testBatcher = pTestBatcher;
        this.existingStructureSpawner = pExistingStructureSpawner;
        this.newStructureSpawner = pNewStructureSpawner;
        this.batches = ImmutableList.copyOf(pBatches);
        this.haltOnError = p_352218_;
        this.allTestInfos = this.batches.stream().flatMap(p_319468_ -> p_319468_.gameTestInfos().stream()).collect(Util.toMutableList());
        pTestTicker.setRunner(this);
        this.allTestInfos.forEach(p_319464_ -> p_319464_.addListener(new ReportGameListener()));
    }

    public List<GameTestInfo> getTestInfos() {
        return this.allTestInfos;
    }

    public void start() {
        this.stopped = false;
        this.runBatch(0);
    }

    public void stop() {
        this.stopped = true;
        if (this.currentBatch != null) {
            this.currentBatch.afterBatchFunction().accept(this.level);
        }
    }

    public void rerunTest(GameTestInfo pTest) {
        GameTestInfo gametestinfo = pTest.copyReset();
        pTest.getListeners().forEach(p_319467_ -> p_319467_.testAddedForRerun(pTest, gametestinfo, this));
        this.allTestInfos.add(gametestinfo);
        this.scheduledForRerun.add(gametestinfo);
        if (this.stopped) {
            this.runScheduledRerunTests();
        }
    }

    void runBatch(final int pIndex) {
        if (pIndex >= this.batches.size()) {
            this.runScheduledRerunTests();
        } else {
            this.currentBatch = this.batches.get(pIndex);
            this.existingStructureSpawner.onBatchStart(this.level);
            this.newStructureSpawner.onBatchStart(this.level);
            Collection<GameTestInfo> collection = this.createStructuresForBatch(this.currentBatch.gameTestInfos());
            String s = this.currentBatch.name();
            LOGGER.info("Running test batch '{}' ({} tests)...", s, collection.size());
            this.currentBatch.beforeBatchFunction().accept(this.level);
            this.batchListeners.forEach(p_319463_ -> p_319463_.testBatchStarting(this.currentBatch));
            final MultipleTestTracker multipletesttracker = new MultipleTestTracker();
            collection.forEach(multipletesttracker::addTestToTrack);
            multipletesttracker.addListener(new GameTestListener() {
                private void testCompleted() {
                    if (multipletesttracker.isDone()) {
                        GameTestRunner.this.currentBatch.afterBatchFunction().accept(GameTestRunner.this.level);
                        GameTestRunner.this.batchListeners.forEach(p_320644_ -> p_320644_.testBatchFinished(GameTestRunner.this.currentBatch));
                        LongSet longset = new LongArraySet(GameTestRunner.this.level.getForcedChunks());
                        longset.forEach(p_319954_ -> GameTestRunner.this.level.setChunkForced(ChunkPos.getX(p_319954_), ChunkPos.getZ(p_319954_), false));
                        GameTestRunner.this.runBatch(pIndex + 1);
                    }
                }

                @Override
                public void testStructureLoaded(GameTestInfo p_320033_) {
                }

                @Override
                public void testPassed(GameTestInfo p_320625_, GameTestRunner p_320879_) {
                    this.testCompleted();
                }

                @Override
                public void testFailed(GameTestInfo p_320900_, GameTestRunner p_320892_) {
                    if (GameTestRunner.this.haltOnError) {
                        GameTestRunner.this.currentBatch.afterBatchFunction().accept(GameTestRunner.this.level);
                        LongSet longset = new LongArraySet(GameTestRunner.this.level.getForcedChunks());
                        longset.forEach(p_351713_ -> GameTestRunner.this.level.setChunkForced(ChunkPos.getX(p_351713_), ChunkPos.getZ(p_351713_), false));
                        GameTestTicker.SINGLETON.clear();
                    } else {
                        this.testCompleted();
                    }
                }

                @Override
                public void testAddedForRerun(GameTestInfo p_320035_, GameTestInfo p_320699_, GameTestRunner p_320447_) {
                }
            });
            collection.forEach(this.testTicker::add);
        }
    }

    private void runScheduledRerunTests() {
        if (!this.scheduledForRerun.isEmpty()) {
            LOGGER.info(
                "Starting re-run of tests: {}",
                this.scheduledForRerun.stream().map(p_325552_ -> p_325552_.getTestFunction().testName()).collect(Collectors.joining(", "))
            );
            this.batches = ImmutableList.copyOf(this.testBatcher.batch(this.scheduledForRerun));
            this.scheduledForRerun.clear();
            this.stopped = false;
            this.runBatch(0);
        } else {
            this.batches = ImmutableList.of();
            this.stopped = true;
        }
    }

    public void addListener(GameTestBatchListener pListener) {
        this.batchListeners.add(pListener);
    }

    private Collection<GameTestInfo> createStructuresForBatch(Collection<GameTestInfo> pBatch) {
        return pBatch.stream().map(this::spawn).flatMap(Optional::stream).toList();
    }

    private Optional<GameTestInfo> spawn(GameTestInfo p_320187_) {
        return p_320187_.getStructureBlockPos() == null
            ? this.newStructureSpawner.spawnStructure(p_320187_)
            : this.existingStructureSpawner.spawnStructure(p_320187_);
    }

    public static void clearMarkers(ServerLevel pServerLevel) {
        DebugPackets.sendGameTestClearPacket(pServerLevel);
    }

    public static class Builder {
        private final ServerLevel level;
        private final GameTestTicker testTicker = GameTestTicker.SINGLETON;
        private GameTestRunner.GameTestBatcher batcher = GameTestBatchFactory.fromGameTestInfo();
        private GameTestRunner.StructureSpawner existingStructureSpawner = GameTestRunner.StructureSpawner.IN_PLACE;
        private GameTestRunner.StructureSpawner newStructureSpawner = GameTestRunner.StructureSpawner.NOT_SET;
        private final Collection<GameTestBatch> batches;
        private boolean haltOnError = false;

        private Builder(Collection<GameTestBatch> pBatches, ServerLevel pLevel) {
            this.batches = pBatches;
            this.level = pLevel;
        }

        public static GameTestRunner.Builder fromBatches(Collection<GameTestBatch> pBatches, ServerLevel pLevel) {
            return new GameTestRunner.Builder(pBatches, pLevel);
        }

        public static GameTestRunner.Builder fromInfo(Collection<GameTestInfo> pInfos, ServerLevel pLevel) {
            return fromBatches(GameTestBatchFactory.fromGameTestInfo().batch(pInfos), pLevel);
        }

        public GameTestRunner.Builder haltOnError(boolean p_352186_) {
            this.haltOnError = p_352186_;
            return this;
        }

        public GameTestRunner.Builder newStructureSpawner(GameTestRunner.StructureSpawner pNewStructureSpawner) {
            this.newStructureSpawner = pNewStructureSpawner;
            return this;
        }

        public GameTestRunner.Builder existingStructureSpawner(StructureGridSpawner p_352321_) {
            this.existingStructureSpawner = p_352321_;
            return this;
        }

        public GameTestRunner.Builder batcher(GameTestRunner.GameTestBatcher p_352338_) {
            this.batcher = p_352338_;
            return this;
        }

        public GameTestRunner build() {
            return new GameTestRunner(
                this.batcher, this.batches, this.level, this.testTicker, this.existingStructureSpawner, this.newStructureSpawner, this.haltOnError
            );
        }
    }

    public interface GameTestBatcher {
        Collection<GameTestBatch> batch(Collection<GameTestInfo> pInfos);
    }

    public interface StructureSpawner {
        GameTestRunner.StructureSpawner IN_PLACE = p_320288_ -> Optional.of(p_320288_.prepareTestStructure().placeStructure().startExecution(1));
        GameTestRunner.StructureSpawner NOT_SET = p_320313_ -> Optional.empty();

        Optional<GameTestInfo> spawnStructure(GameTestInfo pGameTestInfo);

        default void onBatchStart(ServerLevel p_352207_) {
        }
    }
}
