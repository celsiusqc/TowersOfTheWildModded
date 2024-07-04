package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.world.level.ChunkPos;

public class ChunkTaskPriorityQueue<T> {
    public static final int PRIORITY_LEVEL_COUNT = ChunkLevel.MAX_LEVEL + 2;
    private final List<Long2ObjectLinkedOpenHashMap<List<Optional<T>>>> taskQueue = IntStream.range(0, PRIORITY_LEVEL_COUNT)
        .mapToObj(p_140520_ -> new Long2ObjectLinkedOpenHashMap<List<Optional<T>>>())
        .collect(Collectors.toList());
    private volatile int firstQueue = PRIORITY_LEVEL_COUNT;
    private final String name;
    private final LongSet acquired = new LongOpenHashSet();
    private final int maxTasks;

    public ChunkTaskPriorityQueue(String pName, int pMaxTasks) {
        this.name = pName;
        this.maxTasks = pMaxTasks;
    }

    protected void resortChunkTasks(int pQueueLevel, ChunkPos pChunkPos, int pTicketLevel) {
        if (pQueueLevel < PRIORITY_LEVEL_COUNT) {
            Long2ObjectLinkedOpenHashMap<List<Optional<T>>> long2objectlinkedopenhashmap = this.taskQueue.get(pQueueLevel);
            List<Optional<T>> list = long2objectlinkedopenhashmap.remove(pChunkPos.toLong());
            if (pQueueLevel == this.firstQueue) {
                while (this.hasWork() && this.taskQueue.get(this.firstQueue).isEmpty()) {
                    this.firstQueue++;
                }
            }

            if (list != null && !list.isEmpty()) {
                this.taskQueue.get(pTicketLevel).computeIfAbsent(pChunkPos.toLong(), p_140547_ -> Lists.newArrayList()).addAll(list);
                this.firstQueue = Math.min(this.firstQueue, pTicketLevel);
            }
        }
    }

    protected void submit(Optional<T> pTask, long pChunkPos, int pChunkLevel) {
        this.taskQueue.get(pChunkLevel).computeIfAbsent(pChunkPos, p_140545_ -> Lists.newArrayList()).add(pTask);
        this.firstQueue = Math.min(this.firstQueue, pChunkLevel);
    }

    protected void release(long pChunkPos, boolean pFullClear) {
        for (Long2ObjectLinkedOpenHashMap<List<Optional<T>>> long2objectlinkedopenhashmap : this.taskQueue) {
            List<Optional<T>> list = long2objectlinkedopenhashmap.get(pChunkPos);
            if (list != null) {
                if (pFullClear) {
                    list.clear();
                } else {
                    list.removeIf(p_297951_ -> p_297951_.isEmpty());
                }

                if (list.isEmpty()) {
                    long2objectlinkedopenhashmap.remove(pChunkPos);
                }
            }
        }

        while (this.hasWork() && this.taskQueue.get(this.firstQueue).isEmpty()) {
            this.firstQueue++;
        }

        this.acquired.remove(pChunkPos);
    }

    private Runnable acquire(long pChunkPos) {
        return () -> this.acquired.add(pChunkPos);
    }

    @Nullable
    public Stream<Either<T, Runnable>> pop() {
        if (this.acquired.size() >= this.maxTasks) {
            return null;
        } else if (!this.hasWork()) {
            return null;
        } else {
            int i = this.firstQueue;
            Long2ObjectLinkedOpenHashMap<List<Optional<T>>> long2objectlinkedopenhashmap = this.taskQueue.get(i);
            long j = long2objectlinkedopenhashmap.firstLongKey();
            List<Optional<T>> list = long2objectlinkedopenhashmap.removeFirst();

            while (this.hasWork() && this.taskQueue.get(this.firstQueue).isEmpty()) {
                this.firstQueue++;
            }

            return list.stream().map(p_140529_ -> p_140529_.<Either<T,Runnable>>map(Either::left).orElseGet(() -> Either.right(this.acquire(j))));
        }
    }

    public boolean hasWork() {
        return this.firstQueue < PRIORITY_LEVEL_COUNT;
    }

    @Override
    public String toString() {
        return this.name + " " + this.firstQueue + "...";
    }

    @VisibleForTesting
    LongSet getAcquired() {
        return new LongOpenHashSet(this.acquired);
    }
}
