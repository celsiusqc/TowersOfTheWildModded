package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.CrafterSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CrafterScreen extends AbstractContainerScreen<CrafterMenu> {
    private static final ResourceLocation DISABLED_SLOT_LOCATION_SPRITE = ResourceLocation.withDefaultNamespace("container/crafter/disabled_slot");
    private static final ResourceLocation POWERED_REDSTONE_LOCATION_SPRITE = ResourceLocation.withDefaultNamespace("container/crafter/powered_redstone");
    private static final ResourceLocation UNPOWERED_REDSTONE_LOCATION_SPRITE = ResourceLocation.withDefaultNamespace("container/crafter/unpowered_redstone");
    private static final ResourceLocation CONTAINER_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/crafter.png");
    private static final Component DISABLED_SLOT_TOOLTIP = Component.translatable("gui.togglable_slot");
    private final Player player;

    public CrafterScreen(CrafterMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.player = pPlayerInventory.player;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    /**
     * Called when the mouse is clicked over a slot or outside the gui.
     */
    @Override
    protected void slotClicked(Slot pSlot, int pSlotId, int pMouseButton, ClickType pType) {
        if (pSlot instanceof CrafterSlot && !pSlot.hasItem() && !this.player.isSpectator()) {
            switch (pType) {
                case PICKUP:
                    if (this.menu.isSlotDisabled(pSlotId)) {
                        this.enableSlot(pSlotId);
                    } else if (this.menu.getCarried().isEmpty()) {
                        this.disableSlot(pSlotId);
                    }
                    break;
                case SWAP:
                    ItemStack itemstack = this.player.getInventory().getItem(pMouseButton);
                    if (this.menu.isSlotDisabled(pSlotId) && !itemstack.isEmpty()) {
                        this.enableSlot(pSlotId);
                    }
            }
        }

        super.slotClicked(pSlot, pSlotId, pMouseButton, pType);
    }

    private void enableSlot(int pSlot) {
        this.updateSlotState(pSlot, true);
    }

    private void disableSlot(int pSlot) {
        this.updateSlotState(pSlot, false);
    }

    private void updateSlotState(int pSlot, boolean pState) {
        this.menu.setSlotState(pSlot, pState);
        super.handleSlotStateChanged(pSlot, this.menu.containerId, pState);
        float f = pState ? 1.0F : 0.75F;
        this.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.4F, f);
    }

    @Override
    public void renderSlot(GuiGraphics pGuiGraphics, Slot pSlot) {
        if (pSlot instanceof CrafterSlot crafterslot && this.menu.isSlotDisabled(pSlot.index)) {
            this.renderDisabledSlot(pGuiGraphics, crafterslot);
            return;
        }

        super.renderSlot(pGuiGraphics, pSlot);
    }

    private void renderDisabledSlot(GuiGraphics pGuiGraphics, CrafterSlot pSlot) {
        pGuiGraphics.blitSprite(DISABLED_SLOT_LOCATION_SPRITE, pSlot.x - 1, pSlot.y - 1, 18, 18);
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
        this.renderRedstone(pGuiGraphics);
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
        if (this.hoveredSlot instanceof CrafterSlot
            && !this.menu.isSlotDisabled(this.hoveredSlot.index)
            && this.menu.getCarried().isEmpty()
            && !this.hoveredSlot.hasItem()
            && !this.player.isSpectator()) {
            pGuiGraphics.renderTooltip(this.font, DISABLED_SLOT_TOOLTIP, pMouseX, pMouseY);
        }
    }

    private void renderRedstone(GuiGraphics pGuiGraphics) {
        int i = this.width / 2 + 9;
        int j = this.height / 2 - 48;
        ResourceLocation resourcelocation;
        if (this.menu.isPowered()) {
            resourcelocation = POWERED_REDSTONE_LOCATION_SPRITE;
        } else {
            resourcelocation = UNPOWERED_REDSTONE_LOCATION_SPRITE;
        }

        pGuiGraphics.blitSprite(resourcelocation, i, j, 16, 16);
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        pGuiGraphics.blit(CONTAINER_LOCATION, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }
}
