package net.minecraft.client.gui.font;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record GlyphRenderTypes(RenderType normal, RenderType seeThrough, RenderType polygonOffset) {
    public static GlyphRenderTypes createForIntensityTexture(ResourceLocation pId) {
        return new GlyphRenderTypes(
            RenderType.textIntensity(pId), RenderType.textIntensitySeeThrough(pId), RenderType.textIntensityPolygonOffset(pId)
        );
    }

    public static GlyphRenderTypes createForColorTexture(ResourceLocation pId) {
        return new GlyphRenderTypes(RenderType.text(pId), RenderType.textSeeThrough(pId), RenderType.textPolygonOffset(pId));
    }

    public RenderType select(Font.DisplayMode pDisplayMode) {
        return switch (pDisplayMode) {
            case NORMAL -> this.normal;
            case SEE_THROUGH -> this.seeThrough;
            case POLYGON_OFFSET -> this.polygonOffset;
        };
    }
}
