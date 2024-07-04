package net.minecraft.server.level.progress;

import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class ProcessorChunkProgressListener implements ChunkProgressListener {
    private final ChunkProgressListener delegate;
    private final ProcessorMailbox<Runnable> mailbox;
    private boolean started;

    private ProcessorChunkProgressListener(ChunkProgressListener pDelegate, Executor pDispatcher) {
        this.delegate = pDelegate;
        this.mailbox = ProcessorMailbox.create(pDispatcher, "progressListener");
    }

    public static ProcessorChunkProgressListener createStarted(ChunkProgressListener pDelegate, Executor pDispatcher) {
        ProcessorChunkProgressListener processorchunkprogresslistener = new ProcessorChunkProgressListener(pDelegate, pDispatcher);
        processorchunkprogresslistener.start();
        return processorchunkprogresslistener;
    }

    @Override
    public void updateSpawnPos(ChunkPos pCenter) {
        this.mailbox.tell(() -> this.delegate.updateSpawnPos(pCenter));
    }

    @Override
    public void onStatusChange(ChunkPos pChunkPos, @Nullable ChunkStatus pChunkStatus) {
        if (this.started) {
            this.mailbox.tell(() -> this.delegate.onStatusChange(pChunkPos, pChunkStatus));
        }
    }

    @Override
    public void start() {
        this.started = true;
        this.mailbox.tell(this.delegate::start);
    }

    @Override
    public void stop() {
        this.started = false;
        this.mailbox.tell(this.delegate::stop);
    }
}
