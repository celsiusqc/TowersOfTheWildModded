package net.minecraft.world.level.levelgen.structure;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public abstract class Structure {
    public static final Codec<Structure> DIRECT_CODEC = BuiltInRegistries.STRUCTURE_TYPE.byNameCodec().dispatch(Structure::type, StructureType::codec);
    public static final Codec<Holder<Structure>> CODEC = RegistryFileCodec.create(Registries.STRUCTURE, DIRECT_CODEC);
    /** Neo: Field accesses are redirected to {@link #getModifiedStructureSettings()} with a coremod. */
    private final Structure.StructureSettings settings;

    public static <S extends Structure> RecordCodecBuilder<S, Structure.StructureSettings> settingsCodec(Instance<S> pInstance) {
        return Structure.StructureSettings.CODEC.forGetter(p_226595_ -> p_226595_.modifiableStructureInfo().getOriginalStructureInfo().structureSettings()); // FORGE: Patch codec to ignore field redirect coremods.
    }

    public static <S extends Structure> MapCodec<S> simpleCodec(Function<Structure.StructureSettings, S> pFactory) {
        return RecordCodecBuilder.mapCodec(p_226611_ -> p_226611_.group(settingsCodec(p_226611_)).apply(p_226611_, pFactory));
    }

    protected Structure(Structure.StructureSettings pSettings) {
        this.settings = pSettings;
        this.modifiableStructureInfo = new net.neoforged.neoforge.common.world.ModifiableStructureInfo(new net.neoforged.neoforge.common.world.ModifiableStructureInfo.StructureInfo(pSettings)); // FORGE: cache original structure info on construction so we can bypass our field read coremods where necessary
    }

    public HolderSet<Biome> biomes() {
        return this.settings.biomes;
    }

    public Map<MobCategory, StructureSpawnOverride> spawnOverrides() {
        return this.settings.spawnOverrides;
    }

    public GenerationStep.Decoration step() {
        return this.settings.step;
    }

    public TerrainAdjustment terrainAdaptation() {
        return this.settings.terrainAdaptation;
    }

    public BoundingBox adjustBoundingBox(BoundingBox pBoundingBox) {
        return this.terrainAdaptation() != TerrainAdjustment.NONE ? pBoundingBox.inflatedBy(12) : pBoundingBox;
    }

    public StructureStart generate(
        RegistryAccess pRegistryAccess,
        ChunkGenerator pChunkGenerator,
        BiomeSource pBiomeSource,
        RandomState pRandomState,
        StructureTemplateManager pStructureTemplateManager,
        long pSeed,
        ChunkPos pChunkPos,
        int pReferences,
        LevelHeightAccessor pHeightAccessor,
        Predicate<Holder<Biome>> pValidBiome
    ) {
        Structure.GenerationContext structure$generationcontext = new Structure.GenerationContext(
            pRegistryAccess, pChunkGenerator, pBiomeSource, pRandomState, pStructureTemplateManager, pSeed, pChunkPos, pHeightAccessor, pValidBiome
        );
        Optional<Structure.GenerationStub> optional = this.findValidGenerationPoint(structure$generationcontext);
        if (optional.isPresent()) {
            StructurePiecesBuilder structurepiecesbuilder = optional.get().getPiecesBuilder();
            StructureStart structurestart = new StructureStart(this, pChunkPos, pReferences, structurepiecesbuilder.build());
            if (structurestart.isValid()) {
                return structurestart;
            }
        }

        return StructureStart.INVALID_START;
    }

    protected static Optional<Structure.GenerationStub> onTopOfChunkCenter(
        Structure.GenerationContext pContext, Heightmap.Types pHeightmapTypes, Consumer<StructurePiecesBuilder> pGenerator
    ) {
        ChunkPos chunkpos = pContext.chunkPos();
        int i = chunkpos.getMiddleBlockX();
        int j = chunkpos.getMiddleBlockZ();
        int k = pContext.chunkGenerator().getFirstOccupiedHeight(i, j, pHeightmapTypes, pContext.heightAccessor(), pContext.randomState());
        return Optional.of(new Structure.GenerationStub(new BlockPos(i, k, j), pGenerator));
    }

    private static boolean isValidBiome(Structure.GenerationStub pStub, Structure.GenerationContext pContext) {
        BlockPos blockpos = pStub.position();
        return pContext.validBiome
            .test(
                pContext.chunkGenerator
                    .getBiomeSource()
                    .getNoiseBiome(
                        QuartPos.fromBlock(blockpos.getX()),
                        QuartPos.fromBlock(blockpos.getY()),
                        QuartPos.fromBlock(blockpos.getZ()),
                        pContext.randomState.sampler()
                    )
            );
    }

    public void afterPlace(
        WorldGenLevel pLevel,
        StructureManager pStructureManager,
        ChunkGenerator pChunkGenerator,
        RandomSource pRandom,
        BoundingBox pBoundingBox,
        ChunkPos pChunkPos,
        PiecesContainer pPieces
    ) {
    }

    private static int[] getCornerHeights(Structure.GenerationContext pContext, int pMinX, int pMaxX, int pMinZ, int pMaxZ) {
        ChunkGenerator chunkgenerator = pContext.chunkGenerator();
        LevelHeightAccessor levelheightaccessor = pContext.heightAccessor();
        RandomState randomstate = pContext.randomState();
        return new int[]{
            chunkgenerator.getFirstOccupiedHeight(pMinX, pMinZ, Heightmap.Types.WORLD_SURFACE_WG, levelheightaccessor, randomstate),
            chunkgenerator.getFirstOccupiedHeight(pMinX, pMinZ + pMaxZ, Heightmap.Types.WORLD_SURFACE_WG, levelheightaccessor, randomstate),
            chunkgenerator.getFirstOccupiedHeight(pMinX + pMaxX, pMinZ, Heightmap.Types.WORLD_SURFACE_WG, levelheightaccessor, randomstate),
            chunkgenerator.getFirstOccupiedHeight(
                pMinX + pMaxX, pMinZ + pMaxZ, Heightmap.Types.WORLD_SURFACE_WG, levelheightaccessor, randomstate
            )
        };
    }

    public static int getMeanFirstOccupiedHeight(Structure.GenerationContext pContext, int pMinX, int pMaxX, int pMinZ, int pMaxZ) {
        int[] aint = getCornerHeights(pContext, pMinX, pMaxX, pMinZ, pMaxZ);
        return (aint[0] + aint[1] + aint[2] + aint[3]) / 4;
    }

    protected static int getLowestY(Structure.GenerationContext pContext, int pMaxX, int pMaxZ) {
        ChunkPos chunkpos = pContext.chunkPos();
        int i = chunkpos.getMinBlockX();
        int j = chunkpos.getMinBlockZ();
        return getLowestY(pContext, i, j, pMaxX, pMaxZ);
    }

    protected static int getLowestY(Structure.GenerationContext pContext, int pMinX, int pMinZ, int pMaxX, int pMaxZ) {
        int[] aint = getCornerHeights(pContext, pMinX, pMaxX, pMinZ, pMaxZ);
        return Math.min(Math.min(aint[0], aint[1]), Math.min(aint[2], aint[3]));
    }

    @Deprecated
    protected BlockPos getLowestYIn5by5BoxOffset7Blocks(Structure.GenerationContext pContext, Rotation pRotation) {
        int i = 5;
        int j = 5;
        if (pRotation == Rotation.CLOCKWISE_90) {
            i = -5;
        } else if (pRotation == Rotation.CLOCKWISE_180) {
            i = -5;
            j = -5;
        } else if (pRotation == Rotation.COUNTERCLOCKWISE_90) {
            j = -5;
        }

        ChunkPos chunkpos = pContext.chunkPos();
        int k = chunkpos.getBlockX(7);
        int l = chunkpos.getBlockZ(7);
        return new BlockPos(k, getLowestY(pContext, k, l, i, j), l);
    }

    protected abstract Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext pContext);

    public Optional<Structure.GenerationStub> findValidGenerationPoint(Structure.GenerationContext pContext) {
        return this.findGenerationPoint(pContext).filter(p_262911_ -> isValidBiome(p_262911_, pContext));
    }

    public abstract StructureType<?> type();

    // Neo: Grant ability to modify Structures in specific ways such as adding new natural mob spawns to structures
    private final net.neoforged.neoforge.common.world.ModifiableStructureInfo modifiableStructureInfo;

    /**
     * {@return Cache of original structure data and structure data modified by structure modifiers}
     * Modified structure data is set by server after datapacks and serverconfigs load.
     * Settings field reads are coremodded to redirect to this.
     **/
    public net.neoforged.neoforge.common.world.ModifiableStructureInfo modifiableStructureInfo() {
        return this.modifiableStructureInfo;
    }

    /**
     * {@return The structure's settings, with modifications if called after modifiers are applied in server init.}
     */
    public StructureSettings getModifiedStructureSettings() {
        return this.modifiableStructureInfo().get().structureSettings();
    }

    public static record GenerationContext(
        RegistryAccess registryAccess,
        ChunkGenerator chunkGenerator,
        BiomeSource biomeSource,
        RandomState randomState,
        StructureTemplateManager structureTemplateManager,
        WorldgenRandom random,
        long seed,
        ChunkPos chunkPos,
        LevelHeightAccessor heightAccessor,
        Predicate<Holder<Biome>> validBiome
    ) {
        public GenerationContext(
            RegistryAccess p_226632_,
            ChunkGenerator p_226633_,
            BiomeSource p_226634_,
            RandomState p_226635_,
            StructureTemplateManager p_226636_,
            long p_226637_,
            ChunkPos p_226638_,
            LevelHeightAccessor p_226639_,
            Predicate<Holder<Biome>> p_226640_
        ) {
            this(p_226632_, p_226633_, p_226634_, p_226635_, p_226636_, makeRandom(p_226637_, p_226638_), p_226637_, p_226638_, p_226639_, p_226640_);
        }

        private static WorldgenRandom makeRandom(long pSeed, ChunkPos pChunkPos) {
            WorldgenRandom worldgenrandom = new WorldgenRandom(new LegacyRandomSource(0L));
            worldgenrandom.setLargeFeatureSeed(pSeed, pChunkPos.x, pChunkPos.z);
            return worldgenrandom;
        }
    }

    public static record GenerationStub(BlockPos position, Either<Consumer<StructurePiecesBuilder>, StructurePiecesBuilder> generator) {
        public GenerationStub(BlockPos p_226675_, Consumer<StructurePiecesBuilder> p_226676_) {
            this(p_226675_, Either.left(p_226676_));
        }

        public StructurePiecesBuilder getPiecesBuilder() {
            return this.generator.map(p_226681_ -> {
                StructurePiecesBuilder structurepiecesbuilder = new StructurePiecesBuilder();
                p_226681_.accept(structurepiecesbuilder);
                return structurepiecesbuilder;
            }, p_226679_ -> (StructurePiecesBuilder)p_226679_);
        }
    }

    public static record StructureSettings(
        HolderSet<Biome> biomes, Map<MobCategory, StructureSpawnOverride> spawnOverrides, GenerationStep.Decoration step, TerrainAdjustment terrainAdaptation
    ) {
        static final Structure.StructureSettings DEFAULT = new Structure.StructureSettings(
            HolderSet.direct(), Map.of(), GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.NONE
        );
        public static final MapCodec<Structure.StructureSettings> CODEC = RecordCodecBuilder.mapCodec(
            p_351995_ -> p_351995_.group(
                        RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(Structure.StructureSettings::biomes),
                        Codec.simpleMap(MobCategory.CODEC, StructureSpawnOverride.CODEC, StringRepresentable.keys(MobCategory.values()))
                            .fieldOf("spawn_overrides")
                            .forGetter(Structure.StructureSettings::spawnOverrides),
                        GenerationStep.Decoration.CODEC.fieldOf("step").forGetter(Structure.StructureSettings::step),
                        TerrainAdjustment.CODEC
                            .optionalFieldOf("terrain_adaptation", DEFAULT.terrainAdaptation)
                            .forGetter(Structure.StructureSettings::terrainAdaptation)
                    )
                    .apply(p_351995_, Structure.StructureSettings::new)
        );

        public StructureSettings(HolderSet<Biome> p_352235_) {
            this(p_352235_, DEFAULT.spawnOverrides, DEFAULT.step, DEFAULT.terrainAdaptation);
        }

        public static class Builder {
            private final HolderSet<Biome> biomes;
            private Map<MobCategory, StructureSpawnOverride> spawnOverrides;
            private GenerationStep.Decoration step;
            private TerrainAdjustment terrainAdaption;

            public Builder(HolderSet<Biome> p_352136_) {
                this.spawnOverrides = Structure.StructureSettings.DEFAULT.spawnOverrides;
                this.step = Structure.StructureSettings.DEFAULT.step;
                this.terrainAdaption = Structure.StructureSettings.DEFAULT.terrainAdaptation;
                this.biomes = p_352136_;
            }

            public Structure.StructureSettings.Builder spawnOverrides(Map<MobCategory, StructureSpawnOverride> p_352057_) {
                this.spawnOverrides = p_352057_;
                return this;
            }

            public Structure.StructureSettings.Builder generationStep(GenerationStep.Decoration p_352172_) {
                this.step = p_352172_;
                return this;
            }

            public Structure.StructureSettings.Builder terrainAdapation(TerrainAdjustment p_352176_) {
                this.terrainAdaption = p_352176_;
                return this;
            }

            public Structure.StructureSettings build() {
                return new Structure.StructureSettings(this.biomes, this.spawnOverrides, this.step, this.terrainAdaption);
            }
        }
    }
}
