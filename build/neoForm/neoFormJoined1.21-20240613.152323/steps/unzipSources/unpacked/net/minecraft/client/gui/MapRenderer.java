package net.minecraft.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.MapDecorationTextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class MapRenderer implements AutoCloseable {
    private static final int WIDTH = 128;
    private static final int HEIGHT = 128;
    final TextureManager textureManager;
    final MapDecorationTextureManager decorationTextures;
    private final Int2ObjectMap<MapRenderer.MapInstance> maps = new Int2ObjectOpenHashMap<>();

    public MapRenderer(TextureManager pTextureManager, MapDecorationTextureManager pDecorationTextures) {
        this.textureManager = pTextureManager;
        this.decorationTextures = pDecorationTextures;
    }

    public void update(MapId pMapId, MapItemSavedData pMapData) {
        this.getOrCreateMapInstance(pMapId, pMapData).forceUpload();
    }

    public void render(PoseStack pPoseStack, MultiBufferSource pBuffer, MapId pMapId, MapItemSavedData pMapData, boolean pActive, int pPackedLight) {
        this.getOrCreateMapInstance(pMapId, pMapData).draw(pPoseStack, pBuffer, pActive, pPackedLight);
    }

    private MapRenderer.MapInstance getOrCreateMapInstance(MapId pMapId, MapItemSavedData pMapData) {
        return this.maps.compute(pMapId.id(), (p_182563_, p_182564_) -> {
            if (p_182564_ == null) {
                return new MapRenderer.MapInstance(p_182563_, pMapData);
            } else {
                p_182564_.replaceMapData(pMapData);
                return (MapRenderer.MapInstance)p_182564_;
            }
        });
    }

    public void resetData() {
        for (MapRenderer.MapInstance maprenderer$mapinstance : this.maps.values()) {
            maprenderer$mapinstance.close();
        }

        this.maps.clear();
    }

    @Override
    public void close() {
        this.resetData();
    }

    @OnlyIn(Dist.CLIENT)
    class MapInstance implements AutoCloseable {
        private MapItemSavedData data;
        private final DynamicTexture texture;
        private final RenderType renderType;
        private boolean requiresUpload = true;

        MapInstance(int pId, MapItemSavedData pData) {
            this.data = pData;
            this.texture = new DynamicTexture(128, 128, true);
            ResourceLocation resourcelocation = MapRenderer.this.textureManager.register("map/" + pId, this.texture);
            this.renderType = RenderType.text(resourcelocation);
        }

        void replaceMapData(MapItemSavedData pData) {
            boolean flag = this.data != pData;
            this.data = pData;
            this.requiresUpload |= flag;
        }

        public void forceUpload() {
            this.requiresUpload = true;
        }

        private void updateTexture() {
            for (int i = 0; i < 128; i++) {
                for (int j = 0; j < 128; j++) {
                    int k = j + i * 128;
                    this.texture.getPixels().setPixelRGBA(j, i, MapColor.getColorFromPackedId(this.data.colors[k]));
                }
            }

            this.texture.upload();
        }

        void draw(PoseStack pPoseStack, MultiBufferSource pBufferSource, boolean pActive, int pPackedLight) {
            if (this.requiresUpload) {
                this.updateTexture();
                this.requiresUpload = false;
            }

            int i = 0;
            int j = 0;
            float f = 0.0F;
            Matrix4f matrix4f = pPoseStack.last().pose();
            VertexConsumer vertexconsumer = pBufferSource.getBuffer(this.renderType);
            vertexconsumer.addVertex(matrix4f, 0.0F, 128.0F, -0.01F).setColor(-1).setUv(0.0F, 1.0F).setLight(pPackedLight);
            vertexconsumer.addVertex(matrix4f, 128.0F, 128.0F, -0.01F).setColor(-1).setUv(1.0F, 1.0F).setLight(pPackedLight);
            vertexconsumer.addVertex(matrix4f, 128.0F, 0.0F, -0.01F).setColor(-1).setUv(1.0F, 0.0F).setLight(pPackedLight);
            vertexconsumer.addVertex(matrix4f, 0.0F, 0.0F, -0.01F).setColor(-1).setUv(0.0F, 0.0F).setLight(pPackedLight);
            int k = 0;

            for (MapDecoration mapdecoration : this.data.getDecorations()) {
                if (!pActive || mapdecoration.renderOnFrame()) {
                    pPoseStack.pushPose();
                    pPoseStack.translate(0.0F + (float)mapdecoration.x() / 2.0F + 64.0F, 0.0F + (float)mapdecoration.y() / 2.0F + 64.0F, -0.02F);
                    pPoseStack.mulPose(Axis.ZP.rotationDegrees((float)(mapdecoration.rot() * 360) / 16.0F));
                    pPoseStack.scale(4.0F, 4.0F, 3.0F);
                    pPoseStack.translate(-0.125F, 0.125F, 0.0F);
                    Matrix4f matrix4f1 = pPoseStack.last().pose();
                    float f1 = -0.001F;
                    TextureAtlasSprite textureatlassprite = MapRenderer.this.decorationTextures.get(mapdecoration);
                    float f2 = textureatlassprite.getU0();
                    float f3 = textureatlassprite.getV0();
                    float f4 = textureatlassprite.getU1();
                    float f5 = textureatlassprite.getV1();
                    VertexConsumer vertexconsumer1 = pBufferSource.getBuffer(RenderType.text(textureatlassprite.atlasLocation()));
                    vertexconsumer1.addVertex(matrix4f1, -1.0F, 1.0F, (float)k * -0.001F).setColor(-1).setUv(f2, f3).setLight(pPackedLight);
                    vertexconsumer1.addVertex(matrix4f1, 1.0F, 1.0F, (float)k * -0.001F).setColor(-1).setUv(f4, f3).setLight(pPackedLight);
                    vertexconsumer1.addVertex(matrix4f1, 1.0F, -1.0F, (float)k * -0.001F).setColor(-1).setUv(f4, f5).setLight(pPackedLight);
                    vertexconsumer1.addVertex(matrix4f1, -1.0F, -1.0F, (float)k * -0.001F).setColor(-1).setUv(f2, f5).setLight(pPackedLight);
                    pPoseStack.popPose();
                    if (mapdecoration.name().isPresent()) {
                        Font font = Minecraft.getInstance().font;
                        Component component = mapdecoration.name().get();
                        float f6 = (float)font.width(component);
                        float f7 = Mth.clamp(25.0F / f6, 0.0F, 6.0F / 9.0F);
                        pPoseStack.pushPose();
                        pPoseStack.translate(
                            0.0F + (float)mapdecoration.x() / 2.0F + 64.0F - f6 * f7 / 2.0F, 0.0F + (float)mapdecoration.y() / 2.0F + 64.0F + 4.0F, -0.025F
                        );
                        pPoseStack.scale(f7, f7, 1.0F);
                        pPoseStack.translate(0.0F, 0.0F, -0.1F);
                        font.drawInBatch(
                            component, 0.0F, 0.0F, -1, false, pPoseStack.last().pose(), pBufferSource, Font.DisplayMode.NORMAL, Integer.MIN_VALUE, pPackedLight
                        );
                        pPoseStack.popPose();
                    }

                    k++;
                }
            }
        }

        @Override
        public void close() {
            this.texture.close();
        }
    }
}
