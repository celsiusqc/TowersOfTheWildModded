package net.minecraft.client.renderer.chunk;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.primitives.Doubles;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexSorting;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.SectionBufferBuilderPool;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SectionRenderDispatcher {
    private static final int MAX_HIGH_PRIORITY_QUOTA = 2;
    private final PriorityBlockingQueue<SectionRenderDispatcher.RenderSection.CompileTask> toBatchHighPriority = Queues.newPriorityBlockingQueue();
    private final Queue<SectionRenderDispatcher.RenderSection.CompileTask> toBatchLowPriority = Queues.newLinkedBlockingDeque();
    private int highPriorityQuota = 2;
    private final Queue<Runnable> toUpload = Queues.newConcurrentLinkedQueue();
    final SectionBufferBuilderPack fixedBuffers;
    private final SectionBufferBuilderPool bufferPool;
    private volatile int toBatchCount;
    private volatile boolean closed;
    private final ProcessorMailbox<Runnable> mailbox;
    private final Executor executor;
    ClientLevel level;
    final LevelRenderer renderer;
    private Vec3 camera = Vec3.ZERO;
    final SectionCompiler sectionCompiler;

    public SectionRenderDispatcher(
        ClientLevel pLevel,
        LevelRenderer pRenderer,
        Executor pExecutor,
        RenderBuffers pRenderBuffers,
        BlockRenderDispatcher p_350514_,
        BlockEntityRenderDispatcher p_350550_
    ) {
        this.level = pLevel;
        this.renderer = pRenderer;
        this.fixedBuffers = pRenderBuffers.fixedBufferPack();
        this.bufferPool = pRenderBuffers.sectionBufferPool();
        this.executor = pExecutor;
        this.mailbox = ProcessorMailbox.create(pExecutor, "Section Renderer");
        this.mailbox.tell(this::runTask);
        this.sectionCompiler = new SectionCompiler(p_350514_, p_350550_);
    }

    public void setLevel(ClientLevel pLevel) {
        this.level = pLevel;
    }

    private void runTask() {
        if (!this.closed && !this.bufferPool.isEmpty()) {
            SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.pollTask();
            if (sectionrenderdispatcher$rendersection$compiletask != null) {
                SectionBufferBuilderPack sectionbufferbuilderpack = Objects.requireNonNull(this.bufferPool.acquire());
                this.toBatchCount = this.toBatchHighPriority.size() + this.toBatchLowPriority.size();
                CompletableFuture.supplyAsync(
                        Util.wrapThreadWithTaskName(
                            sectionrenderdispatcher$rendersection$compiletask.name(),
                            () -> sectionrenderdispatcher$rendersection$compiletask.doTask(sectionbufferbuilderpack)
                        ),
                        this.executor
                    )
                    .thenCompose(p_296185_ -> (CompletionStage<SectionRenderDispatcher.SectionTaskResult>)p_296185_)
                    .whenComplete((p_296170_, p_294818_) -> {
                        if (p_294818_ != null) {
                            Minecraft.getInstance().delayCrash(CrashReport.forThrowable(p_294818_, "Batching sections"));
                        } else {
                            this.mailbox.tell(() -> {
                                if (p_296170_ == SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL) {
                                    sectionbufferbuilderpack.clearAll();
                                } else {
                                    sectionbufferbuilderpack.discardAll();
                                }

                                this.bufferPool.release(sectionbufferbuilderpack);
                                this.runTask();
                            });
                        }
                    });
            }
        }
    }

    @Nullable
    private SectionRenderDispatcher.RenderSection.CompileTask pollTask() {
        if (this.highPriorityQuota <= 0) {
            SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.toBatchLowPriority.poll();
            if (sectionrenderdispatcher$rendersection$compiletask != null) {
                this.highPriorityQuota = 2;
                return sectionrenderdispatcher$rendersection$compiletask;
            }
        }

        SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask1 = this.toBatchHighPriority.poll();
        if (sectionrenderdispatcher$rendersection$compiletask1 != null) {
            this.highPriorityQuota--;
            return sectionrenderdispatcher$rendersection$compiletask1;
        } else {
            this.highPriorityQuota = 2;
            return this.toBatchLowPriority.poll();
        }
    }

    public String getStats() {
        return String.format(Locale.ROOT, "pC: %03d, pU: %02d, aB: %02d", this.toBatchCount, this.toUpload.size(), this.bufferPool.getFreeBufferCount());
    }

    public int getToBatchCount() {
        return this.toBatchCount;
    }

    public int getToUpload() {
        return this.toUpload.size();
    }

    public int getFreeBufferCount() {
        return this.bufferPool.getFreeBufferCount();
    }

    public void setCamera(Vec3 pCamera) {
        this.camera = pCamera;
    }

    public Vec3 getCameraPosition() {
        return this.camera;
    }

    public void uploadAllPendingUploads() {
        Runnable runnable;
        while ((runnable = this.toUpload.poll()) != null) {
            runnable.run();
        }
    }

    public void rebuildSectionSync(SectionRenderDispatcher.RenderSection pSection, RenderRegionCache pRegionCache) {
        pSection.compileSync(pRegionCache);
    }

    public void blockUntilClear() {
        this.clearBatchQueue();
    }

    public void schedule(SectionRenderDispatcher.RenderSection.CompileTask pTask) {
        if (!this.closed) {
            this.mailbox.tell(() -> {
                if (!this.closed) {
                    if (pTask.isHighPriority) {
                        this.toBatchHighPriority.offer(pTask);
                    } else {
                        this.toBatchLowPriority.offer(pTask);
                    }

                    this.toBatchCount = this.toBatchHighPriority.size() + this.toBatchLowPriority.size();
                    this.runTask();
                }
            });
        }
    }

    public CompletableFuture<Void> uploadSectionLayer(MeshData p_350732_, VertexBuffer p_294163_) {
        return this.closed ? CompletableFuture.completedFuture(null) : CompletableFuture.runAsync(() -> {
            if (p_294163_.isInvalid()) {
                p_350732_.close();
            } else {
                p_294163_.bind();
                p_294163_.upload(p_350732_);
                VertexBuffer.unbind();
            }
        }, this.toUpload::add);
    }

    public CompletableFuture<Void> uploadSectionIndexBuffer(ByteBufferBuilder.Result p_350933_, VertexBuffer p_350643_) {
        return this.closed ? CompletableFuture.completedFuture(null) : CompletableFuture.runAsync(() -> {
            if (p_350643_.isInvalid()) {
                p_350933_.close();
            } else {
                p_350643_.bind();
                p_350643_.uploadIndexBuffer(p_350933_);
                VertexBuffer.unbind();
            }
        }, this.toUpload::add);
    }

    private void clearBatchQueue() {
        while (!this.toBatchHighPriority.isEmpty()) {
            SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.toBatchHighPriority.poll();
            if (sectionrenderdispatcher$rendersection$compiletask != null) {
                sectionrenderdispatcher$rendersection$compiletask.cancel();
            }
        }

        while (!this.toBatchLowPriority.isEmpty()) {
            SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask1 = this.toBatchLowPriority.poll();
            if (sectionrenderdispatcher$rendersection$compiletask1 != null) {
                sectionrenderdispatcher$rendersection$compiletask1.cancel();
            }
        }

        this.toBatchCount = 0;
    }

    public boolean isQueueEmpty() {
        return this.toBatchCount == 0 && this.toUpload.isEmpty();
    }

    public void dispose() {
        this.closed = true;
        this.clearBatchQueue();
        this.uploadAllPendingUploads();
    }

    @OnlyIn(Dist.CLIENT)
    public static class CompiledSection {
        public static final SectionRenderDispatcher.CompiledSection UNCOMPILED = new SectionRenderDispatcher.CompiledSection() {
            @Override
            public boolean facesCanSeeEachother(Direction p_296238_, Direction p_294727_) {
                return false;
            }
        };
        public static final SectionRenderDispatcher.CompiledSection EMPTY = new SectionRenderDispatcher.CompiledSection() {
            @Override
            public boolean facesCanSeeEachother(Direction p_351039_, Direction p_350415_) {
                return true;
            }
        };
        final Set<RenderType> hasBlocks = new ObjectArraySet<>(RenderType.chunkBufferLayers().size());
        final List<BlockEntity> renderableBlockEntities = Lists.newArrayList();
        VisibilitySet visibilitySet = new VisibilitySet();
        @Nullable
        MeshData.SortState transparencyState;

        public boolean hasNoRenderableLayers() {
            return this.hasBlocks.isEmpty();
        }

        public boolean isEmpty(RenderType pRenderType) {
            return !this.hasBlocks.contains(pRenderType);
        }

        public List<BlockEntity> getRenderableBlockEntities() {
            return this.renderableBlockEntities;
        }

        public boolean facesCanSeeEachother(Direction pFace1, Direction pFace2) {
            return this.visibilitySet.visibilityBetween(pFace1, pFace2);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public class RenderSection {
        public static final int SIZE = 16;
        public final int index;
        public final AtomicReference<SectionRenderDispatcher.CompiledSection> compiled = new AtomicReference<>(
            SectionRenderDispatcher.CompiledSection.UNCOMPILED
        );
        private final AtomicInteger initialCompilationCancelCount = new AtomicInteger(0);
        @Nullable
        private SectionRenderDispatcher.RenderSection.RebuildTask lastRebuildTask;
        @Nullable
        private SectionRenderDispatcher.RenderSection.ResortTransparencyTask lastResortTransparencyTask;
        private final Set<BlockEntity> globalBlockEntities = Sets.newHashSet();
        private final Map<RenderType, VertexBuffer> buffers = RenderType.chunkBufferLayers()
            .stream()
            .collect(Collectors.toMap(p_295245_ -> (RenderType)p_295245_, p_295640_ -> new VertexBuffer(VertexBuffer.Usage.STATIC)));
        private AABB bb;
        private boolean dirty = true;
        final BlockPos.MutableBlockPos origin = new BlockPos.MutableBlockPos(-1, -1, -1);
        private final BlockPos.MutableBlockPos[] relativeOrigins = Util.make(new BlockPos.MutableBlockPos[6], p_294717_ -> {
            for (int i = 0; i < p_294717_.length; i++) {
                p_294717_[i] = new BlockPos.MutableBlockPos();
            }
        });
        private boolean playerChanged;

        public RenderSection(int pIndex, int pOriginX, int pOriginY, int pOriginZ) {
            this.index = pIndex;
            this.setOrigin(pOriginX, pOriginY, pOriginZ);
        }

        private boolean doesChunkExistAt(BlockPos pPos) {
            return SectionRenderDispatcher.this.level
                    .getChunk(SectionPos.blockToSectionCoord(pPos.getX()), SectionPos.blockToSectionCoord(pPos.getZ()), ChunkStatus.FULL, false)
                != null;
        }

        public boolean hasAllNeighbors() {
            int i = 24;
            return !(this.getDistToPlayerSqr() > 576.0)
                ? true
                : this.doesChunkExistAt(this.relativeOrigins[Direction.WEST.ordinal()])
                    && this.doesChunkExistAt(this.relativeOrigins[Direction.NORTH.ordinal()])
                    && this.doesChunkExistAt(this.relativeOrigins[Direction.EAST.ordinal()])
                    && this.doesChunkExistAt(this.relativeOrigins[Direction.SOUTH.ordinal()]);
        }

        public AABB getBoundingBox() {
            return this.bb;
        }

        public VertexBuffer getBuffer(RenderType pRenderType) {
            return this.buffers.get(pRenderType);
        }

        public void setOrigin(int pX, int pY, int pZ) {
            this.reset();
            this.origin.set(pX, pY, pZ);
            this.bb = new AABB(
                (double)pX, (double)pY, (double)pZ, (double)(pX + 16), (double)(pY + 16), (double)(pZ + 16)
            );

            for (Direction direction : Direction.values()) {
                this.relativeOrigins[direction.ordinal()].set(this.origin).move(direction, 16);
            }
        }

        protected double getDistToPlayerSqr() {
            Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            double d0 = this.bb.minX + 8.0 - camera.getPosition().x;
            double d1 = this.bb.minY + 8.0 - camera.getPosition().y;
            double d2 = this.bb.minZ + 8.0 - camera.getPosition().z;
            return d0 * d0 + d1 * d1 + d2 * d2;
        }

        public SectionRenderDispatcher.CompiledSection getCompiled() {
            return this.compiled.get();
        }

        private void reset() {
            this.cancelTasks();
            this.compiled.set(SectionRenderDispatcher.CompiledSection.UNCOMPILED);
            this.dirty = true;
        }

        public void releaseBuffers() {
            this.reset();
            this.buffers.values().forEach(VertexBuffer::close);
        }

        public BlockPos getOrigin() {
            return this.origin;
        }

        public void setDirty(boolean pPlayerChanged) {
            boolean flag = this.dirty;
            this.dirty = true;
            this.playerChanged = pPlayerChanged | (flag && this.playerChanged);
        }

        public void setNotDirty() {
            this.dirty = false;
            this.playerChanged = false;
        }

        public boolean isDirty() {
            return this.dirty;
        }

        public boolean isDirtyFromPlayer() {
            return this.dirty && this.playerChanged;
        }

        public BlockPos getRelativeOrigin(Direction pDirection) {
            return this.relativeOrigins[pDirection.ordinal()];
        }

        public boolean resortTransparency(RenderType pRenderType, SectionRenderDispatcher pSectionRenderDispatcher) {
            SectionRenderDispatcher.CompiledSection sectionrenderdispatcher$compiledsection = this.getCompiled();
            if (this.lastResortTransparencyTask != null) {
                this.lastResortTransparencyTask.cancel();
            }

            if (!sectionrenderdispatcher$compiledsection.hasBlocks.contains(pRenderType)) {
                return false;
            } else {
                this.lastResortTransparencyTask = new SectionRenderDispatcher.RenderSection.ResortTransparencyTask(
                    this.getDistToPlayerSqr(), sectionrenderdispatcher$compiledsection
                );
                pSectionRenderDispatcher.schedule(this.lastResortTransparencyTask);
                return true;
            }
        }

        protected boolean cancelTasks() {
            boolean flag = false;
            if (this.lastRebuildTask != null) {
                this.lastRebuildTask.cancel();
                this.lastRebuildTask = null;
                flag = true;
            }

            if (this.lastResortTransparencyTask != null) {
                this.lastResortTransparencyTask.cancel();
                this.lastResortTransparencyTask = null;
            }

            return flag;
        }

        public SectionRenderDispatcher.RenderSection.CompileTask createCompileTask(RenderRegionCache pRegionCache) {
            boolean flag = this.cancelTasks();
            var additionalRenderers = net.neoforged.neoforge.client.ClientHooks.gatherAdditionalRenderers(this.origin, SectionRenderDispatcher.this.level);
            RenderChunkRegion renderchunkregion = pRegionCache.createRegion(SectionRenderDispatcher.this.level, SectionPos.of(this.origin), additionalRenderers.isEmpty());
            boolean flag1 = this.compiled.get() == SectionRenderDispatcher.CompiledSection.UNCOMPILED;
            if (flag1 && flag) {
                this.initialCompilationCancelCount.incrementAndGet();
            }

            this.lastRebuildTask = new SectionRenderDispatcher.RenderSection.RebuildTask(
                this.getDistToPlayerSqr(), renderchunkregion, !flag1 || this.initialCompilationCancelCount.get() > 2, additionalRenderers
            );
            return this.lastRebuildTask;
        }

        public void rebuildSectionAsync(SectionRenderDispatcher pSectionRenderDispatcher, RenderRegionCache pRegionCache) {
            SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.createCompileTask(pRegionCache);
            pSectionRenderDispatcher.schedule(sectionrenderdispatcher$rendersection$compiletask);
        }

        void updateGlobalBlockEntities(Collection<BlockEntity> pBlockEntities) {
            Set<BlockEntity> set = Sets.newHashSet(pBlockEntities);
            Set<BlockEntity> set1;
            synchronized (this.globalBlockEntities) {
                set1 = Sets.newHashSet(this.globalBlockEntities);
                set.removeAll(this.globalBlockEntities);
                set1.removeAll(pBlockEntities);
                this.globalBlockEntities.clear();
                this.globalBlockEntities.addAll(pBlockEntities);
            }

            SectionRenderDispatcher.this.renderer.updateGlobalBlockEntities(set1, set);
        }

        public void compileSync(RenderRegionCache pRegionCache) {
            SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.createCompileTask(pRegionCache);
            sectionrenderdispatcher$rendersection$compiletask.doTask(SectionRenderDispatcher.this.fixedBuffers);
        }

        public boolean isAxisAlignedWith(int pX, int pY, int pZ) {
            BlockPos blockpos = this.getOrigin();
            return pX == SectionPos.blockToSectionCoord(blockpos.getX())
                || pZ == SectionPos.blockToSectionCoord(blockpos.getZ())
                || pY == SectionPos.blockToSectionCoord(blockpos.getY());
        }

        void setCompiled(SectionRenderDispatcher.CompiledSection p_350692_) {
            this.compiled.set(p_350692_);
            this.initialCompilationCancelCount.set(0);
            SectionRenderDispatcher.this.renderer.addRecentlyCompiledSection(this);
        }

        VertexSorting createVertexSorting() {
            Vec3 vec3 = SectionRenderDispatcher.this.getCameraPosition();
            return VertexSorting.byDistance(
                (float)(vec3.x - (double)this.origin.getX()), (float)(vec3.y - (double)this.origin.getY()), (float)(vec3.z - (double)this.origin.getZ())
            );
        }

        @OnlyIn(Dist.CLIENT)
        abstract class CompileTask implements Comparable<SectionRenderDispatcher.RenderSection.CompileTask> {
            protected final double distAtCreation;
            protected final AtomicBoolean isCancelled = new AtomicBoolean(false);
            protected final boolean isHighPriority;

            public CompileTask(double pDistAtCreation, boolean pIsHighPriority) {
                this.distAtCreation = pDistAtCreation;
                this.isHighPriority = pIsHighPriority;
            }

            public abstract CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack pSectionBufferBuilderPack);

            public abstract void cancel();

            protected abstract String name();

            public int compareTo(SectionRenderDispatcher.RenderSection.CompileTask pOther) {
                return Doubles.compare(this.distAtCreation, pOther.distAtCreation);
            }
        }

        @OnlyIn(Dist.CLIENT)
        class RebuildTask extends SectionRenderDispatcher.RenderSection.CompileTask {
            @Nullable
            protected RenderChunkRegion region;
            private final List<net.neoforged.neoforge.client.event.AddSectionGeometryEvent.AdditionalSectionRenderer> additionalRenderers;

            @Deprecated
            public RebuildTask(double pDistAtCreation, @Nullable RenderChunkRegion pRegion, boolean pIsHighPriority) {
                this(pDistAtCreation, pRegion, pIsHighPriority, List.of());
            }

            public RebuildTask(double pDistAtCreation, @Nullable RenderChunkRegion pRegion, boolean pIsHighPriority, List<net.neoforged.neoforge.client.event.AddSectionGeometryEvent.AdditionalSectionRenderer> additionalRenderers) {
                super(pDistAtCreation, pIsHighPriority);
                this.region = pRegion;
                this.additionalRenderers = additionalRenderers;
            }

            @Override
            protected String name() {
                return "rend_chk_rebuild";
            }

            @Override
            public CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack pSectionBufferBuilderPack) {
                if (this.isCancelled.get()) {
                    return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                } else if (!RenderSection.this.hasAllNeighbors()) {
                    this.cancel();
                    return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                } else if (this.isCancelled.get()) {
                    return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                } else {
                    RenderChunkRegion renderchunkregion = this.region;
                    this.region = null;
                    if (renderchunkregion == null) {
                        RenderSection.this.setCompiled(SectionRenderDispatcher.CompiledSection.EMPTY);
                        return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL);
                    } else {
                        SectionPos sectionpos = SectionPos.of(RenderSection.this.origin);
                        SectionCompiler.Results sectioncompiler$results = SectionRenderDispatcher.this.sectionCompiler
                            .compile(sectionpos, renderchunkregion, RenderSection.this.createVertexSorting(), pSectionBufferBuilderPack, this.additionalRenderers);
                        RenderSection.this.updateGlobalBlockEntities(sectioncompiler$results.globalBlockEntities);
                        if (this.isCancelled.get()) {
                            sectioncompiler$results.release();
                            return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                        } else {
                            SectionRenderDispatcher.CompiledSection sectionrenderdispatcher$compiledsection = new SectionRenderDispatcher.CompiledSection();
                            sectionrenderdispatcher$compiledsection.visibilitySet = sectioncompiler$results.visibilitySet;
                            sectionrenderdispatcher$compiledsection.renderableBlockEntities.addAll(sectioncompiler$results.blockEntities);
                            sectionrenderdispatcher$compiledsection.transparencyState = sectioncompiler$results.transparencyState;
                            List<CompletableFuture<Void>> list = new ArrayList<>(sectioncompiler$results.renderedLayers.size());
                            sectioncompiler$results.renderedLayers.forEach((p_349884_, p_349885_) -> {
                                list.add(SectionRenderDispatcher.this.uploadSectionLayer(p_349885_, RenderSection.this.getBuffer(p_349884_)));
                                sectionrenderdispatcher$compiledsection.hasBlocks.add(p_349884_);
                            });
                            return Util.sequenceFailFast(list).handle((p_349887_, p_349888_) -> {
                                if (p_349888_ != null && !(p_349888_ instanceof CancellationException) && !(p_349888_ instanceof InterruptedException)) {
                                    Minecraft.getInstance().delayCrash(CrashReport.forThrowable(p_349888_, "Rendering section"));
                                }

                                if (this.isCancelled.get()) {
                                    return SectionRenderDispatcher.SectionTaskResult.CANCELLED;
                                } else {
                                    RenderSection.this.setCompiled(sectionrenderdispatcher$compiledsection);
                                    return SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL;
                                }
                            });
                        }
                    }
                }
            }

            @Override
            public void cancel() {
                this.region = null;
                if (this.isCancelled.compareAndSet(false, true)) {
                    RenderSection.this.setDirty(false);
                }
            }
        }

        @OnlyIn(Dist.CLIENT)
        class ResortTransparencyTask extends SectionRenderDispatcher.RenderSection.CompileTask {
            private final SectionRenderDispatcher.CompiledSection compiledSection;

            public ResortTransparencyTask(double pDistAtCreation, SectionRenderDispatcher.CompiledSection pCompiledSection) {
                super(pDistAtCreation, true);
                this.compiledSection = pCompiledSection;
            }

            @Override
            protected String name() {
                return "rend_chk_sort";
            }

            @Override
            public CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack pSectionBufferBuilderPack) {
                if (this.isCancelled.get()) {
                    return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                } else if (!RenderSection.this.hasAllNeighbors()) {
                    this.isCancelled.set(true);
                    return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                } else if (this.isCancelled.get()) {
                    return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                } else {
                    MeshData.SortState meshdata$sortstate = this.compiledSection.transparencyState;
                    if (meshdata$sortstate != null && !this.compiledSection.isEmpty(RenderType.translucent())) {
                        VertexSorting vertexsorting = RenderSection.this.createVertexSorting();
                        ByteBufferBuilder.Result bytebufferbuilder$result = meshdata$sortstate.buildSortedIndexBuffer(
                            pSectionBufferBuilderPack.buffer(RenderType.translucent()), vertexsorting
                        );
                        if (bytebufferbuilder$result == null) {
                            return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                        } else if (this.isCancelled.get()) {
                            bytebufferbuilder$result.close();
                            return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                        } else {
                            CompletableFuture<SectionRenderDispatcher.SectionTaskResult> completablefuture = SectionRenderDispatcher.this.uploadSectionIndexBuffer(
                                    bytebufferbuilder$result, RenderSection.this.getBuffer(RenderType.translucent())
                                )
                                .thenApply(p_294714_ -> SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                            return completablefuture.handle(
                                (p_295896_, p_295826_) -> {
                                    if (p_295826_ != null && !(p_295826_ instanceof CancellationException) && !(p_295826_ instanceof InterruptedException)) {
                                        Minecraft.getInstance().delayCrash(CrashReport.forThrowable(p_295826_, "Rendering section"));
                                    }

                                    return this.isCancelled.get()
                                        ? SectionRenderDispatcher.SectionTaskResult.CANCELLED
                                        : SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL;
                                }
                            );
                        }
                    } else {
                        return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                    }
                }
            }

            @Override
            public void cancel() {
                this.isCancelled.set(true);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    static enum SectionTaskResult {
        SUCCESSFUL,
        CANCELLED;
    }
}
