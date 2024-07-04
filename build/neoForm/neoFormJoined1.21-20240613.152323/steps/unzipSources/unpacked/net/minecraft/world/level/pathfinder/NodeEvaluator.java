package net.minecraft.world.level.pathfinder;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

public abstract class NodeEvaluator {
    protected PathfindingContext currentContext;
    protected Mob mob;
    protected final Int2ObjectMap<Node> nodes = new Int2ObjectOpenHashMap<>();
    protected int entityWidth;
    protected int entityHeight;
    protected int entityDepth;
    protected boolean canPassDoors;
    protected boolean canOpenDoors;
    protected boolean canFloat;
    protected boolean canWalkOverFences;

    public void prepare(PathNavigationRegion pLevel, Mob pMob) {
        this.currentContext = new PathfindingContext(pLevel, pMob);
        this.mob = pMob;
        this.nodes.clear();
        this.entityWidth = Mth.floor(pMob.getBbWidth() + 1.0F);
        this.entityHeight = Mth.floor(pMob.getBbHeight() + 1.0F);
        this.entityDepth = Mth.floor(pMob.getBbWidth() + 1.0F);
    }

    public void done() {
        this.currentContext = null;
        this.mob = null;
    }

    protected Node getNode(BlockPos pPos) {
        return this.getNode(pPos.getX(), pPos.getY(), pPos.getZ());
    }

    /**
     * Returns a mapped point or creates and adds one
     */
    protected Node getNode(int pX, int pY, int pZ) {
        return this.nodes.computeIfAbsent(Node.createHash(pX, pY, pZ), p_77332_ -> new Node(pX, pY, pZ));
    }

    public abstract Node getStart();

    public abstract Target getTarget(double pX, double pY, double pZ);

    protected Target getTargetNodeAt(double pX, double pY, double pZ) {
        return new Target(this.getNode(Mth.floor(pX), Mth.floor(pY), Mth.floor(pZ)));
    }

    public abstract int getNeighbors(Node[] pOutputArray, Node pNode);

    public abstract PathType getPathTypeOfMob(PathfindingContext pContext, int pX, int pY, int pZ, Mob pMob);

    public abstract PathType getPathType(PathfindingContext pContext, int pX, int pY, int pZ);

    public PathType getPathType(Mob pMob, BlockPos pPos) {
        return this.getPathType(new PathfindingContext(pMob.level(), pMob), pPos.getX(), pPos.getY(), pPos.getZ());
    }

    public void setCanPassDoors(boolean pCanEnterDoors) {
        this.canPassDoors = pCanEnterDoors;
    }

    public void setCanOpenDoors(boolean pCanOpenDoors) {
        this.canOpenDoors = pCanOpenDoors;
    }

    public void setCanFloat(boolean pCanFloat) {
        this.canFloat = pCanFloat;
    }

    public void setCanWalkOverFences(boolean pCanWalkOverFences) {
        this.canWalkOverFences = pCanWalkOverFences;
    }

    public boolean canPassDoors() {
        return this.canPassDoors;
    }

    public boolean canOpenDoors() {
        return this.canOpenDoors;
    }

    public boolean canFloat() {
        return this.canFloat;
    }

    public boolean canWalkOverFences() {
        return this.canWalkOverFences;
    }

    public static boolean isBurningBlock(BlockState pState) {
        return pState.is(BlockTags.FIRE)
            || pState.is(Blocks.LAVA)
            || pState.is(Blocks.MAGMA_BLOCK)
            || CampfireBlock.isLitCampfire(pState)
            || pState.is(Blocks.LAVA_CAULDRON);
    }
}
