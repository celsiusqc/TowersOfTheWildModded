package net.minecraft.client.renderer.debug;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.StructuresDebugPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StructureRenderer implements DebugRenderer.SimpleDebugRenderer {
    private final Minecraft minecraft;
    private final Map<ResourceKey<Level>, Map<String, BoundingBox>> postMainBoxes = Maps.newIdentityHashMap();
    private final Map<ResourceKey<Level>, Map<String, StructuresDebugPayload.PieceInfo>> postPieces = Maps.newIdentityHashMap();
    private static final int MAX_RENDER_DIST = 500;

    public StructureRenderer(Minecraft pMinecraft) {
        this.minecraft = pMinecraft;
    }

    @Override
    public void render(PoseStack pPoseStack, MultiBufferSource pBuffer, double pCamX, double pCamY, double pCamZ) {
        Camera camera = this.minecraft.gameRenderer.getMainCamera();
        ResourceKey<Level> resourcekey = this.minecraft.level.dimension();
        BlockPos blockpos = BlockPos.containing(camera.getPosition().x, 0.0, camera.getPosition().z);
        VertexConsumer vertexconsumer = pBuffer.getBuffer(RenderType.lines());
        if (this.postMainBoxes.containsKey(resourcekey)) {
            for (BoundingBox boundingbox : this.postMainBoxes.get(resourcekey).values()) {
                if (blockpos.closerThan(boundingbox.getCenter(), 500.0)) {
                    LevelRenderer.renderLineBox(
                        pPoseStack,
                        vertexconsumer,
                        (double)boundingbox.minX() - pCamX,
                        (double)boundingbox.minY() - pCamY,
                        (double)boundingbox.minZ() - pCamZ,
                        (double)(boundingbox.maxX() + 1) - pCamX,
                        (double)(boundingbox.maxY() + 1) - pCamY,
                        (double)(boundingbox.maxZ() + 1) - pCamZ,
                        1.0F,
                        1.0F,
                        1.0F,
                        1.0F,
                        1.0F,
                        1.0F,
                        1.0F
                    );
                }
            }
        }

        Map<String, StructuresDebugPayload.PieceInfo> map = this.postPieces.get(resourcekey);
        if (map != null) {
            for (StructuresDebugPayload.PieceInfo structuresdebugpayload$pieceinfo : map.values()) {
                BoundingBox boundingbox1 = structuresdebugpayload$pieceinfo.boundingBox();
                if (blockpos.closerThan(boundingbox1.getCenter(), 500.0)) {
                    if (structuresdebugpayload$pieceinfo.isStart()) {
                        LevelRenderer.renderLineBox(
                            pPoseStack,
                            vertexconsumer,
                            (double)boundingbox1.minX() - pCamX,
                            (double)boundingbox1.minY() - pCamY,
                            (double)boundingbox1.minZ() - pCamZ,
                            (double)(boundingbox1.maxX() + 1) - pCamX,
                            (double)(boundingbox1.maxY() + 1) - pCamY,
                            (double)(boundingbox1.maxZ() + 1) - pCamZ,
                            0.0F,
                            1.0F,
                            0.0F,
                            1.0F,
                            0.0F,
                            1.0F,
                            0.0F
                        );
                    } else {
                        LevelRenderer.renderLineBox(
                            pPoseStack,
                            vertexconsumer,
                            (double)boundingbox1.minX() - pCamX,
                            (double)boundingbox1.minY() - pCamY,
                            (double)boundingbox1.minZ() - pCamZ,
                            (double)(boundingbox1.maxX() + 1) - pCamX,
                            (double)(boundingbox1.maxY() + 1) - pCamY,
                            (double)(boundingbox1.maxZ() + 1) - pCamZ,
                            0.0F,
                            0.0F,
                            1.0F,
                            1.0F,
                            0.0F,
                            0.0F,
                            1.0F
                        );
                    }
                }
            }
        }
    }

    public void addBoundingBox(BoundingBox pBoundingBox, List<StructuresDebugPayload.PieceInfo> pPieces, ResourceKey<Level> pDimension) {
        this.postMainBoxes.computeIfAbsent(pDimension, p_294379_ -> new HashMap<>()).put(pBoundingBox.toString(), pBoundingBox);
        Map<String, StructuresDebugPayload.PieceInfo> map = this.postPieces.computeIfAbsent(pDimension, p_294187_ -> new HashMap<>());

        for (StructuresDebugPayload.PieceInfo structuresdebugpayload$pieceinfo : pPieces) {
            map.put(structuresdebugpayload$pieceinfo.boundingBox().toString(), structuresdebugpayload$pieceinfo);
        }
    }

    @Override
    public void clear() {
        this.postMainBoxes.clear();
        this.postPieces.clear();
    }
}
