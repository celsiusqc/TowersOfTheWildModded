package net.minecraft.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PanoramaRenderer {
    public static final ResourceLocation PANORAMA_OVERLAY = ResourceLocation.withDefaultNamespace("textures/gui/title/background/panorama_overlay.png");
    private final Minecraft minecraft;
    private final CubeMap cubeMap;
    private float spin;
    private float bob;

    public PanoramaRenderer(CubeMap pCubeMap) {
        this.cubeMap = pCubeMap;
        this.minecraft = Minecraft.getInstance();
    }

    public void render(GuiGraphics pGuiGraphics, int pWidth, int pHeight, float pFade, float pPartialTick) {
        float f = (float)((double)pPartialTick * this.minecraft.options.panoramaSpeed().get());
        this.spin = wrap(this.spin + f * 0.1F, 360.0F);
        this.bob = wrap(this.bob + f * 0.001F, (float) (Math.PI * 2));
        this.cubeMap.render(this.minecraft, 10.0F, -this.spin, pFade);
        RenderSystem.enableBlend();
        pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, pFade);
        pGuiGraphics.blit(PANORAMA_OVERLAY, 0, 0, pWidth, pHeight, 0.0F, 0.0F, 16, 128, 16, 128);
        pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static float wrap(float pValue, float pMax) {
        return pValue > pMax ? pValue - pMax : pValue;
    }
}
