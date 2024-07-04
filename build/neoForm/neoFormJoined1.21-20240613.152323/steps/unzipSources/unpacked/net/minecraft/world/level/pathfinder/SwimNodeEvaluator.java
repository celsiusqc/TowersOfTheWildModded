package net.minecraft.world.level.pathfinder;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class SwimNodeEvaluator extends NodeEvaluator {
    private final boolean allowBreaching;
    private final Long2ObjectMap<PathType> pathTypesByPosCache = new Long2ObjectOpenHashMap<>();

    public SwimNodeEvaluator(boolean pAllowBreaching) {
        this.allowBreaching = pAllowBreaching;
    }

    @Override
    public void prepare(PathNavigationRegion pLevel, Mob pMob) {
        super.prepare(pLevel, pMob);
        this.pathTypesByPosCache.clear();
    }

    @Override
    public void done() {
        super.done();
        this.pathTypesByPosCache.clear();
    }

    @Override
    public Node getStart() {
        return this.getNode(
            Mth.floor(this.mob.getBoundingBox().minX), Mth.floor(this.mob.getBoundingBox().minY + 0.5), Mth.floor(this.mob.getBoundingBox().minZ)
        );
    }

    @Override
    public Target getTarget(double pX, double pY, double pZ) {
        return this.getTargetNodeAt(pX, pY, pZ);
    }

    @Override
    public int getNeighbors(Node[] pOutputArray, Node pNode) {
        int i = 0;
        Map<Direction, Node> map = Maps.newEnumMap(Direction.class);

        for (Direction direction : Direction.values()) {
            Node node = this.findAcceptedNode(pNode.x + direction.getStepX(), pNode.y + direction.getStepY(), pNode.z + direction.getStepZ());
            map.put(direction, node);
            if (this.isNodeValid(node)) {
                pOutputArray[i++] = node;
            }
        }

        for (Direction direction1 : Direction.Plane.HORIZONTAL) {
            Direction direction2 = direction1.getClockWise();
            if (hasMalus(map.get(direction1)) && hasMalus(map.get(direction2))) {
                Node node1 = this.findAcceptedNode(
                    pNode.x + direction1.getStepX() + direction2.getStepX(), pNode.y, pNode.z + direction1.getStepZ() + direction2.getStepZ()
                );
                if (this.isNodeValid(node1)) {
                    pOutputArray[i++] = node1;
                }
            }
        }

        return i;
    }

    protected boolean isNodeValid(@Nullable Node pNode) {
        return pNode != null && !pNode.closed;
    }

    private static boolean hasMalus(@Nullable Node pNode) {
        return pNode != null && pNode.costMalus >= 0.0F;
    }

    @Nullable
    protected Node findAcceptedNode(int pX, int pY, int pZ) {
        Node node = null;
        PathType pathtype = this.getCachedBlockType(pX, pY, pZ);
        if (this.allowBreaching && pathtype == PathType.BREACH || pathtype == PathType.WATER) {
            float f = this.mob.getPathfindingMalus(pathtype);
            if (f >= 0.0F) {
                node = this.getNode(pX, pY, pZ);
                node.type = pathtype;
                node.costMalus = Math.max(node.costMalus, f);
                if (this.currentContext.level().getFluidState(new BlockPos(pX, pY, pZ)).isEmpty()) {
                    node.costMalus += 8.0F;
                }
            }
        }

        return node;
    }

    protected PathType getCachedBlockType(int pX, int pY, int pZ) {
        return this.pathTypesByPosCache
            .computeIfAbsent(
                BlockPos.asLong(pX, pY, pZ), p_330157_ -> this.getPathType(this.currentContext, pX, pY, pZ)
            );
    }

    @Override
    public PathType getPathType(PathfindingContext pContext, int pX, int pY, int pZ) {
        return this.getPathTypeOfMob(pContext, pX, pY, pZ, this.mob);
    }

    @Override
    public PathType getPathTypeOfMob(PathfindingContext pContext, int pX, int pY, int pZ, Mob pMob) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int i = pX; i < pX + this.entityWidth; i++) {
            for (int j = pY; j < pY + this.entityHeight; j++) {
                for (int k = pZ; k < pZ + this.entityDepth; k++) {
                    BlockState blockstate = pContext.getBlockState(blockpos$mutableblockpos.set(i, j, k));
                    FluidState fluidstate = blockstate.getFluidState();
                    if (fluidstate.isEmpty() && blockstate.isPathfindable(PathComputationType.WATER) && blockstate.isAir()) {
                        return PathType.BREACH;
                    }

                    if (!fluidstate.is(FluidTags.WATER)) {
                        return PathType.BLOCKED;
                    }
                }
            }
        }

        BlockState blockstate1 = pContext.getBlockState(blockpos$mutableblockpos);
        return blockstate1.isPathfindable(PathComputationType.WATER) ? PathType.WATER : PathType.BLOCKED;
    }
}
