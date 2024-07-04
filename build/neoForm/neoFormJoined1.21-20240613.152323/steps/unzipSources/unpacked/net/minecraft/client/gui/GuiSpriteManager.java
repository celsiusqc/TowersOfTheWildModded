package net.minecraft.client.gui;

import java.util.Set;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.TextureAtlasHolder;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.gui.GuiMetadataSection;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiSpriteManager extends TextureAtlasHolder {
    private static final Set<MetadataSectionSerializer<?>> METADATA_SECTIONS = Set.of(AnimationMetadataSection.SERIALIZER, GuiMetadataSection.TYPE);

    public GuiSpriteManager(TextureManager pTextureManager) {
        super(pTextureManager, ResourceLocation.withDefaultNamespace("textures/atlas/gui.png"), ResourceLocation.withDefaultNamespace("gui"), METADATA_SECTIONS);
    }

    /**
     * Gets a sprite associated with the passed resource location.
     */
    @Override
    public TextureAtlasSprite getSprite(ResourceLocation pLocation) {
        return super.getSprite(pLocation);
    }

    public GuiSpriteScaling getSpriteScaling(TextureAtlasSprite pSprite) {
        return this.getMetadata(pSprite).scaling();
    }

    private GuiMetadataSection getMetadata(TextureAtlasSprite pSprite) {
        return pSprite.contents().metadata().getSection(GuiMetadataSection.TYPE).orElse(GuiMetadataSection.DEFAULT);
    }
}
