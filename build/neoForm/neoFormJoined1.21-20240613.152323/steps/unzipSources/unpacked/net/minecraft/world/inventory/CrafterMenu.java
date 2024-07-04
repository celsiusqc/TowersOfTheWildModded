package net.minecraft.world.inventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CrafterBlock;

public class CrafterMenu extends AbstractContainerMenu implements ContainerListener {
    protected static final int SLOT_COUNT = 9;
    private static final int INV_SLOT_START = 9;
    private static final int INV_SLOT_END = 36;
    private static final int USE_ROW_SLOT_START = 36;
    private static final int USE_ROW_SLOT_END = 45;
    private final ResultContainer resultContainer = new ResultContainer();
    private final ContainerData containerData;
    private final Player player;
    private final CraftingContainer container;

    public CrafterMenu(int pContainerId, Inventory pPlayerInventory) {
        super(MenuType.CRAFTER_3x3, pContainerId);
        this.player = pPlayerInventory.player;
        this.containerData = new SimpleContainerData(10);
        this.container = new TransientCraftingContainer(this, 3, 3);
        this.addSlots(pPlayerInventory);
    }

    public CrafterMenu(int pContainerId, Inventory pPlayerInventory, CraftingContainer pContainer, ContainerData pContainerData) {
        super(MenuType.CRAFTER_3x3, pContainerId);
        this.player = pPlayerInventory.player;
        this.containerData = pContainerData;
        this.container = pContainer;
        checkContainerSize(pContainer, 9);
        pContainer.startOpen(pPlayerInventory.player);
        this.addSlots(pPlayerInventory);
        this.addSlotListener(this);
    }

    private void addSlots(Inventory pPlayerInventory) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int k = j + i * 3;
                this.addSlot(new CrafterSlot(this.container, k, 26 + j * 18, 17 + i * 18, this));
            }
        }

        for (int l = 0; l < 3; l++) {
            for (int j1 = 0; j1 < 9; j1++) {
                this.addSlot(new Slot(pPlayerInventory, j1 + l * 9 + 9, 8 + j1 * 18, 84 + l * 18));
            }
        }

        for (int i1 = 0; i1 < 9; i1++) {
            this.addSlot(new Slot(pPlayerInventory, i1, 8 + i1 * 18, 142));
        }

        this.addSlot(new NonInteractiveResultSlot(this.resultContainer, 0, 134, 35));
        this.addDataSlots(this.containerData);
        this.refreshRecipeResult();
    }

    public void setSlotState(int pSlot, boolean pEnabled) {
        CrafterSlot crafterslot = (CrafterSlot)this.getSlot(pSlot);
        this.containerData.set(crafterslot.index, pEnabled ? 0 : 1);
        this.broadcastChanges();
    }

    public boolean isSlotDisabled(int pSlot) {
        return pSlot > -1 && pSlot < 9 ? this.containerData.get(pSlot) == 1 : false;
    }

    public boolean isPowered() {
        return this.containerData.get(9) == 1;
    }

    /**
     * Handle when the stack in slot {@code index} is shift-clicked. Normally this moves the stack between the player inventory and the other inventory(s).
     */
    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (pIndex < 9) {
                if (!this.moveItemStackTo(itemstack1, 9, 45, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, 9, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(pPlayer, itemstack1);
        }

        return itemstack;
    }

    /**
     * Determines whether supplied player can use this container
     */
    @Override
    public boolean stillValid(Player pPlayer) {
        return this.container.stillValid(pPlayer);
    }

    private void refreshRecipeResult() {
        if (this.player instanceof ServerPlayer serverplayer) {
            Level level = serverplayer.level();
            CraftingInput craftinginput = this.container.asCraftInput();
            ItemStack itemstack = CrafterBlock.getPotentialResults(level, craftinginput)
                .map(p_344359_ -> p_344359_.value().assemble(craftinginput, level.registryAccess()))
                .orElse(ItemStack.EMPTY);
            this.resultContainer.setItem(0, itemstack);
        }
    }

    public Container getContainer() {
        return this.container;
    }

    /**
     * Sends the contents of an inventory slot to the client-side Container. This doesn't have to match the actual contents of that slot.
     */
    @Override
    public void slotChanged(AbstractContainerMenu pContainerToSend, int pDataSlotIndex, ItemStack pStack) {
        this.refreshRecipeResult();
    }

    @Override
    public void dataChanged(AbstractContainerMenu pContainerMenu, int pDataSlotIndex, int pValue) {
    }
}
