package net.minecraft.world.level.levelgen.structure.structures;

import java.util.Map;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

public class ShipwreckPieces {
    private static final int NUMBER_OF_BLOCKS_ALLOWED_IN_WORLD_GEN_REGION = 32;
    static final BlockPos PIVOT = new BlockPos(4, 0, 15);
    private static final ResourceLocation[] STRUCTURE_LOCATION_BEACHED = new ResourceLocation[]{
        ResourceLocation.withDefaultNamespace("shipwreck/with_mast"),
        ResourceLocation.withDefaultNamespace("shipwreck/sideways_full"),
        ResourceLocation.withDefaultNamespace("shipwreck/sideways_fronthalf"),
        ResourceLocation.withDefaultNamespace("shipwreck/sideways_backhalf"),
        ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_full"),
        ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_fronthalf"),
        ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_backhalf"),
        ResourceLocation.withDefaultNamespace("shipwreck/with_mast_degraded"),
        ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_full_degraded"),
        ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_fronthalf_degraded"),
        ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_backhalf_degraded")
    };
    private static final ResourceLocation[] STRUCTURE_LOCATION_OCEAN = new ResourceLocation[]{
        ResourceLocation.withDefaultNamespace("shipwreck/with_mast"),
        ResourceLocation.withDefaultNamespace("shipwreck/upsidedown_full"),
        ResourceLocation.withDefaultNamespace("shipwreck/upsidedown_fronthalf"),
        ResourceLocation.withDefaultNamespace("shipwreck/upsidedown_backhalf"),
        ResourceLocation.withDefaultNamespace("shipwreck/sideways_full"),
        ResourceLocation.withDefaultNamespace("shipwreck/sideways_fronthalf"),
        ResourceLocation.withDefaultNamespace("shipwreck/sideways_backhalf"),
        ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_full"),
        ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_fronthalf"),
        ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_backhalf"),
        ResourceLocation.withDefaultNamespace("shipwreck/with_mast_degraded"),
        ResourceLocation.withDefaultNamespace("shipwreck/upsidedown_full_degraded"),
        ResourceLocation.withDefaultNamespace("shipwreck/upsidedown_fronthalf_degraded"),
        ResourceLocation.withDefaultNamespace("shipwreck/upsidedown_backhalf_degraded"),
        ResourceLocation.withDefaultNamespace("shipwreck/sideways_full_degraded"),
        ResourceLocation.withDefaultNamespace("shipwreck/sideways_fronthalf_degraded"),
        ResourceLocation.withDefaultNamespace("shipwreck/sideways_backhalf_degraded"),
        ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_full_degraded"),
        ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_fronthalf_degraded"),
        ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_backhalf_degraded")
    };
    static final Map<String, ResourceKey<LootTable>> MARKERS_TO_LOOT = Map.of(
        "map_chest",
        BuiltInLootTables.SHIPWRECK_MAP,
        "treasure_chest",
        BuiltInLootTables.SHIPWRECK_TREASURE,
        "supply_chest",
        BuiltInLootTables.SHIPWRECK_SUPPLY
    );

    public static ShipwreckPieces.ShipwreckPiece addRandomPiece(
        StructureTemplateManager pStructureTemplateManager, BlockPos pPos, Rotation pRotation, StructurePieceAccessor pPieces, RandomSource pRandom, boolean pIsBeached
    ) {
        ResourceLocation resourcelocation = Util.getRandom(pIsBeached ? STRUCTURE_LOCATION_BEACHED : STRUCTURE_LOCATION_OCEAN, pRandom);
        ShipwreckPieces.ShipwreckPiece shipwreckpieces$shipwreckpiece = new ShipwreckPieces.ShipwreckPiece(
            pStructureTemplateManager, resourcelocation, pPos, pRotation, pIsBeached
        );
        pPieces.addPiece(shipwreckpieces$shipwreckpiece);
        return shipwreckpieces$shipwreckpiece;
    }

    public static class ShipwreckPiece extends TemplateStructurePiece {
        private final boolean isBeached;

        public ShipwreckPiece(StructureTemplateManager pStructureTemplateManager, ResourceLocation pLocation, BlockPos pPos, Rotation pRotation, boolean pIsBeached) {
            super(StructurePieceType.SHIPWRECK_PIECE, 0, pStructureTemplateManager, pLocation, pLocation.toString(), makeSettings(pRotation), pPos);
            this.isBeached = pIsBeached;
        }

        public ShipwreckPiece(StructureTemplateManager pStructureTemplateManager, CompoundTag pTag) {
            super(StructurePieceType.SHIPWRECK_PIECE, pTag, pStructureTemplateManager, p_229383_ -> makeSettings(Rotation.valueOf(pTag.getString("Rot"))));
            this.isBeached = pTag.getBoolean("isBeached");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
            super.addAdditionalSaveData(pContext, pTag);
            pTag.putBoolean("isBeached", this.isBeached);
            pTag.putString("Rot", this.placeSettings.getRotation().name());
        }

        private static StructurePlaceSettings makeSettings(Rotation pRotation) {
            return new StructurePlaceSettings()
                .setRotation(pRotation)
                .setMirror(Mirror.NONE)
                .setRotationPivot(ShipwreckPieces.PIVOT)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
        }

        @Override
        protected void handleDataMarker(String pName, BlockPos pPos, ServerLevelAccessor pLevel, RandomSource pRandom, BoundingBox pBox) {
            ResourceKey<LootTable> resourcekey = ShipwreckPieces.MARKERS_TO_LOOT.get(pName);
            if (resourcekey != null) {
                RandomizableContainer.setBlockEntityLootTable(pLevel, pRandom, pPos.below(), resourcekey);
            }
        }

        @Override
        public void postProcess(
            WorldGenLevel pLevel,
            StructureManager pStructureManager,
            ChunkGenerator pGenerator,
            RandomSource pRandom,
            BoundingBox pBox,
            ChunkPos pChunkPos,
            BlockPos pPos
        ) {
            if (this.isTooBigToFitInWorldGenRegion()) {
                super.postProcess(pLevel, pStructureManager, pGenerator, pRandom, pBox, pChunkPos, pPos);
            } else {
                int i = pLevel.getMaxBuildHeight();
                int j = 0;
                Vec3i vec3i = this.template.getSize();
                Heightmap.Types heightmap$types = this.isBeached ? Heightmap.Types.WORLD_SURFACE_WG : Heightmap.Types.OCEAN_FLOOR_WG;
                int k = vec3i.getX() * vec3i.getZ();
                if (k == 0) {
                    j = pLevel.getHeight(heightmap$types, this.templatePosition.getX(), this.templatePosition.getZ());
                } else {
                    BlockPos blockpos = this.templatePosition.offset(vec3i.getX() - 1, 0, vec3i.getZ() - 1);

                    for (BlockPos blockpos1 : BlockPos.betweenClosed(this.templatePosition, blockpos)) {
                        int l = pLevel.getHeight(heightmap$types, blockpos1.getX(), blockpos1.getZ());
                        j += l;
                        i = Math.min(i, l);
                    }

                    j /= k;
                }

                this.adjustPositionHeight(this.isBeached ? this.calculateBeachedPosition(i, pRandom) : j);
                super.postProcess(pLevel, pStructureManager, pGenerator, pRandom, pBox, pChunkPos, pPos);
            }
        }

        public boolean isTooBigToFitInWorldGenRegion() {
            Vec3i vec3i = this.template.getSize();
            return vec3i.getX() > 32 || vec3i.getY() > 32;
        }

        public int calculateBeachedPosition(int pMaxHeight, RandomSource pRandom) {
            return pMaxHeight - this.template.getSize().getY() / 2 - pRandom.nextInt(3);
        }

        public void adjustPositionHeight(int pHeight) {
            this.templatePosition = new BlockPos(this.templatePosition.getX(), pHeight, this.templatePosition.getZ());
        }
    }
}
