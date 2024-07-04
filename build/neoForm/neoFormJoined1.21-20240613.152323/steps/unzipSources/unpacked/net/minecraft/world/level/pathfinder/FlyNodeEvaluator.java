package net.minecraft.world.level.pathfinder;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class FlyNodeEvaluator extends WalkNodeEvaluator {
    private final Long2ObjectMap<PathType> pathTypeByPosCache = new Long2ObjectOpenHashMap<>();
    private static final float SMALL_MOB_SIZE = 1.0F;
    private static final float SMALL_MOB_INFLATED_START_NODE_BOUNDING_BOX = 1.1F;
    private static final int MAX_START_NODE_CANDIDATES = 10;

    @Override
    public void prepare(PathNavigationRegion pLevel, Mob pMob) {
        super.prepare(pLevel, pMob);
        this.pathTypeByPosCache.clear();
        pMob.onPathfindingStart();
    }

    @Override
    public void done() {
        this.mob.onPathfindingDone();
        this.pathTypeByPosCache.clear();
        super.done();
    }

    @Override
    public Node getStart() {
        int i;
        if (this.canFloat() && this.mob.isInWater()) {
            i = this.mob.getBlockY();
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos(this.mob.getX(), (double)i, this.mob.getZ());

            for (BlockState blockstate = this.currentContext.getBlockState(blockpos$mutableblockpos);
                blockstate.is(Blocks.WATER);
                blockstate = this.currentContext.getBlockState(blockpos$mutableblockpos)
            ) {
                blockpos$mutableblockpos.set(this.mob.getX(), (double)(++i), this.mob.getZ());
            }
        } else {
            i = Mth.floor(this.mob.getY() + 0.5);
        }

        BlockPos blockpos1 = BlockPos.containing(this.mob.getX(), (double)i, this.mob.getZ());
        if (!this.canStartAt(blockpos1)) {
            for (BlockPos blockpos : this.iteratePathfindingStartNodeCandidatePositions(this.mob)) {
                if (this.canStartAt(blockpos)) {
                    return super.getStartNode(blockpos);
                }
            }
        }

        return super.getStartNode(blockpos1);
    }

    @Override
    protected boolean canStartAt(BlockPos pPos) {
        PathType pathtype = this.getCachedPathType(pPos.getX(), pPos.getY(), pPos.getZ());
        return this.mob.getPathfindingMalus(pathtype) >= 0.0F;
    }

    @Override
    public Target getTarget(double pX, double pY, double pZ) {
        return this.getTargetNodeAt(pX, pY, pZ);
    }

    @Override
    public int getNeighbors(Node[] pOutputArray, Node pNode) {
        int i = 0;
        Node node = this.findAcceptedNode(pNode.x, pNode.y, pNode.z + 1);
        if (this.isOpen(node)) {
            pOutputArray[i++] = node;
        }

        Node node1 = this.findAcceptedNode(pNode.x - 1, pNode.y, pNode.z);
        if (this.isOpen(node1)) {
            pOutputArray[i++] = node1;
        }

        Node node2 = this.findAcceptedNode(pNode.x + 1, pNode.y, pNode.z);
        if (this.isOpen(node2)) {
            pOutputArray[i++] = node2;
        }

        Node node3 = this.findAcceptedNode(pNode.x, pNode.y, pNode.z - 1);
        if (this.isOpen(node3)) {
            pOutputArray[i++] = node3;
        }

        Node node4 = this.findAcceptedNode(pNode.x, pNode.y + 1, pNode.z);
        if (this.isOpen(node4)) {
            pOutputArray[i++] = node4;
        }

        Node node5 = this.findAcceptedNode(pNode.x, pNode.y - 1, pNode.z);
        if (this.isOpen(node5)) {
            pOutputArray[i++] = node5;
        }

        Node node6 = this.findAcceptedNode(pNode.x, pNode.y + 1, pNode.z + 1);
        if (this.isOpen(node6) && this.hasMalus(node) && this.hasMalus(node4)) {
            pOutputArray[i++] = node6;
        }

        Node node7 = this.findAcceptedNode(pNode.x - 1, pNode.y + 1, pNode.z);
        if (this.isOpen(node7) && this.hasMalus(node1) && this.hasMalus(node4)) {
            pOutputArray[i++] = node7;
        }

        Node node8 = this.findAcceptedNode(pNode.x + 1, pNode.y + 1, pNode.z);
        if (this.isOpen(node8) && this.hasMalus(node2) && this.hasMalus(node4)) {
            pOutputArray[i++] = node8;
        }

        Node node9 = this.findAcceptedNode(pNode.x, pNode.y + 1, pNode.z - 1);
        if (this.isOpen(node9) && this.hasMalus(node3) && this.hasMalus(node4)) {
            pOutputArray[i++] = node9;
        }

        Node node10 = this.findAcceptedNode(pNode.x, pNode.y - 1, pNode.z + 1);
        if (this.isOpen(node10) && this.hasMalus(node) && this.hasMalus(node5)) {
            pOutputArray[i++] = node10;
        }

        Node node11 = this.findAcceptedNode(pNode.x - 1, pNode.y - 1, pNode.z);
        if (this.isOpen(node11) && this.hasMalus(node1) && this.hasMalus(node5)) {
            pOutputArray[i++] = node11;
        }

        Node node12 = this.findAcceptedNode(pNode.x + 1, pNode.y - 1, pNode.z);
        if (this.isOpen(node12) && this.hasMalus(node2) && this.hasMalus(node5)) {
            pOutputArray[i++] = node12;
        }

        Node node13 = this.findAcceptedNode(pNode.x, pNode.y - 1, pNode.z - 1);
        if (this.isOpen(node13) && this.hasMalus(node3) && this.hasMalus(node5)) {
            pOutputArray[i++] = node13;
        }

        Node node14 = this.findAcceptedNode(pNode.x + 1, pNode.y, pNode.z - 1);
        if (this.isOpen(node14) && this.hasMalus(node3) && this.hasMalus(node2)) {
            pOutputArray[i++] = node14;
        }

        Node node15 = this.findAcceptedNode(pNode.x + 1, pNode.y, pNode.z + 1);
        if (this.isOpen(node15) && this.hasMalus(node) && this.hasMalus(node2)) {
            pOutputArray[i++] = node15;
        }

        Node node16 = this.findAcceptedNode(pNode.x - 1, pNode.y, pNode.z - 1);
        if (this.isOpen(node16) && this.hasMalus(node3) && this.hasMalus(node1)) {
            pOutputArray[i++] = node16;
        }

        Node node17 = this.findAcceptedNode(pNode.x - 1, pNode.y, pNode.z + 1);
        if (this.isOpen(node17) && this.hasMalus(node) && this.hasMalus(node1)) {
            pOutputArray[i++] = node17;
        }

        Node node18 = this.findAcceptedNode(pNode.x + 1, pNode.y + 1, pNode.z - 1);
        if (this.isOpen(node18)
            && this.hasMalus(node14)
            && this.hasMalus(node3)
            && this.hasMalus(node2)
            && this.hasMalus(node4)
            && this.hasMalus(node9)
            && this.hasMalus(node8)) {
            pOutputArray[i++] = node18;
        }

        Node node19 = this.findAcceptedNode(pNode.x + 1, pNode.y + 1, pNode.z + 1);
        if (this.isOpen(node19)
            && this.hasMalus(node15)
            && this.hasMalus(node)
            && this.hasMalus(node2)
            && this.hasMalus(node4)
            && this.hasMalus(node6)
            && this.hasMalus(node8)) {
            pOutputArray[i++] = node19;
        }

        Node node20 = this.findAcceptedNode(pNode.x - 1, pNode.y + 1, pNode.z - 1);
        if (this.isOpen(node20)
            && this.hasMalus(node16)
            && this.hasMalus(node3)
            && this.hasMalus(node1)
            && this.hasMalus(node4)
            && this.hasMalus(node9)
            && this.hasMalus(node7)) {
            pOutputArray[i++] = node20;
        }

        Node node21 = this.findAcceptedNode(pNode.x - 1, pNode.y + 1, pNode.z + 1);
        if (this.isOpen(node21)
            && this.hasMalus(node17)
            && this.hasMalus(node)
            && this.hasMalus(node1)
            && this.hasMalus(node4)
            && this.hasMalus(node6)
            && this.hasMalus(node7)) {
            pOutputArray[i++] = node21;
        }

        Node node22 = this.findAcceptedNode(pNode.x + 1, pNode.y - 1, pNode.z - 1);
        if (this.isOpen(node22)
            && this.hasMalus(node14)
            && this.hasMalus(node3)
            && this.hasMalus(node2)
            && this.hasMalus(node5)
            && this.hasMalus(node13)
            && this.hasMalus(node12)) {
            pOutputArray[i++] = node22;
        }

        Node node23 = this.findAcceptedNode(pNode.x + 1, pNode.y - 1, pNode.z + 1);
        if (this.isOpen(node23)
            && this.hasMalus(node15)
            && this.hasMalus(node)
            && this.hasMalus(node2)
            && this.hasMalus(node5)
            && this.hasMalus(node10)
            && this.hasMalus(node12)) {
            pOutputArray[i++] = node23;
        }

        Node node24 = this.findAcceptedNode(pNode.x - 1, pNode.y - 1, pNode.z - 1);
        if (this.isOpen(node24)
            && this.hasMalus(node16)
            && this.hasMalus(node3)
            && this.hasMalus(node1)
            && this.hasMalus(node5)
            && this.hasMalus(node13)
            && this.hasMalus(node11)) {
            pOutputArray[i++] = node24;
        }

        Node node25 = this.findAcceptedNode(pNode.x - 1, pNode.y - 1, pNode.z + 1);
        if (this.isOpen(node25)
            && this.hasMalus(node17)
            && this.hasMalus(node)
            && this.hasMalus(node1)
            && this.hasMalus(node5)
            && this.hasMalus(node10)
            && this.hasMalus(node11)) {
            pOutputArray[i++] = node25;
        }

        return i;
    }

    private boolean hasMalus(@Nullable Node pNode) {
        return pNode != null && pNode.costMalus >= 0.0F;
    }

    private boolean isOpen(@Nullable Node pNode) {
        return pNode != null && !pNode.closed;
    }

    @Nullable
    protected Node findAcceptedNode(int pX, int pY, int pZ) {
        Node node = null;
        PathType pathtype = this.getCachedPathType(pX, pY, pZ);
        float f = this.mob.getPathfindingMalus(pathtype);
        if (f >= 0.0F) {
            node = this.getNode(pX, pY, pZ);
            node.type = pathtype;
            node.costMalus = Math.max(node.costMalus, f);
            if (pathtype == PathType.WALKABLE) {
                node.costMalus++;
            }
        }

        return node;
    }

    @Override
    protected PathType getCachedPathType(int pX, int pY, int pZ) {
        return this.pathTypeByPosCache
            .computeIfAbsent(
                BlockPos.asLong(pX, pY, pZ),
                p_330153_ -> this.getPathTypeOfMob(this.currentContext, pX, pY, pZ, this.mob)
            );
    }

    @Override
    public PathType getPathType(PathfindingContext pContext, int pX, int pY, int pZ) {
        PathType pathtype = pContext.getPathTypeFromState(pX, pY, pZ);
        if (pathtype == PathType.OPEN && pY >= pContext.level().getMinBuildHeight() + 1) {
            BlockPos blockpos = new BlockPos(pX, pY - 1, pZ);
            PathType pathtype1 = pContext.getPathTypeFromState(blockpos.getX(), blockpos.getY(), blockpos.getZ());
            if (pathtype1 == PathType.DAMAGE_FIRE || pathtype1 == PathType.LAVA) {
                pathtype = PathType.DAMAGE_FIRE;
            } else if (pathtype1 == PathType.DAMAGE_OTHER) {
                pathtype = PathType.DAMAGE_OTHER;
            } else if (pathtype1 == PathType.COCOA) {
                pathtype = PathType.COCOA;
            } else if (pathtype1 == PathType.FENCE) {
                if (!blockpos.equals(pContext.mobPosition())) {
                    pathtype = PathType.FENCE;
                }
            } else {
                pathtype = pathtype1 != PathType.WALKABLE && pathtype1 != PathType.OPEN && pathtype1 != PathType.WATER ? PathType.WALKABLE : PathType.OPEN;
            }
        }

        if (pathtype == PathType.WALKABLE || pathtype == PathType.OPEN) {
            pathtype = checkNeighbourBlocks(pContext, pX, pY, pZ, pathtype);
        }

        return pathtype;
    }

    private Iterable<BlockPos> iteratePathfindingStartNodeCandidatePositions(Mob pMob) {
        AABB aabb = pMob.getBoundingBox();
        boolean flag = aabb.getSize() < 1.0;
        if (!flag) {
            return List.of(
                BlockPos.containing(aabb.minX, (double)pMob.getBlockY(), aabb.minZ),
                BlockPos.containing(aabb.minX, (double)pMob.getBlockY(), aabb.maxZ),
                BlockPos.containing(aabb.maxX, (double)pMob.getBlockY(), aabb.minZ),
                BlockPos.containing(aabb.maxX, (double)pMob.getBlockY(), aabb.maxZ)
            );
        } else {
            double d0 = Math.max(0.0, 1.1F - aabb.getZsize());
            double d1 = Math.max(0.0, 1.1F - aabb.getXsize());
            double d2 = Math.max(0.0, 1.1F - aabb.getYsize());
            AABB aabb1 = aabb.inflate(d1, d2, d0);
            return BlockPos.randomBetweenClosed(
                pMob.getRandom(),
                10,
                Mth.floor(aabb1.minX),
                Mth.floor(aabb1.minY),
                Mth.floor(aabb1.minZ),
                Mth.floor(aabb1.maxX),
                Mth.floor(aabb1.maxY),
                Mth.floor(aabb1.maxZ)
            );
        }
    }
}
