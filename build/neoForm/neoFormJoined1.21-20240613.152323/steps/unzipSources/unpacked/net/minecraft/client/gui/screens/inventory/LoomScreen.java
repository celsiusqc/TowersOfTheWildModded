package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LoomScreen extends AbstractContainerScreen<LoomMenu> {
    private static final ResourceLocation BANNER_SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/loom/banner_slot");
    private static final ResourceLocation DYE_SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/loom/dye_slot");
    private static final ResourceLocation PATTERN_SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/loom/pattern_slot");
    private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace("container/loom/scroller");
    private static final ResourceLocation SCROLLER_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("container/loom/scroller_disabled");
    private static final ResourceLocation PATTERN_SELECTED_SPRITE = ResourceLocation.withDefaultNamespace("container/loom/pattern_selected");
    private static final ResourceLocation PATTERN_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("container/loom/pattern_highlighted");
    private static final ResourceLocation PATTERN_SPRITE = ResourceLocation.withDefaultNamespace("container/loom/pattern");
    private static final ResourceLocation ERROR_SPRITE = ResourceLocation.withDefaultNamespace("container/loom/error");
    private static final ResourceLocation BG_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/loom.png");
    private static final int PATTERN_COLUMNS = 4;
    private static final int PATTERN_ROWS = 4;
    private static final int SCROLLER_WIDTH = 12;
    private static final int SCROLLER_HEIGHT = 15;
    private static final int PATTERN_IMAGE_SIZE = 14;
    private static final int SCROLLER_FULL_HEIGHT = 56;
    private static final int PATTERNS_X = 60;
    private static final int PATTERNS_Y = 13;
    private ModelPart flag;
    @Nullable
    private BannerPatternLayers resultBannerPatterns;
    private ItemStack bannerStack = ItemStack.EMPTY;
    private ItemStack dyeStack = ItemStack.EMPTY;
    private ItemStack patternStack = ItemStack.EMPTY;
    private boolean displayPatterns;
    private boolean hasMaxPatterns;
    private float scrollOffs;
    private boolean scrolling;
    private int startRow;

    public LoomScreen(LoomMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        pMenu.registerUpdateListener(this::containerChanged);
        this.titleLabelY -= 2;
    }

    @Override
    protected void init() {
        super.init();
        this.flag = this.minecraft.getEntityModels().bakeLayer(ModelLayers.BANNER).getChild("flag");
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
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    private int totalRowCount() {
        return Mth.positiveCeilDiv(this.menu.getSelectablePatterns().size(), 4);
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        int i = this.leftPos;
        int j = this.topPos;
        pGuiGraphics.blit(BG_LOCATION, i, j, 0, 0, this.imageWidth, this.imageHeight);
        Slot slot = this.menu.getBannerSlot();
        Slot slot1 = this.menu.getDyeSlot();
        Slot slot2 = this.menu.getPatternSlot();
        Slot slot3 = this.menu.getResultSlot();
        if (!slot.hasItem()) {
            pGuiGraphics.blitSprite(BANNER_SLOT_SPRITE, i + slot.x, j + slot.y, 16, 16);
        }

        if (!slot1.hasItem()) {
            pGuiGraphics.blitSprite(DYE_SLOT_SPRITE, i + slot1.x, j + slot1.y, 16, 16);
        }

        if (!slot2.hasItem()) {
            pGuiGraphics.blitSprite(PATTERN_SLOT_SPRITE, i + slot2.x, j + slot2.y, 16, 16);
        }

        int k = (int)(41.0F * this.scrollOffs);
        ResourceLocation resourcelocation = this.displayPatterns ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE;
        pGuiGraphics.blitSprite(resourcelocation, i + 119, j + 13 + k, 12, 15);
        Lighting.setupForFlatItems();
        if (this.resultBannerPatterns != null && !this.hasMaxPatterns) {
            pGuiGraphics.pose().pushPose();
            pGuiGraphics.pose().translate((float)(i + 139), (float)(j + 52), 0.0F);
            pGuiGraphics.pose().scale(24.0F, 24.0F, 1.0F);
            pGuiGraphics.pose().translate(0.5F, -0.5F, 0.5F);
            float f = 0.6666667F;
            pGuiGraphics.pose().scale(0.6666667F, 0.6666667F, -0.6666667F);
            this.flag.xRot = 0.0F;
            this.flag.y = -32.0F;
            DyeColor dyecolor = ((BannerItem)slot3.getItem().getItem()).getColor();
            BannerRenderer.renderPatterns(
                pGuiGraphics.pose(),
                pGuiGraphics.bufferSource(),
                15728880,
                OverlayTexture.NO_OVERLAY,
                this.flag,
                ModelBakery.BANNER_BASE,
                true,
                dyecolor,
                this.resultBannerPatterns
            );
            pGuiGraphics.pose().popPose();
            pGuiGraphics.flush();
        } else if (this.hasMaxPatterns) {
            pGuiGraphics.blitSprite(ERROR_SPRITE, i + slot3.x - 5, j + slot3.y - 5, 26, 26);
        }

        if (this.displayPatterns) {
            int j2 = i + 60;
            int k2 = j + 13;
            List<Holder<BannerPattern>> list = this.menu.getSelectablePatterns();

            label64:
            for (int l = 0; l < 4; l++) {
                for (int i1 = 0; i1 < 4; i1++) {
                    int j1 = l + this.startRow;
                    int k1 = j1 * 4 + i1;
                    if (k1 >= list.size()) {
                        break label64;
                    }

                    int l1 = j2 + i1 * 14;
                    int i2 = k2 + l * 14;
                    boolean flag = pMouseX >= l1 && pMouseY >= i2 && pMouseX < l1 + 14 && pMouseY < i2 + 14;
                    ResourceLocation resourcelocation1;
                    if (k1 == this.menu.getSelectedBannerPatternIndex()) {
                        resourcelocation1 = PATTERN_SELECTED_SPRITE;
                    } else if (flag) {
                        resourcelocation1 = PATTERN_HIGHLIGHTED_SPRITE;
                    } else {
                        resourcelocation1 = PATTERN_SPRITE;
                    }

                    pGuiGraphics.blitSprite(resourcelocation1, l1, i2, 14, 14);
                    this.renderPattern(pGuiGraphics, list.get(k1), l1, i2);
                }
            }
        }

        Lighting.setupFor3DItems();
    }

    private void renderPattern(GuiGraphics pGuiGraphics, Holder<BannerPattern> pPatern, int pX, int pY) {
        PoseStack posestack = new PoseStack();
        posestack.pushPose();
        posestack.translate((float)pX + 0.5F, (float)(pY + 16), 0.0F);
        posestack.scale(6.0F, -6.0F, 1.0F);
        posestack.translate(0.5F, 0.5F, 0.0F);
        posestack.translate(0.5F, 0.5F, 0.5F);
        float f = 0.6666667F;
        posestack.scale(0.6666667F, -0.6666667F, -0.6666667F);
        this.flag.xRot = 0.0F;
        this.flag.y = -32.0F;
        BannerPatternLayers bannerpatternlayers = new BannerPatternLayers.Builder().add(pPatern, DyeColor.WHITE).build();
        BannerRenderer.renderPatterns(
            posestack,
            pGuiGraphics.bufferSource(),
            15728880,
            OverlayTexture.NO_OVERLAY,
            this.flag,
            ModelBakery.BANNER_BASE,
            true,
            DyeColor.GRAY,
            bannerpatternlayers
        );
        posestack.popPose();
        pGuiGraphics.flush();
    }

    /**
     * Called when a mouse button is clicked within the GUI element.
     * <p>
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     *
     * @param pMouseX the X coordinate of the mouse.
     * @param pMouseY the Y coordinate of the mouse.
     * @param pButton the button that was clicked.
     */
    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        this.scrolling = false;
        if (this.displayPatterns) {
            int i = this.leftPos + 60;
            int j = this.topPos + 13;

            for (int k = 0; k < 4; k++) {
                for (int l = 0; l < 4; l++) {
                    double d0 = pMouseX - (double)(i + l * 14);
                    double d1 = pMouseY - (double)(j + k * 14);
                    int i1 = k + this.startRow;
                    int j1 = i1 * 4 + l;
                    if (d0 >= 0.0 && d1 >= 0.0 && d0 < 14.0 && d1 < 14.0 && this.menu.clickMenuButton(this.minecraft.player, j1)) {
                        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_LOOM_SELECT_PATTERN, 1.0F));
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, j1);
                        return true;
                    }
                }
            }

            i = this.leftPos + 119;
            j = this.topPos + 9;
            if (pMouseX >= (double)i && pMouseX < (double)(i + 12) && pMouseY >= (double)j && pMouseY < (double)(j + 56)) {
                this.scrolling = true;
            }
        }

        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    /**
     * Called when the mouse is dragged within the GUI element.
     * <p>
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     *
     * @param pMouseX the X coordinate of the mouse.
     * @param pMouseY the Y coordinate of the mouse.
     * @param pButton the button that is being dragged.
     * @param pDragX  the X distance of the drag.
     * @param pDragY  the Y distance of the drag.
     */
    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        int i = this.totalRowCount() - 4;
        if (this.scrolling && this.displayPatterns && i > 0) {
            int j = this.topPos + 13;
            int k = j + 56;
            this.scrollOffs = ((float)pMouseY - (float)j - 7.5F) / ((float)(k - j) - 15.0F);
            this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
            this.startRow = Math.max((int)((double)(this.scrollOffs * (float)i) + 0.5), 0);
            return true;
        } else {
            return super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
        }
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
        int i = this.totalRowCount() - 4;
        if (this.displayPatterns && i > 0) {
            float f = (float)pScrollY / (float)i;
            this.scrollOffs = Mth.clamp(this.scrollOffs - f, 0.0F, 1.0F);
            this.startRow = Math.max((int)(this.scrollOffs * (float)i + 0.5F), 0);
        }

        return true;
    }

    @Override
    protected boolean hasClickedOutside(double pMouseX, double pMouseY, int pGuiLeft, int pGuiTop, int pMouseButton) {
        return pMouseX < (double)pGuiLeft
            || pMouseY < (double)pGuiTop
            || pMouseX >= (double)(pGuiLeft + this.imageWidth)
            || pMouseY >= (double)(pGuiTop + this.imageHeight);
    }

    private void containerChanged() {
        ItemStack itemstack = this.menu.getResultSlot().getItem();
        if (itemstack.isEmpty()) {
            this.resultBannerPatterns = null;
        } else {
            this.resultBannerPatterns = itemstack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
        }

        ItemStack itemstack1 = this.menu.getBannerSlot().getItem();
        ItemStack itemstack2 = this.menu.getDyeSlot().getItem();
        ItemStack itemstack3 = this.menu.getPatternSlot().getItem();
        BannerPatternLayers bannerpatternlayers = itemstack1.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
        this.hasMaxPatterns = bannerpatternlayers.layers().size() >= 6;
        if (this.hasMaxPatterns) {
            this.resultBannerPatterns = null;
        }

        if (!ItemStack.matches(itemstack1, this.bannerStack)
            || !ItemStack.matches(itemstack2, this.dyeStack)
            || !ItemStack.matches(itemstack3, this.patternStack)) {
            this.displayPatterns = !itemstack1.isEmpty() && !itemstack2.isEmpty() && !this.hasMaxPatterns && !this.menu.getSelectablePatterns().isEmpty();
        }

        if (this.startRow >= this.totalRowCount()) {
            this.startRow = 0;
            this.scrollOffs = 0.0F;
        }

        this.bannerStack = itemstack1.copy();
        this.dyeStack = itemstack2.copy();
        this.patternStack = itemstack3.copy();
    }
}
