package net.minecraft.world.level.levelgen.structure.pools;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SequencedPriorityIterator;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;

public class JigsawPlacement {
    static final Logger LOGGER = LogUtils.getLogger();

    public static Optional<Structure.GenerationStub> addPieces(
        Structure.GenerationContext pContext,
        Holder<StructureTemplatePool> pStartPool,
        Optional<ResourceLocation> pStartJigsawName,
        int pMaxDepth,
        BlockPos pPos,
        boolean pUseExpansionHack,
        Optional<Heightmap.Types> pProjectStartToHeightmap,
        int pMaxDistanceFromCenter,
        PoolAliasLookup pAliasLookup,
        DimensionPadding p_348489_,
        LiquidSettings p_352161_
    ) {
        RegistryAccess registryaccess = pContext.registryAccess();
        ChunkGenerator chunkgenerator = pContext.chunkGenerator();
        StructureTemplateManager structuretemplatemanager = pContext.structureTemplateManager();
        LevelHeightAccessor levelheightaccessor = pContext.heightAccessor();
        WorldgenRandom worldgenrandom = pContext.random();
        Registry<StructureTemplatePool> registry = registryaccess.registryOrThrow(Registries.TEMPLATE_POOL);
        Rotation rotation = Rotation.getRandom(worldgenrandom);
        StructureTemplatePool structuretemplatepool = pStartPool.unwrapKey()
            .flatMap(p_314915_ -> registry.getOptional(pAliasLookup.lookup((ResourceKey<StructureTemplatePool>)p_314915_)))
            .orElse(pStartPool.value());
        StructurePoolElement structurepoolelement = structuretemplatepool.getRandomTemplate(worldgenrandom);
        if (structurepoolelement == EmptyPoolElement.INSTANCE) {
            return Optional.empty();
        } else {
            BlockPos blockpos;
            if (pStartJigsawName.isPresent()) {
                ResourceLocation resourcelocation = pStartJigsawName.get();
                Optional<BlockPos> optional = getRandomNamedJigsaw(
                    structurepoolelement, resourcelocation, pPos, rotation, structuretemplatemanager, worldgenrandom
                );
                if (optional.isEmpty()) {
                    LOGGER.error(
                        "No starting jigsaw {} found in start pool {}",
                        resourcelocation,
                        pStartPool.unwrapKey().map(p_248484_ -> p_248484_.location().toString()).orElse("<unregistered>")
                    );
                    return Optional.empty();
                }

                blockpos = optional.get();
            } else {
                blockpos = pPos;
            }

            Vec3i vec3i = blockpos.subtract(pPos);
            BlockPos blockpos1 = pPos.subtract(vec3i);
            PoolElementStructurePiece poolelementstructurepiece = new PoolElementStructurePiece(
                structuretemplatemanager,
                structurepoolelement,
                blockpos1,
                structurepoolelement.getGroundLevelDelta(),
                rotation,
                structurepoolelement.getBoundingBox(structuretemplatemanager, blockpos1, rotation),
                p_352161_
            );
            BoundingBox boundingbox = poolelementstructurepiece.getBoundingBox();
            int i = (boundingbox.maxX() + boundingbox.minX()) / 2;
            int j = (boundingbox.maxZ() + boundingbox.minZ()) / 2;
            int k;
            if (pProjectStartToHeightmap.isPresent()) {
                k = pPos.getY() + chunkgenerator.getFirstFreeHeight(i, j, pProjectStartToHeightmap.get(), levelheightaccessor, pContext.randomState());
            } else {
                k = blockpos1.getY();
            }

            int l = boundingbox.minY() + poolelementstructurepiece.getGroundLevelDelta();
            poolelementstructurepiece.move(0, k - l, 0);
            int i1 = k + vec3i.getY();
            return Optional.of(
                new Structure.GenerationStub(
                    new BlockPos(i, i1, j),
                    p_352014_ -> {
                        List<PoolElementStructurePiece> list = Lists.newArrayList();
                        list.add(poolelementstructurepiece);
                        if (pMaxDepth > 0) {
                            AABB aabb = new AABB(
                                (double)(i - pMaxDistanceFromCenter),
                                (double)Math.max(i1 - pMaxDistanceFromCenter, levelheightaccessor.getMinBuildHeight() + p_348489_.bottom()),
                                (double)(j - pMaxDistanceFromCenter),
                                (double)(i + pMaxDistanceFromCenter + 1),
                                (double)Math.min(i1 + pMaxDistanceFromCenter + 1, levelheightaccessor.getMaxBuildHeight() - p_348489_.top()),
                                (double)(j + pMaxDistanceFromCenter + 1)
                            );
                            VoxelShape voxelshape = Shapes.join(Shapes.create(aabb), Shapes.create(AABB.of(boundingbox)), BooleanOp.ONLY_FIRST);
                            addPieces(
                                pContext.randomState(),
                                pMaxDepth,
                                pUseExpansionHack,
                                chunkgenerator,
                                structuretemplatemanager,
                                levelheightaccessor,
                                worldgenrandom,
                                registry,
                                poolelementstructurepiece,
                                list,
                                voxelshape,
                                pAliasLookup,
                                p_352161_
                            );
                            list.forEach(p_352014_::addPiece);
                        }
                    }
                )
            );
        }
    }

    private static Optional<BlockPos> getRandomNamedJigsaw(
        StructurePoolElement pElement,
        ResourceLocation pStartJigsawName,
        BlockPos pPos,
        Rotation pRotation,
        StructureTemplateManager pStructureTemplateManager,
        WorldgenRandom pRandom
    ) {
        List<StructureTemplate.StructureBlockInfo> list = pElement.getShuffledJigsawBlocks(pStructureTemplateManager, pPos, pRotation, pRandom);
        Optional<BlockPos> optional = Optional.empty();

        for (StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo : list) {
            ResourceLocation resourcelocation = ResourceLocation.tryParse(
                Objects.requireNonNull(structuretemplate$structureblockinfo.nbt(), () -> structuretemplate$structureblockinfo + " nbt was null")
                    .getString("name")
            );
            if (pStartJigsawName.equals(resourcelocation)) {
                optional = Optional.of(structuretemplate$structureblockinfo.pos());
                break;
            }
        }

        return optional;
    }

    private static void addPieces(
        RandomState pRandomState,
        int pMaxDepth,
        boolean pUseExpansionHack,
        ChunkGenerator pChunkGenerator,
        StructureTemplateManager pStructureTemplateManager,
        LevelHeightAccessor pLevel,
        RandomSource pRandom,
        Registry<StructureTemplatePool> pPools,
        PoolElementStructurePiece p_227219_,
        List<PoolElementStructurePiece> pPieces,
        VoxelShape pFree,
        PoolAliasLookup pAliasLookup,
        LiquidSettings p_352331_
    ) {
        JigsawPlacement.Placer jigsawplacement$placer = new JigsawPlacement.Placer(pPools, pMaxDepth, pChunkGenerator, pStructureTemplateManager, pPieces, pRandom);
        jigsawplacement$placer.tryPlacingChildren(p_227219_, new MutableObject<>(pFree), 0, pUseExpansionHack, pLevel, pRandomState, pAliasLookup, p_352331_);

        while (jigsawplacement$placer.placing.hasNext()) {
            JigsawPlacement.PieceState jigsawplacement$piecestate = jigsawplacement$placer.placing.next();
            jigsawplacement$placer.tryPlacingChildren(
                jigsawplacement$piecestate.piece,
                jigsawplacement$piecestate.free,
                jigsawplacement$piecestate.depth,
                pUseExpansionHack,
                pLevel,
                pRandomState,
                pAliasLookup,
                p_352331_
            );
        }
    }

    public static boolean generateJigsaw(
        ServerLevel pLevel, Holder<StructureTemplatePool> pStartPool, ResourceLocation pStartJigsawName, int pMaxDepth, BlockPos pPos, boolean pKeepJigsaws
    ) {
        ChunkGenerator chunkgenerator = pLevel.getChunkSource().getGenerator();
        StructureTemplateManager structuretemplatemanager = pLevel.getStructureManager();
        StructureManager structuremanager = pLevel.structureManager();
        RandomSource randomsource = pLevel.getRandom();
        Structure.GenerationContext structure$generationcontext = new Structure.GenerationContext(
            pLevel.registryAccess(),
            chunkgenerator,
            chunkgenerator.getBiomeSource(),
            pLevel.getChunkSource().randomState(),
            structuretemplatemanager,
            pLevel.getSeed(),
            new ChunkPos(pPos),
            pLevel,
            p_227255_ -> true
        );
        Optional<Structure.GenerationStub> optional = addPieces(
            structure$generationcontext,
            pStartPool,
            Optional.of(pStartJigsawName),
            pMaxDepth,
            pPos,
            false,
            Optional.empty(),
            128,
            PoolAliasLookup.EMPTY,
            JigsawStructure.DEFAULT_DIMENSION_PADDING,
            JigsawStructure.DEFAULT_LIQUID_SETTINGS
        );
        if (optional.isPresent()) {
            StructurePiecesBuilder structurepiecesbuilder = optional.get().getPiecesBuilder();

            for (StructurePiece structurepiece : structurepiecesbuilder.build().pieces()) {
                if (structurepiece instanceof PoolElementStructurePiece poolelementstructurepiece) {
                    poolelementstructurepiece.place(pLevel, structuremanager, chunkgenerator, randomsource, BoundingBox.infinite(), pPos, pKeepJigsaws);
                }
            }

            return true;
        } else {
            return false;
        }
    }

    static record PieceState(PoolElementStructurePiece piece, MutableObject<VoxelShape> free, int depth) {
    }

    static final class Placer {
        private final Registry<StructureTemplatePool> pools;
        private final int maxDepth;
        private final ChunkGenerator chunkGenerator;
        private final StructureTemplateManager structureTemplateManager;
        private final List<? super PoolElementStructurePiece> pieces;
        private final RandomSource random;
        final SequencedPriorityIterator<JigsawPlacement.PieceState> placing = new SequencedPriorityIterator<>();

        Placer(
            Registry<StructureTemplatePool> pPools,
            int pMaxDepth,
            ChunkGenerator pChunkGenerator,
            StructureTemplateManager pStructureTemplateManager,
            List<? super PoolElementStructurePiece> pPieces,
            RandomSource pRandom
        ) {
            this.pools = pPools;
            this.maxDepth = pMaxDepth;
            this.chunkGenerator = pChunkGenerator;
            this.structureTemplateManager = pStructureTemplateManager;
            this.pieces = pPieces;
            this.random = pRandom;
        }

        void tryPlacingChildren(
            PoolElementStructurePiece pPiece,
            MutableObject<VoxelShape> pFree,
            int pDepth,
            boolean pUseExpansionHack,
            LevelHeightAccessor pLevel,
            RandomState pRandomState,
            PoolAliasLookup pPoolAliasLookup,
            LiquidSettings p_352442_
        ) {
            StructurePoolElement structurepoolelement = pPiece.getElement();
            BlockPos blockpos = pPiece.getPosition();
            Rotation rotation = pPiece.getRotation();
            StructureTemplatePool.Projection structuretemplatepool$projection = structurepoolelement.getProjection();
            boolean flag = structuretemplatepool$projection == StructureTemplatePool.Projection.RIGID;
            MutableObject<VoxelShape> mutableobject = new MutableObject<>();
            BoundingBox boundingbox = pPiece.getBoundingBox();
            int i = boundingbox.minY();

            label134:
            for (StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo : structurepoolelement.getShuffledJigsawBlocks(
                this.structureTemplateManager, blockpos, rotation, this.random
            )) {
                Direction direction = JigsawBlock.getFrontFacing(structuretemplate$structureblockinfo.state());
                BlockPos blockpos1 = structuretemplate$structureblockinfo.pos();
                BlockPos blockpos2 = blockpos1.relative(direction);
                int j = blockpos1.getY() - i;
                int k = -1;
                ResourceKey<StructureTemplatePool> resourcekey = readPoolKey(structuretemplate$structureblockinfo, pPoolAliasLookup);
                Optional<? extends Holder<StructureTemplatePool>> optional = this.pools.getHolder(resourcekey);
                if (optional.isEmpty()) {
                    JigsawPlacement.LOGGER.warn("Empty or non-existent pool: {}", resourcekey.location());
                } else {
                    Holder<StructureTemplatePool> holder = (Holder<StructureTemplatePool>)optional.get();
                    if (holder.value().size() == 0 && !holder.is(Pools.EMPTY)) {
                        JigsawPlacement.LOGGER.warn("Empty or non-existent pool: {}", resourcekey.location());
                    } else {
                        Holder<StructureTemplatePool> holder1 = holder.value().getFallback();
                        if (holder1.value().size() == 0 && !holder1.is(Pools.EMPTY)) {
                            JigsawPlacement.LOGGER
                                .warn(
                                    "Empty or non-existent fallback pool: {}",
                                    holder1.unwrapKey().map(p_255599_ -> p_255599_.location().toString()).orElse("<unregistered>")
                                );
                        } else {
                            boolean flag1 = boundingbox.isInside(blockpos2);
                            MutableObject<VoxelShape> mutableobject1;
                            if (flag1) {
                                mutableobject1 = mutableobject;
                                if (mutableobject.getValue() == null) {
                                    mutableobject.setValue(Shapes.create(AABB.of(boundingbox)));
                                }
                            } else {
                                mutableobject1 = pFree;
                            }

                            List<StructurePoolElement> list = Lists.newArrayList();
                            if (pDepth != this.maxDepth) {
                                list.addAll(holder.value().getShuffledTemplates(this.random));
                            }

                            list.addAll(holder1.value().getShuffledTemplates(this.random));
                            int l = structuretemplate$structureblockinfo.nbt() != null
                                ? structuretemplate$structureblockinfo.nbt().getInt("placement_priority")
                                : 0;

                            for (StructurePoolElement structurepoolelement1 : list) {
                                if (structurepoolelement1 == EmptyPoolElement.INSTANCE) {
                                    break;
                                }

                                for (Rotation rotation1 : Rotation.getShuffled(this.random)) {
                                    List<StructureTemplate.StructureBlockInfo> list1 = structurepoolelement1.getShuffledJigsawBlocks(
                                        this.structureTemplateManager, BlockPos.ZERO, rotation1, this.random
                                    );
                                    BoundingBox boundingbox1 = structurepoolelement1.getBoundingBox(this.structureTemplateManager, BlockPos.ZERO, rotation1);
                                    int i1;
                                    if (pUseExpansionHack && boundingbox1.getYSpan() <= 16) {
                                        i1 = list1.stream()
                                            .mapToInt(
                                                p_255598_ -> {
                                                    if (!boundingbox1.isInside(p_255598_.pos().relative(JigsawBlock.getFrontFacing(p_255598_.state())))) {
                                                        return 0;
                                                    } else {
                                                        ResourceKey<StructureTemplatePool> resourcekey1 = readPoolKey(p_255598_, pPoolAliasLookup);
                                                        Optional<? extends Holder<StructureTemplatePool>> optional1 = this.pools.getHolder(resourcekey1);
                                                        Optional<Holder<StructureTemplatePool>> optional2 = optional1.map(
                                                            p_255600_ -> p_255600_.value().getFallback()
                                                        );
                                                        int k3 = optional1.<Integer>map(
                                                                p_255596_ -> p_255596_.value().getMaxSize(this.structureTemplateManager)
                                                            )
                                                            .orElse(0);
                                                        int l3 = optional2.<Integer>map(
                                                                p_255601_ -> p_255601_.value().getMaxSize(this.structureTemplateManager)
                                                            )
                                                            .orElse(0);
                                                        return Math.max(k3, l3);
                                                    }
                                                }
                                            )
                                            .max()
                                            .orElse(0);
                                    } else {
                                        i1 = 0;
                                    }

                                    for (StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo1 : list1) {
                                        if (JigsawBlock.canAttach(structuretemplate$structureblockinfo, structuretemplate$structureblockinfo1)) {
                                            BlockPos blockpos3 = structuretemplate$structureblockinfo1.pos();
                                            BlockPos blockpos4 = blockpos2.subtract(blockpos3);
                                            BoundingBox boundingbox2 = structurepoolelement1.getBoundingBox(this.structureTemplateManager, blockpos4, rotation1);
                                            int j1 = boundingbox2.minY();
                                            StructureTemplatePool.Projection structuretemplatepool$projection1 = structurepoolelement1.getProjection();
                                            boolean flag2 = structuretemplatepool$projection1 == StructureTemplatePool.Projection.RIGID;
                                            int k1 = blockpos3.getY();
                                            int l1 = j - k1 + JigsawBlock.getFrontFacing(structuretemplate$structureblockinfo.state()).getStepY();
                                            int i2;
                                            if (flag && flag2) {
                                                i2 = i + l1;
                                            } else {
                                                if (k == -1) {
                                                    k = this.chunkGenerator
                                                        .getFirstFreeHeight(
                                                            blockpos1.getX(), blockpos1.getZ(), Heightmap.Types.WORLD_SURFACE_WG, pLevel, pRandomState
                                                        );
                                                }

                                                i2 = k - k1;
                                            }

                                            int j2 = i2 - j1;
                                            BoundingBox boundingbox3 = boundingbox2.moved(0, j2, 0);
                                            BlockPos blockpos5 = blockpos4.offset(0, j2, 0);
                                            if (i1 > 0) {
                                                int k2 = Math.max(i1 + 1, boundingbox3.maxY() - boundingbox3.minY());
                                                boundingbox3.encapsulate(new BlockPos(boundingbox3.minX(), boundingbox3.minY() + k2, boundingbox3.minZ()));
                                            }

                                            if (!Shapes.joinIsNotEmpty(
                                                mutableobject1.getValue(), Shapes.create(AABB.of(boundingbox3).deflate(0.25)), BooleanOp.ONLY_SECOND
                                            )) {
                                                mutableobject1.setValue(
                                                    Shapes.joinUnoptimized(
                                                        mutableobject1.getValue(), Shapes.create(AABB.of(boundingbox3)), BooleanOp.ONLY_FIRST
                                                    )
                                                );
                                                int j3 = pPiece.getGroundLevelDelta();
                                                int l2;
                                                if (flag2) {
                                                    l2 = j3 - l1;
                                                } else {
                                                    l2 = structurepoolelement1.getGroundLevelDelta();
                                                }

                                                PoolElementStructurePiece poolelementstructurepiece = new PoolElementStructurePiece(
                                                    this.structureTemplateManager, structurepoolelement1, blockpos5, l2, rotation1, boundingbox3, p_352442_
                                                );
                                                int i3;
                                                if (flag) {
                                                    i3 = i + j;
                                                } else if (flag2) {
                                                    i3 = i2 + k1;
                                                } else {
                                                    if (k == -1) {
                                                        k = this.chunkGenerator
                                                            .getFirstFreeHeight(
                                                                blockpos1.getX(), blockpos1.getZ(), Heightmap.Types.WORLD_SURFACE_WG, pLevel, pRandomState
                                                            );
                                                    }

                                                    i3 = k + l1 / 2;
                                                }

                                                pPiece.addJunction(
                                                    new JigsawJunction(blockpos2.getX(), i3 - j + j3, blockpos2.getZ(), l1, structuretemplatepool$projection1)
                                                );
                                                poolelementstructurepiece.addJunction(
                                                    new JigsawJunction(blockpos1.getX(), i3 - k1 + l2, blockpos1.getZ(), -l1, structuretemplatepool$projection)
                                                );
                                                this.pieces.add(poolelementstructurepiece);
                                                if (pDepth + 1 <= this.maxDepth) {
                                                    JigsawPlacement.PieceState jigsawplacement$piecestate = new JigsawPlacement.PieceState(
                                                        poolelementstructurepiece, mutableobject1, pDepth + 1
                                                    );
                                                    this.placing.add(jigsawplacement$piecestate, l);
                                                }
                                                continue label134;
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

        private static ResourceKey<StructureTemplatePool> readPoolKey(StructureTemplate.StructureBlockInfo pBlockInfo, PoolAliasLookup pAliasLookup) {
            CompoundTag compoundtag = Objects.requireNonNull(pBlockInfo.nbt(), () -> pBlockInfo + " nbt was null");
            ResourceKey<StructureTemplatePool> resourcekey = Pools.parseKey(compoundtag.getString("pool"));
            return pAliasLookup.lookup(resourcekey);
        }
    }
}
