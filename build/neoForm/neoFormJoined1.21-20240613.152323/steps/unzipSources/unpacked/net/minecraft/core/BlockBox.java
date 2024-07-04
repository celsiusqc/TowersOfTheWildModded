package net.minecraft.core;

import io.netty.buffer.ByteBuf;
import java.util.Iterator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.AABB;

public record BlockBox(BlockPos min, BlockPos max) implements Iterable<BlockPos> {
    public static final StreamCodec<ByteBuf, BlockBox> STREAM_CODEC = new StreamCodec<ByteBuf, BlockBox>() {
        public BlockBox decode(ByteBuf p_333801_) {
            return new BlockBox(FriendlyByteBuf.readBlockPos(p_333801_), FriendlyByteBuf.readBlockPos(p_333801_));
        }

        public void encode(ByteBuf p_333786_, BlockBox p_334091_) {
            FriendlyByteBuf.writeBlockPos(p_333786_, p_334091_.min());
            FriendlyByteBuf.writeBlockPos(p_333786_, p_334091_.max());
        }
    };

    public BlockBox(BlockPos min, BlockPos max) {
        this.min = BlockPos.min(min, max);
        this.max = BlockPos.max(min, max);
    }

    public static BlockBox of(BlockPos pPos) {
        return new BlockBox(pPos, pPos);
    }

    public static BlockBox of(BlockPos pPos1, BlockPos pPos2) {
        return new BlockBox(pPos1, pPos2);
    }

    public BlockBox include(BlockPos pPos) {
        return new BlockBox(BlockPos.min(this.min, pPos), BlockPos.max(this.max, pPos));
    }

    public boolean isBlock() {
        return this.min.equals(this.max);
    }

    public boolean contains(BlockPos pPos) {
        return pPos.getX() >= this.min.getX()
            && pPos.getY() >= this.min.getY()
            && pPos.getZ() >= this.min.getZ()
            && pPos.getX() <= this.max.getX()
            && pPos.getY() <= this.max.getY()
            && pPos.getZ() <= this.max.getZ();
    }

    public AABB aabb() {
        return AABB.encapsulatingFullBlocks(this.min, this.max);
    }

    @Override
    public Iterator<BlockPos> iterator() {
        return BlockPos.betweenClosed(this.min, this.max).iterator();
    }

    public int sizeX() {
        return this.max.getX() - this.min.getX() + 1;
    }

    public int sizeY() {
        return this.max.getY() - this.min.getY() + 1;
    }

    public int sizeZ() {
        return this.max.getZ() - this.min.getZ() + 1;
    }

    public BlockBox extend(Direction pDirection, int pAmount) {
        if (pAmount == 0) {
            return this;
        } else {
            return pDirection.getAxisDirection() == Direction.AxisDirection.POSITIVE
                ? of(this.min, BlockPos.max(this.min, this.max.relative(pDirection, pAmount)))
                : of(BlockPos.min(this.min.relative(pDirection, pAmount), this.max), this.max);
        }
    }

    public BlockBox move(Direction pDirection, int pAmount) {
        return pAmount == 0 ? this : new BlockBox(this.min.relative(pDirection, pAmount), this.max.relative(pDirection, pAmount));
    }

    public BlockBox offset(Vec3i pVector) {
        return new BlockBox(this.min.offset(pVector), this.max.offset(pVector));
    }
}
