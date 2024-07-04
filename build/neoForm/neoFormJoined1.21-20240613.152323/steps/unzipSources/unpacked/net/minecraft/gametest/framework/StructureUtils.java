package net.minecraft.gametest.framework;

import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class StructureUtils {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int DEFAULT_Y_SEARCH_RADIUS = 10;
    public static final String DEFAULT_TEST_STRUCTURES_DIR = "gameteststructures";
    public static String testStructuresDir = "gameteststructures";

    public static Rotation getRotationForRotationSteps(int pRotationSteps) {
        switch (pRotationSteps) {
            case 0:
                return Rotation.NONE;
            case 1:
                return Rotation.CLOCKWISE_90;
            case 2:
                return Rotation.CLOCKWISE_180;
            case 3:
                return Rotation.COUNTERCLOCKWISE_90;
            default:
                throw new IllegalArgumentException("rotationSteps must be a value from 0-3. Got value " + pRotationSteps);
        }
    }

    public static int getRotationStepsForRotation(Rotation pRotation) {
        switch (pRotation) {
            case NONE:
                return 0;
            case CLOCKWISE_90:
                return 1;
            case CLOCKWISE_180:
                return 2;
            case COUNTERCLOCKWISE_90:
                return 3;
            default:
                throw new IllegalArgumentException("Unknown rotation value, don't know how many steps it represents: " + pRotation);
        }
    }

    public static AABB getStructureBounds(StructureBlockEntity pStructureBlockEntity) {
        return AABB.of(getStructureBoundingBox(pStructureBlockEntity));
    }

    public static BoundingBox getStructureBoundingBox(StructureBlockEntity pStructureBlockEntity) {
        BlockPos blockpos = getStructureOrigin(pStructureBlockEntity);
        BlockPos blockpos1 = getTransformedFarCorner(blockpos, pStructureBlockEntity.getStructureSize(), pStructureBlockEntity.getRotation());
        return BoundingBox.fromCorners(blockpos, blockpos1);
    }

    public static BlockPos getStructureOrigin(StructureBlockEntity pStructureBlockEntity) {
        return pStructureBlockEntity.getBlockPos().offset(pStructureBlockEntity.getStructurePos());
    }

    public static void addCommandBlockAndButtonToStartTest(BlockPos pStructureBlockPos, BlockPos pOffset, Rotation pRotation, ServerLevel pServerLevel) {
        BlockPos blockpos = StructureTemplate.transform(pStructureBlockPos.offset(pOffset), Mirror.NONE, pRotation, pStructureBlockPos);
        pServerLevel.setBlockAndUpdate(blockpos, Blocks.COMMAND_BLOCK.defaultBlockState());
        CommandBlockEntity commandblockentity = (CommandBlockEntity)pServerLevel.getBlockEntity(blockpos);
        commandblockentity.getCommandBlock().setCommand("test runclosest");
        BlockPos blockpos1 = StructureTemplate.transform(blockpos.offset(0, 0, -1), Mirror.NONE, pRotation, blockpos);
        pServerLevel.setBlockAndUpdate(blockpos1, Blocks.STONE_BUTTON.defaultBlockState().rotate(pRotation));
    }

    public static void createNewEmptyStructureBlock(String pStructureName, BlockPos pPos, Vec3i pSize, Rotation pRotation, ServerLevel pServerLevel) {
        BoundingBox boundingbox = getStructureBoundingBox(pPos.above(), pSize, pRotation);
        clearSpaceForStructure(boundingbox, pServerLevel);
        pServerLevel.setBlockAndUpdate(pPos, Blocks.STRUCTURE_BLOCK.defaultBlockState());
        StructureBlockEntity structureblockentity = (StructureBlockEntity)pServerLevel.getBlockEntity(pPos);
        structureblockentity.setIgnoreEntities(false);
        structureblockentity.setStructureName(ResourceLocation.parse(pStructureName));
        structureblockentity.setStructureSize(pSize);
        structureblockentity.setMode(StructureMode.SAVE);
        structureblockentity.setShowBoundingBox(true);
    }

    public static StructureBlockEntity prepareTestStructure(GameTestInfo pGameTestInfo, BlockPos pPos, Rotation pRotation, ServerLevel pLevel) {
        Vec3i vec3i = pLevel.getStructureManager()
            .get(ResourceLocation.parse(pGameTestInfo.getStructureName()))
            .orElseThrow(() -> new IllegalStateException("Missing test structure: " + pGameTestInfo.getStructureName()))
            .getSize();
        BoundingBox boundingbox = getStructureBoundingBox(pPos, vec3i, pRotation);
        BlockPos blockpos;
        if (pRotation == Rotation.NONE) {
            blockpos = pPos;
        } else if (pRotation == Rotation.CLOCKWISE_90) {
            blockpos = pPos.offset(vec3i.getZ() - 1, 0, 0);
        } else if (pRotation == Rotation.CLOCKWISE_180) {
            blockpos = pPos.offset(vec3i.getX() - 1, 0, vec3i.getZ() - 1);
        } else {
            if (pRotation != Rotation.COUNTERCLOCKWISE_90) {
                throw new IllegalArgumentException("Invalid rotation: " + pRotation);
            }

            blockpos = pPos.offset(0, 0, vec3i.getX() - 1);
        }

        forceLoadChunks(boundingbox, pLevel);
        clearSpaceForStructure(boundingbox, pLevel);
        return createStructureBlock(pGameTestInfo, blockpos.below(), pRotation, pLevel);
    }

    public static void encaseStructure(AABB pBounds, ServerLevel pLevel, boolean pPlaceBarriers) {
        BlockPos blockpos = BlockPos.containing(pBounds.minX, pBounds.minY, pBounds.minZ).offset(-1, 0, -1);
        BlockPos blockpos1 = BlockPos.containing(pBounds.maxX, pBounds.maxY, pBounds.maxZ);
        BlockPos.betweenClosedStream(blockpos, blockpos1)
            .forEach(
                p_326745_ -> {
                    boolean flag = p_326745_.getX() == blockpos.getX()
                        || p_326745_.getX() == blockpos1.getX()
                        || p_326745_.getZ() == blockpos.getZ()
                        || p_326745_.getZ() == blockpos1.getZ();
                    boolean flag1 = p_326745_.getY() == blockpos1.getY();
                    if (flag || flag1 && pPlaceBarriers) {
                        pLevel.setBlockAndUpdate(p_326745_, Blocks.BARRIER.defaultBlockState());
                    }
                }
            );
    }

    public static void removeBarriers(AABB pBounds, ServerLevel pLevel) {
        BlockPos blockpos = BlockPos.containing(pBounds.minX, pBounds.minY, pBounds.minZ).offset(-1, 0, -1);
        BlockPos blockpos1 = BlockPos.containing(pBounds.maxX, pBounds.maxY, pBounds.maxZ);
        BlockPos.betweenClosedStream(blockpos, blockpos1)
            .forEach(
                p_326740_ -> {
                    boolean flag = p_326740_.getX() == blockpos.getX()
                        || p_326740_.getX() == blockpos1.getX()
                        || p_326740_.getZ() == blockpos.getZ()
                        || p_326740_.getZ() == blockpos1.getZ();
                    boolean flag1 = p_326740_.getY() == blockpos1.getY();
                    if (pLevel.getBlockState(p_326740_).is(Blocks.BARRIER) && (flag || flag1)) {
                        pLevel.setBlockAndUpdate(p_326740_, Blocks.AIR.defaultBlockState());
                    }
                }
            );
    }

    private static void forceLoadChunks(BoundingBox pBoundingBox, ServerLevel pLevel) {
        pBoundingBox.intersectingChunks().forEach(p_308480_ -> pLevel.setChunkForced(p_308480_.x, p_308480_.z, true));
    }

    public static void clearSpaceForStructure(BoundingBox pBoundingBox, ServerLevel pLevel) {
        int i = pBoundingBox.minY() - 1;
        BoundingBox boundingbox = new BoundingBox(
            pBoundingBox.minX() - 2, pBoundingBox.minY() - 3, pBoundingBox.minZ() - 3, pBoundingBox.maxX() + 3, pBoundingBox.maxY() + 20, pBoundingBox.maxZ() + 3
        );
        BlockPos.betweenClosedStream(boundingbox).forEach(p_177748_ -> clearBlock(i, p_177748_, pLevel));
        pLevel.getBlockTicks().clearArea(boundingbox);
        pLevel.clearBlockEvents(boundingbox);
        AABB aabb = AABB.of(boundingbox);
        List<Entity> list = pLevel.getEntitiesOfClass(Entity.class, aabb, p_177750_ -> !(p_177750_ instanceof Player));
        list.forEach(Entity::discard);
    }

    public static BlockPos getTransformedFarCorner(BlockPos pPos, Vec3i pOffset, Rotation pRotation) {
        BlockPos blockpos = pPos.offset(pOffset).offset(-1, -1, -1);
        return StructureTemplate.transform(blockpos, Mirror.NONE, pRotation, pPos);
    }

    public static BoundingBox getStructureBoundingBox(BlockPos pPos, Vec3i pOffset, Rotation pRotation) {
        BlockPos blockpos = getTransformedFarCorner(pPos, pOffset, pRotation);
        BoundingBox boundingbox = BoundingBox.fromCorners(pPos, blockpos);
        int i = Math.min(boundingbox.minX(), boundingbox.maxX());
        int j = Math.min(boundingbox.minZ(), boundingbox.maxZ());
        return boundingbox.move(pPos.getX() - i, 0, pPos.getZ() - j);
    }

    public static Optional<BlockPos> findStructureBlockContainingPos(BlockPos pPos, int pRadius, ServerLevel pServerLevel) {
        return findStructureBlocks(pPos, pRadius, pServerLevel).filter(p_177756_ -> doesStructureContain(p_177756_, pPos, pServerLevel)).findFirst();
    }

    public static Optional<BlockPos> findNearestStructureBlock(BlockPos pPos, int pRadius, ServerLevel pLevel) {
        Comparator<BlockPos> comparator = Comparator.comparingInt(p_177759_ -> p_177759_.distManhattan(pPos));
        return findStructureBlocks(pPos, pRadius, pLevel).min(comparator);
    }

    public static Stream<BlockPos> findStructureByTestFunction(BlockPos pPos, int pRadius, ServerLevel pLevel, String pTestName) {
        return findStructureBlocks(pPos, pRadius, pLevel)
            .map(p_340630_ -> (StructureBlockEntity)pLevel.getBlockEntity(p_340630_))
            .filter(Objects::nonNull)
            .filter(p_340628_ -> Objects.equals(p_340628_.getStructureName(), pTestName))
            .map(BlockEntity::getBlockPos)
            .map(BlockPos::immutable);
    }

    public static Stream<BlockPos> findStructureBlocks(BlockPos pPos, int pRadius, ServerLevel pLevel) {
        BoundingBox boundingbox = getBoundingBoxAtGround(pPos, pRadius, pLevel);
        return BlockPos.betweenClosedStream(boundingbox)
            .filter(p_319470_ -> pLevel.getBlockState(p_319470_).is(Blocks.STRUCTURE_BLOCK))
            .map(BlockPos::immutable);
    }

    private static StructureBlockEntity createStructureBlock(GameTestInfo pGameTestInfo, BlockPos pPos, Rotation pRotation, ServerLevel pLevel) {
        pLevel.setBlockAndUpdate(pPos, Blocks.STRUCTURE_BLOCK.defaultBlockState());
        StructureBlockEntity structureblockentity = (StructureBlockEntity)pLevel.getBlockEntity(pPos);
        structureblockentity.setMode(StructureMode.LOAD);
        structureblockentity.setRotation(pRotation);
        structureblockentity.setIgnoreEntities(false);
        structureblockentity.setStructureName(ResourceLocation.parse(pGameTestInfo.getStructureName()));
        structureblockentity.setMetaData(pGameTestInfo.getTestName());
        if (!structureblockentity.loadStructureInfo(pLevel)) {
            throw new RuntimeException(
                "Failed to load structure info for test: " + pGameTestInfo.getTestName() + ". Structure name: " + pGameTestInfo.getStructureName()
            );
        } else {
            return structureblockentity;
        }
    }

    private static BoundingBox getBoundingBoxAtGround(BlockPos pPos, int pRadius, ServerLevel pLevel) {
        BlockPos blockpos = BlockPos.containing(
            (double)pPos.getX(), (double)pLevel.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, pPos).getY(), (double)pPos.getZ()
        );
        return new BoundingBox(blockpos).inflatedBy(pRadius, 10, pRadius);
    }

    public static Stream<BlockPos> lookedAtStructureBlockPos(BlockPos pPos, Entity pEntity, ServerLevel pLevel) {
        int i = 200;
        Vec3 vec3 = pEntity.getEyePosition();
        Vec3 vec31 = vec3.add(pEntity.getLookAngle().scale(200.0));
        return findStructureBlocks(pPos, 200, pLevel)
            .map(p_319477_ -> pLevel.getBlockEntity(p_319477_, BlockEntityType.STRUCTURE_BLOCK))
            .flatMap(Optional::stream)
            .filter(p_319475_ -> getStructureBounds(p_319475_).clip(vec3, vec31).isPresent())
            .map(BlockEntity::getBlockPos)
            .sorted(Comparator.comparing(pPos::distSqr))
            .limit(1L);
    }

    private static void clearBlock(int pStructureBlockY, BlockPos pPos, ServerLevel pServerLevel) {
        BlockState blockstate;
        if (pPos.getY() < pStructureBlockY) {
            blockstate = Blocks.STONE.defaultBlockState();
        } else {
            blockstate = Blocks.AIR.defaultBlockState();
        }

        BlockInput blockinput = new BlockInput(blockstate, Collections.emptySet(), null);
        blockinput.place(pServerLevel, pPos, 2);
        pServerLevel.blockUpdated(pPos, blockstate.getBlock());
    }

    private static boolean doesStructureContain(BlockPos pStructureBlockPos, BlockPos pPosToTest, ServerLevel pServerLevel) {
        StructureBlockEntity structureblockentity = (StructureBlockEntity)pServerLevel.getBlockEntity(pStructureBlockPos);
        return getStructureBoundingBox(structureblockentity).isInside(pPosToTest);
    }
}
