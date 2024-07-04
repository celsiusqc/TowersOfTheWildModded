package net.minecraft.client.renderer.texture.atlas;

import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface SpriteSource {
    FileToIdConverter TEXTURE_ID_CONVERTER = new FileToIdConverter("textures", ".png");

    void run(ResourceManager pResourceManager, SpriteSource.Output pOutput);

    SpriteSourceType type();

    @OnlyIn(Dist.CLIENT)
    public interface Output {
        default void add(ResourceLocation pLocation, Resource pResource) {
            this.add(pLocation, p_293684_ -> p_293684_.loadSprite(pLocation, pResource));
        }

        void add(ResourceLocation pLocation, SpriteSource.SpriteSupplier pSprite);

        void removeAll(Predicate<ResourceLocation> pPredicate);
    }

    @OnlyIn(Dist.CLIENT)
    public interface SpriteSupplier extends Function<SpriteResourceLoader, SpriteContents> {
        default void discard() {
        }
    }
}
