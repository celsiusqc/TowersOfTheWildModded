package net.minecraft.client.resources;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class TextureAtlasHolder implements PreparableReloadListener, AutoCloseable {
    protected final TextureAtlas textureAtlas;
    private final ResourceLocation atlasInfoLocation;
    private final Set<MetadataSectionSerializer<?>> metadataSections;

    public TextureAtlasHolder(TextureManager pTextureManager, ResourceLocation pTextureAtlasLocation, ResourceLocation pAtlasInfoLocation) {
        this(pTextureManager, pTextureAtlasLocation, pAtlasInfoLocation, SpriteLoader.DEFAULT_METADATA_SECTIONS);
    }

    public TextureAtlasHolder(TextureManager pTextureManager, ResourceLocation pTextureAtlasLocation, ResourceLocation pAtlasInfoLocation, Set<MetadataSectionSerializer<?>> pMetadataSections) {
        this.atlasInfoLocation = pAtlasInfoLocation;
        this.textureAtlas = new TextureAtlas(pTextureAtlasLocation);
        pTextureManager.register(this.textureAtlas.location(), this.textureAtlas);
        this.metadataSections = pMetadataSections;
    }

    /**
     * Gets a sprite associated with the passed resource location.
     */
    protected TextureAtlasSprite getSprite(ResourceLocation pLocation) {
        return this.textureAtlas.getSprite(pLocation);
    }

    @Override
    public final CompletableFuture<Void> reload(
        PreparableReloadListener.PreparationBarrier pPreparationBarrier,
        ResourceManager pResourceManager,
        ProfilerFiller pPreparationsProfiler,
        ProfilerFiller pReloadProfiler,
        Executor pBackgroundExecutor,
        Executor pGameExecutor
    ) {
        return SpriteLoader.create(this.textureAtlas)
            .loadAndStitch(pResourceManager, this.atlasInfoLocation, 0, pBackgroundExecutor, this.metadataSections)
            .thenCompose(SpriteLoader.Preparations::waitForUpload)
            .thenCompose(pPreparationBarrier::wait)
            .thenAcceptAsync(p_249246_ -> this.apply(p_249246_, pReloadProfiler), pGameExecutor);
    }

    private void apply(SpriteLoader.Preparations pPreparations, ProfilerFiller pProfiler) {
        pProfiler.startTick();
        pProfiler.push("upload");
        this.textureAtlas.upload(pPreparations);
        pProfiler.pop();
        pProfiler.endTick();
    }

    @Override
    public void close() {
        this.textureAtlas.clearTextureData();
    }
}
