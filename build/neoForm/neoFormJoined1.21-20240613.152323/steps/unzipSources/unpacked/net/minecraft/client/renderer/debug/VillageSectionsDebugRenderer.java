package net.minecraft.client.renderer.debug;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Set;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VillageSectionsDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final int MAX_RENDER_DIST_FOR_VILLAGE_SECTIONS = 60;
    private final Set<SectionPos> villageSections = Sets.newHashSet();

    VillageSectionsDebugRenderer() {
    }

    @Override
    public void clear() {
        this.villageSections.clear();
    }

    public void setVillageSection(SectionPos pPos) {
        this.villageSections.add(pPos);
    }

    public void setNotVillageSection(SectionPos pPos) {
        this.villageSections.remove(pPos);
    }

    @Override
    public void render(PoseStack pPoseStack, MultiBufferSource pBuffer, double pCamX, double pCamY, double pCamZ) {
        BlockPos blockpos = BlockPos.containing(pCamX, pCamY, pCamZ);
        this.villageSections.forEach(p_269747_ -> {
            if (blockpos.closerThan(p_269747_.center(), 60.0)) {
                highlightVillageSection(pPoseStack, pBuffer, p_269747_);
            }
        });
    }

    private static void highlightVillageSection(PoseStack pPoseStack, MultiBufferSource pBuffer, SectionPos pPos) {
        DebugRenderer.renderFilledUnitCube(pPoseStack, pBuffer, pPos.center(), 0.2F, 1.0F, 0.2F, 0.15F);
    }
}
