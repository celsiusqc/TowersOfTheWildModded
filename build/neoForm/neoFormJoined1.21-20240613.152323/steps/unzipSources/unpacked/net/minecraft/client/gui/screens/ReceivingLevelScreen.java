package net.minecraft.client.gui.screens;

import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ReceivingLevelScreen extends Screen {
    private static final Component DOWNLOADING_TERRAIN_TEXT = Component.translatable("multiplayer.downloadingTerrain");
    private static final long CHUNK_LOADING_START_WAIT_LIMIT_MS = 30000L;
    private final long createdAt;
    private final BooleanSupplier levelReceived;
    private final ReceivingLevelScreen.Reason reason;
    @Nullable
    private TextureAtlasSprite cachedNetherPortalSprite;

    public ReceivingLevelScreen(BooleanSupplier pLevelReceived, ReceivingLevelScreen.Reason pReason) {
        super(GameNarrator.NO_TITLE);
        this.levelReceived = pLevelReceived;
        this.reason = pReason;
        this.createdAt = System.currentTimeMillis();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected boolean shouldNarrateNavigation() {
        return false;
    }

    /**
     * Renders the graphical user interface (GUI) element.
     *
     * @param pGuiGraphics the GuiGraphics object used for rendering.
     * @param pMouseX      the x-coordinate of the mouse cursor.
     * @param pMouseY      the y-coordinate of the mouse cursor.
     * @param pPartialTick the partial tick time.
     */
    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        pGuiGraphics.drawCenteredString(this.font, DOWNLOADING_TERRAIN_TEXT, this.width / 2, this.height / 2 - 50, 16777215);
    }

    @Override
    public void renderBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        switch (this.reason) {
            case NETHER_PORTAL:
                pGuiGraphics.blit(0, 0, -90, pGuiGraphics.guiWidth(), pGuiGraphics.guiHeight(), this.getNetherPortalSprite());
                break;
            case END_PORTAL:
                pGuiGraphics.fillRenderType(RenderType.endPortal(), 0, 0, this.width, this.height, 0);
                break;
            case OTHER:
                this.renderPanorama(pGuiGraphics, pPartialTick);
                this.renderBlurredBackground(pPartialTick);
                this.renderMenuBackground(pGuiGraphics);
        }
    }

    private TextureAtlasSprite getNetherPortalSprite() {
        if (this.cachedNetherPortalSprite != null) {
            return this.cachedNetherPortalSprite;
        } else {
            this.cachedNetherPortalSprite = this.minecraft.getBlockRenderer().getBlockModelShaper().getParticleIcon(Blocks.NETHER_PORTAL.defaultBlockState());
            return this.cachedNetherPortalSprite;
        }
    }

    @Override
    public void tick() {
        if (this.levelReceived.getAsBoolean() || System.currentTimeMillis() > this.createdAt + 30000L) {
            this.onClose();
        }
    }

    @Override
    public void onClose() {
        this.minecraft.getNarrator().sayNow(Component.translatable("narrator.ready_to_play"));
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Reason {
        NETHER_PORTAL,
        END_PORTAL,
        OTHER;
    }
}
