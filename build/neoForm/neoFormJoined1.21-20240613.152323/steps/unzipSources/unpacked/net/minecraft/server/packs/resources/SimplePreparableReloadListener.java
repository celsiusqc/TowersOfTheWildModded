package net.minecraft.server.packs.resources;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.util.profiling.ProfilerFiller;

public abstract class SimplePreparableReloadListener<T> extends net.neoforged.neoforge.resource.ContextAwareReloadListener implements PreparableReloadListener {
    @Override
    public final CompletableFuture<Void> reload(
        PreparableReloadListener.PreparationBarrier pStage,
        ResourceManager pResourceManager,
        ProfilerFiller pPreparationsProfiler,
        ProfilerFiller pReloadProfiler,
        Executor pBackgroundExecutor,
        Executor pGameExecutor
    ) {
        return CompletableFuture.<T>supplyAsync(() -> this.prepare(pResourceManager, pPreparationsProfiler), pBackgroundExecutor)
            .thenCompose(pStage::wait)
            .thenAcceptAsync(p_10792_ -> this.apply((T)p_10792_, pResourceManager, pReloadProfiler), pGameExecutor);
    }

    /**
     * Performs any reloading that can be done off-thread, such as file IO
     */
    protected abstract T prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler);

    protected abstract void apply(T pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler);
}
