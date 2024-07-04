package net.minecraft.world.level.pathfinder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class Path {
    private final List<Node> nodes;
    @Nullable
    private Path.DebugData debugData;
    private int nextNodeIndex;
    private final BlockPos target;
    private final float distToTarget;
    private final boolean reached;

    public Path(List<Node> pNodes, BlockPos pTarget, boolean pReached) {
        this.nodes = pNodes;
        this.target = pTarget;
        this.distToTarget = pNodes.isEmpty() ? Float.MAX_VALUE : this.nodes.get(this.nodes.size() - 1).distanceManhattan(this.target);
        this.reached = pReached;
    }

    public void advance() {
        this.nextNodeIndex++;
    }

    public boolean notStarted() {
        return this.nextNodeIndex <= 0;
    }

    public boolean isDone() {
        return this.nextNodeIndex >= this.nodes.size();
    }

    @Nullable
    public Node getEndNode() {
        return !this.nodes.isEmpty() ? this.nodes.get(this.nodes.size() - 1) : null;
    }

    /**
     * Returns the {@link net.minecraft.world.level.pathfinder.Node} located at the specified index, usually the current one.
     */
    public Node getNode(int pIndex) {
        return this.nodes.get(pIndex);
    }

    public void truncateNodes(int pLength) {
        if (this.nodes.size() > pLength) {
            this.nodes.subList(pLength, this.nodes.size()).clear();
        }
    }

    public void replaceNode(int pIndex, Node pPoint) {
        this.nodes.set(pIndex, pPoint);
    }

    public int getNodeCount() {
        return this.nodes.size();
    }

    public int getNextNodeIndex() {
        return this.nextNodeIndex;
    }

    public void setNextNodeIndex(int pCurrentPathIndex) {
        this.nextNodeIndex = pCurrentPathIndex;
    }

    /**
     * Gets the vector of the {@link net.minecraft.world.level.pathfinder.Node} associated with the given index.
     */
    public Vec3 getEntityPosAtNode(Entity pEntity, int pIndex) {
        Node node = this.nodes.get(pIndex);
        double d0 = (double)node.x + (double)((int)(pEntity.getBbWidth() + 1.0F)) * 0.5;
        double d1 = (double)node.y;
        double d2 = (double)node.z + (double)((int)(pEntity.getBbWidth() + 1.0F)) * 0.5;
        return new Vec3(d0, d1, d2);
    }

    public BlockPos getNodePos(int pIndex) {
        return this.nodes.get(pIndex).asBlockPos();
    }

    /**
     * @return the current {@code PathEntity} target node as a {@code Vec3D}
     */
    public Vec3 getNextEntityPos(Entity pEntity) {
        return this.getEntityPosAtNode(pEntity, this.nextNodeIndex);
    }

    public BlockPos getNextNodePos() {
        return this.nodes.get(this.nextNodeIndex).asBlockPos();
    }

    public Node getNextNode() {
        return this.nodes.get(this.nextNodeIndex);
    }

    @Nullable
    public Node getPreviousNode() {
        return this.nextNodeIndex > 0 ? this.nodes.get(this.nextNodeIndex - 1) : null;
    }

    /**
     * Returns {@code true} if the EntityPath are the same. Non instance related equals.
     */
    public boolean sameAs(@Nullable Path pPathentity) {
        if (pPathentity == null) {
            return false;
        } else if (pPathentity.nodes.size() != this.nodes.size()) {
            return false;
        } else {
            for (int i = 0; i < this.nodes.size(); i++) {
                Node node = this.nodes.get(i);
                Node node1 = pPathentity.nodes.get(i);
                if (node.x != node1.x || node.y != node1.y || node.z != node1.z) {
                    return false;
                }
            }

            return true;
        }
    }

    public boolean canReach() {
        return this.reached;
    }

    @VisibleForDebug
    void setDebug(Node[] pOpenSet, Node[] pClosedSet, Set<Target> pTargetNodes) {
        this.debugData = new Path.DebugData(pOpenSet, pClosedSet, pTargetNodes);
    }

    @Nullable
    public Path.DebugData debugData() {
        return this.debugData;
    }

    public void writeToStream(FriendlyByteBuf pBuffer) {
        if (this.debugData != null && !this.debugData.targetNodes.isEmpty()) {
            pBuffer.writeBoolean(this.reached);
            pBuffer.writeInt(this.nextNodeIndex);
            pBuffer.writeBlockPos(this.target);
            pBuffer.writeCollection(this.nodes, (p_294084_, p_294085_) -> p_294085_.writeToStream(p_294084_));
            this.debugData.write(pBuffer);
        }
    }

    public static Path createFromStream(FriendlyByteBuf pBuf) {
        boolean flag = pBuf.readBoolean();
        int i = pBuf.readInt();
        BlockPos blockpos = pBuf.readBlockPos();
        List<Node> list = pBuf.readList(Node::createFromStream);
        Path.DebugData path$debugdata = Path.DebugData.read(pBuf);
        Path path = new Path(list, blockpos, flag);
        path.debugData = path$debugdata;
        path.nextNodeIndex = i;
        return path;
    }

    @Override
    public String toString() {
        return "Path(length=" + this.nodes.size() + ")";
    }

    public BlockPos getTarget() {
        return this.target;
    }

    public float getDistToTarget() {
        return this.distToTarget;
    }

    static Node[] readNodeArray(FriendlyByteBuf pBuffer) {
        Node[] anode = new Node[pBuffer.readVarInt()];

        for (int i = 0; i < anode.length; i++) {
            anode[i] = Node.createFromStream(pBuffer);
        }

        return anode;
    }

    static void writeNodeArray(FriendlyByteBuf pBuffer, Node[] pNodeArray) {
        pBuffer.writeVarInt(pNodeArray.length);

        for (Node node : pNodeArray) {
            node.writeToStream(pBuffer);
        }
    }

    public Path copy() {
        Path path = new Path(this.nodes, this.target, this.reached);
        path.debugData = this.debugData;
        path.nextNodeIndex = this.nextNodeIndex;
        return path;
    }

    public static record DebugData(Node[] openSet, Node[] closedSet, Set<Target> targetNodes) {
        public void write(FriendlyByteBuf pBuffer) {
            pBuffer.writeCollection(this.targetNodes, (p_295084_, p_294361_) -> p_294361_.writeToStream(p_295084_));
            Path.writeNodeArray(pBuffer, this.openSet);
            Path.writeNodeArray(pBuffer, this.closedSet);
        }

        public static Path.DebugData read(FriendlyByteBuf pBuffer) {
            HashSet<Target> hashset = pBuffer.readCollection(HashSet::new, Target::createFromStream);
            Node[] anode = Path.readNodeArray(pBuffer);
            Node[] anode1 = Path.readNodeArray(pBuffer);
            return new Path.DebugData(anode, anode1, hashset);
        }
    }
}
