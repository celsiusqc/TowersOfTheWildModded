package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import java.util.function.Consumer;
import net.minecraft.world.level.ChunkPos;

public interface ChunkTrackingView {
    ChunkTrackingView EMPTY = new ChunkTrackingView() {
        @Override
        public boolean contains(int p_294225_, int p_294897_, boolean p_294644_) {
            return false;
        }

        @Override
        public void forEach(Consumer<ChunkPos> p_295201_) {
        }
    };

    static ChunkTrackingView of(ChunkPos pCenter, int pViewDistance) {
        return new ChunkTrackingView.Positioned(pCenter, pViewDistance);
    }

    static void difference(ChunkTrackingView pOldChunkTrackingView, ChunkTrackingView pNewChunkTrackingView, Consumer<ChunkPos> pChunkDropper, Consumer<ChunkPos> pChunkMarker) {
        if (!pOldChunkTrackingView.equals(pNewChunkTrackingView)) {
            if (pOldChunkTrackingView instanceof ChunkTrackingView.Positioned chunktrackingview$positioned
                && pNewChunkTrackingView instanceof ChunkTrackingView.Positioned chunktrackingview$positioned1
                && chunktrackingview$positioned.squareIntersects(chunktrackingview$positioned1)) {
                int i = Math.min(chunktrackingview$positioned.minX(), chunktrackingview$positioned1.minX());
                int j = Math.min(chunktrackingview$positioned.minZ(), chunktrackingview$positioned1.minZ());
                int k = Math.max(chunktrackingview$positioned.maxX(), chunktrackingview$positioned1.maxX());
                int l = Math.max(chunktrackingview$positioned.maxZ(), chunktrackingview$positioned1.maxZ());

                for (int i1 = i; i1 <= k; i1++) {
                    for (int j1 = j; j1 <= l; j1++) {
                        boolean flag = chunktrackingview$positioned.contains(i1, j1);
                        boolean flag1 = chunktrackingview$positioned1.contains(i1, j1);
                        if (flag != flag1) {
                            if (flag1) {
                                pChunkDropper.accept(new ChunkPos(i1, j1));
                            } else {
                                pChunkMarker.accept(new ChunkPos(i1, j1));
                            }
                        }
                    }
                }

                return;
            }

            pOldChunkTrackingView.forEach(pChunkMarker);
            pNewChunkTrackingView.forEach(pChunkDropper);
        }
    }

    default boolean contains(ChunkPos pChunkPos) {
        return this.contains(pChunkPos.x, pChunkPos.z);
    }

    default boolean contains(int pX, int pZ) {
        return this.contains(pX, pZ, true);
    }

    boolean contains(int pX, int pZ, boolean pSearchAllChunks);

    void forEach(Consumer<ChunkPos> pAction);

    default boolean isInViewDistance(int pX, int pZ) {
        return this.contains(pX, pZ, false);
    }

    static boolean isInViewDistance(int pCenterX, int pCenterZ, int pViewDistance, int pX, int pZ) {
        return isWithinDistance(pCenterX, pCenterZ, pViewDistance, pX, pZ, false);
    }

    static boolean isWithinDistance(int pCenterX, int pCenterZ, int pViewDistance, int pX, int pZ, boolean pSerachAllChunks) {
        int i = Math.max(0, Math.abs(pX - pCenterX) - 1);
        int j = Math.max(0, Math.abs(pZ - pCenterZ) - 1);
        long k = (long)Math.max(0, Math.max(i, j) - (pSerachAllChunks ? 1 : 0));
        long l = (long)Math.min(i, j);
        long i1 = l * l + k * k;
        int j1 = pViewDistance * pViewDistance;
        return i1 < (long)j1;
    }

    public static record Positioned(ChunkPos center, int viewDistance) implements ChunkTrackingView {
        int minX() {
            return this.center.x - this.viewDistance - 1;
        }

        int minZ() {
            return this.center.z - this.viewDistance - 1;
        }

        int maxX() {
            return this.center.x + this.viewDistance + 1;
        }

        int maxZ() {
            return this.center.z + this.viewDistance + 1;
        }

        @VisibleForTesting
        protected boolean squareIntersects(ChunkTrackingView.Positioned pOther) {
            return this.minX() <= pOther.maxX() && this.maxX() >= pOther.minX() && this.minZ() <= pOther.maxZ() && this.maxZ() >= pOther.minZ();
        }

        @Override
        public boolean contains(int pX, int pZ, boolean pSearchAllChunks) {
            return ChunkTrackingView.isWithinDistance(this.center.x, this.center.z, this.viewDistance, pX, pZ, pSearchAllChunks);
        }

        @Override
        public void forEach(Consumer<ChunkPos> pAction) {
            for (int i = this.minX(); i <= this.maxX(); i++) {
                for (int j = this.minZ(); j <= this.maxZ(); j++) {
                    if (this.contains(i, j)) {
                        pAction.accept(new ChunkPos(i, j));
                    }
                }
            }
        }
    }
}
