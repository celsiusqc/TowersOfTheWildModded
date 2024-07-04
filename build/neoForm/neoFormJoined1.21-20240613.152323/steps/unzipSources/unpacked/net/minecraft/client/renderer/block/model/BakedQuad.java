package net.minecraft.client.renderer.block.model;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BakedQuad {
    /**
     * Joined 4 vertex records, each stores packed data according to the VertexFormat of the quad. Vanilla minecraft uses DefaultVertexFormats.BLOCK, Forge uses (usually) ITEM, use BakedQuad.getFormat() to get the correct format.
     */
    protected final int[] vertices;
    protected final int tintIndex;
    protected final Direction direction;
    protected final TextureAtlasSprite sprite;
    private final boolean shade;
    private final boolean hasAmbientOcclusion;

    public BakedQuad(int[] pVertices, int pTintIndex, Direction pDirection, TextureAtlasSprite pSprite, boolean pShade) {
        this(pVertices, pTintIndex, pDirection, pSprite, pShade, true);
    }

    public BakedQuad(int[] pVertices, int pTintIndex, Direction pDirection, TextureAtlasSprite pSprite, boolean pShade, boolean hasAmbientOcclusion) {
        this.vertices = pVertices;
        this.tintIndex = pTintIndex;
        this.direction = pDirection;
        this.sprite = pSprite;
        this.shade = pShade;
        this.hasAmbientOcclusion = hasAmbientOcclusion;
    }

    public TextureAtlasSprite getSprite() {
        return this.sprite;
    }

    public int[] getVertices() {
        return this.vertices;
    }

    public boolean isTinted() {
        return this.tintIndex != -1;
    }

    public int getTintIndex() {
        return this.tintIndex;
    }

    public Direction getDirection() {
        return this.direction;
    }

    public boolean isShade() {
        return this.shade;
    }

    /**
     * {@return false to force-disable AO for this quad or true to use the behavior dictated by
     * {@link net.neoforged.neoforge.client.extensions.IBakedModelExtension#useAmbientOcclusion(net.minecraft.world.level.block.state.BlockState, net.neoforged.neoforge.client.model.data.ModelData, net.minecraft.client.renderer.RenderType)}}
     */
    public boolean hasAmbientOcclusion() {
        return this.hasAmbientOcclusion;
    }
}
