package net.minecraft.tags;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

public class TagManager implements PreparableReloadListener {
    private final RegistryAccess registryAccess;
    private List<TagManager.LoadResult<?>> results = List.of();

    public TagManager(RegistryAccess pRegistryAccess) {
        this.registryAccess = pRegistryAccess;
    }

    public List<TagManager.LoadResult<?>> getResult() {
        return this.results;
    }

    @Override
    public CompletableFuture<Void> reload(
        PreparableReloadListener.PreparationBarrier pStage,
        ResourceManager pResourceManager,
        ProfilerFiller pPreparationsProfiler,
        ProfilerFiller pReloadProfiler,
        Executor pBackgroundExecutor,
        Executor pGameExecutor
    ) {
        List<? extends CompletableFuture<? extends TagManager.LoadResult<?>>> list = this.registryAccess
            .registries()
            .map(p_203927_ -> this.createLoader(pResourceManager, pBackgroundExecutor, (RegistryAccess.RegistryEntry<?>)p_203927_))
            .toList();
        return CompletableFuture.allOf(list.toArray(CompletableFuture[]::new))
            .thenCompose(pStage::wait)
            .thenAcceptAsync(p_203917_ -> this.results = list.stream().map(CompletableFuture::join).collect(Collectors.toUnmodifiableList()), pGameExecutor);
    }

    private <T> CompletableFuture<TagManager.LoadResult<T>> createLoader(
        ResourceManager pResourceManager, Executor pBackgroundExecutor, RegistryAccess.RegistryEntry<T> pEntry
    ) {
        ResourceKey<? extends Registry<T>> resourcekey = pEntry.key();
        Registry<T> registry = pEntry.value();
        TagLoader<Holder<T>> tagloader = new TagLoader<>(registry::getHolder, Registries.tagsDirPath(resourcekey));
        return CompletableFuture.supplyAsync(() -> new TagManager.LoadResult<>(resourcekey, tagloader.loadAndBuild(pResourceManager)), pBackgroundExecutor);
    }

    public static record LoadResult<T>(ResourceKey<? extends Registry<T>> key, Map<ResourceLocation, Collection<Holder<T>>> tags) {
    }
}
