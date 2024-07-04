package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HorseInventoryScreen extends AbstractContainerScreen<HorseInventoryMenu> {
    private static final ResourceLocation CHEST_SLOTS_SPRITE = ResourceLocation.withDefaultNamespace("container/horse/chest_slots");
    private static final ResourceLocation SADDLE_SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/horse/saddle_slot");
    private static final ResourceLocation LLAMA_ARMOR_SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/horse/llama_armor_slot");
    private static final ResourceLocation ARMOR_SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/horse/armor_slot");
    private static final ResourceLocation HORSE_INVENTORY_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/horse.png");
    /**
     * The EntityHorse whose inventory is currently being accessed.
     */
    private final AbstractHorse horse;
    private final int inventoryColumns;
    /**
     * The mouse x-position recorded during the last rendered frame.
     */
    private float xMouse;
    /**
     * The mouse y-position recorded during the last rendered frame.
     */
    private float yMouse;

    public HorseInventoryScreen(HorseInventoryMenu pMenu, Inventory pPlayerInventory, AbstractHorse pHorse, int p_352203_) {
        super(pMenu, pPlayerInventory, pHorse.getDisplayName());
        this.horse = pHorse;
        this.inventoryColumns = p_352203_;
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        pGuiGraphics.blit(HORSE_INVENTORY_LOCATION, i, j, 0, 0, this.imageWidth, this.imageHeight);
        if (this.inventoryColumns > 0) {
            pGuiGraphics.blitSprite(CHEST_SLOTS_SPRITE, 90, 54, 0, 0, i + 79, j + 17, this.inventoryColumns * 18, 54);
        }

        if (this.horse.isSaddleable()) {
            pGuiGraphics.blitSprite(SADDLE_SLOT_SPRITE, i + 7, j + 35 - 18, 18, 18);
        }

        if (this.horse.canUseSlot(EquipmentSlot.BODY)) {
            if (this.horse instanceof Llama) {
                pGuiGraphics.blitSprite(LLAMA_ARMOR_SLOT_SPRITE, i + 7, j + 35, 18, 18);
            } else {
                pGuiGraphics.blitSprite(ARMOR_SLOT_SPRITE, i + 7, j + 35, 18, 18);
            }
        }

        InventoryScreen.renderEntityInInventoryFollowsMouse(pGuiGraphics, i + 26, j + 18, i + 78, j + 70, 17, 0.25F, this.xMouse, this.yMouse, this.horse);
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
        this.xMouse = (float)pMouseX;
        this.yMouse = (float)pMouseY;
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }
}
