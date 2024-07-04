package net.minecraft.world.level.block.entity;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.state.BlockState;

public class CrafterBlockEntity extends RandomizableContainerBlockEntity implements CraftingContainer {
    public static final int CONTAINER_WIDTH = 3;
    public static final int CONTAINER_HEIGHT = 3;
    public static final int CONTAINER_SIZE = 9;
    public static final int SLOT_DISABLED = 1;
    public static final int SLOT_ENABLED = 0;
    public static final int DATA_TRIGGERED = 9;
    public static final int NUM_DATA = 10;
    private NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
    private int craftingTicksRemaining = 0;
    protected final ContainerData containerData = new ContainerData() {
        private final int[] slotStates = new int[9];
        private int triggered = 0;

        @Override
        public int get(int p_307671_) {
            return p_307671_ == 9 ? this.triggered : this.slotStates[p_307671_];
        }

        @Override
        public void set(int p_307241_, int p_307484_) {
            if (p_307241_ == 9) {
                this.triggered = p_307484_;
            } else {
                this.slotStates[p_307241_] = p_307484_;
            }
        }

        @Override
        public int getCount() {
            return 10;
        }
    };

    public CrafterBlockEntity(BlockPos pPos, BlockState pState) {
        super(BlockEntityType.CRAFTER, pPos, pState);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.crafter");
    }

    @Override
    protected AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory) {
        return new CrafterMenu(pContainerId, pInventory, this, this.containerData);
    }

    public void setSlotState(int pSlot, boolean pState) {
        if (this.slotCanBeDisabled(pSlot)) {
            this.containerData.set(pSlot, pState ? 0 : 1);
            this.setChanged();
        }
    }

    public boolean isSlotDisabled(int pSlot) {
        return pSlot >= 0 && pSlot < 9 ? this.containerData.get(pSlot) == 1 : false;
    }

    /**
     * Returns {@code true} if automation is allowed to insert the given stack (ignoring stack size) into the given slot. For guis use Slot.isItemValid
     */
    @Override
    public boolean canPlaceItem(int pSlot, ItemStack pStack) {
        if (this.containerData.get(pSlot) == 1) {
            return false;
        } else {
            ItemStack itemstack = this.items.get(pSlot);
            int i = itemstack.getCount();
            if (i >= itemstack.getMaxStackSize()) {
                return false;
            } else {
                return itemstack.isEmpty() ? true : !this.smallerStackExist(i, itemstack, pSlot);
            }
        }
    }

    private boolean smallerStackExist(int pCurrentSize, ItemStack pStack, int pSlot) {
        for (int i = pSlot + 1; i < 9; i++) {
            if (!this.isSlotDisabled(i)) {
                ItemStack itemstack = this.getItem(i);
                if (itemstack.isEmpty() || itemstack.getCount() < pCurrentSize && ItemStack.isSameItemSameComponents(itemstack, pStack)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);
        this.craftingTicksRemaining = pTag.getInt("crafting_ticks_remaining");
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(pTag)) {
            ContainerHelper.loadAllItems(pTag, this.items, pRegistries);
        }

        int[] aint = pTag.getIntArray("disabled_slots");

        for (int i = 0; i < 9; i++) {
            this.containerData.set(i, 0);
        }

        for (int j : aint) {
            if (this.slotCanBeDisabled(j)) {
                this.containerData.set(j, 1);
            }
        }

        this.containerData.set(9, pTag.getInt("triggered"));
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(pTag, pRegistries);
        pTag.putInt("crafting_ticks_remaining", this.craftingTicksRemaining);
        if (!this.trySaveLootTable(pTag)) {
            ContainerHelper.saveAllItems(pTag, this.items, pRegistries);
        }

        this.addDisabledSlots(pTag);
        this.addTriggered(pTag);
    }

    @Override
    public int getContainerSize() {
        return 9;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : this.items) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getItem(int pIndex) {
        return this.items.get(pIndex);
    }

    @Override
    public void setItem(int pIndex, ItemStack pStack) {
        if (this.isSlotDisabled(pIndex)) {
            this.setSlotState(pIndex, true);
        }

        super.setItem(pIndex, pStack);
    }

    /**
     * Don't rename this method to canInteractWith due to conflicts with Container
     */
    @Override
    public boolean stillValid(Player pPlayer) {
        return Container.stillValidBlockEntity(this, pPlayer);
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> pItems) {
        this.items = pItems;
    }

    @Override
    public int getWidth() {
        return 3;
    }

    @Override
    public int getHeight() {
        return 3;
    }

    @Override
    public void fillStackedContents(StackedContents pContents) {
        for (ItemStack itemstack : this.items) {
            pContents.accountSimpleStack(itemstack);
        }
    }

    private void addDisabledSlots(CompoundTag pTag) {
        IntList intlist = new IntArrayList();

        for (int i = 0; i < 9; i++) {
            if (this.isSlotDisabled(i)) {
                intlist.add(i);
            }
        }

        pTag.putIntArray("disabled_slots", intlist);
    }

    private void addTriggered(CompoundTag pTag) {
        pTag.putInt("triggered", this.containerData.get(9));
    }

    public void setTriggered(boolean pTriggered) {
        this.containerData.set(9, pTriggered ? 1 : 0);
    }

    @VisibleForTesting
    public boolean isTriggered() {
        return this.containerData.get(9) == 1;
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, CrafterBlockEntity pCrafter) {
        int i = pCrafter.craftingTicksRemaining - 1;
        if (i >= 0) {
            pCrafter.craftingTicksRemaining = i;
            if (i == 0) {
                pLevel.setBlock(pPos, pState.setValue(CrafterBlock.CRAFTING, Boolean.valueOf(false)), 3);
            }
        }
    }

    public void setCraftingTicksRemaining(int pCraftingTicksRemaining) {
        this.craftingTicksRemaining = pCraftingTicksRemaining;
    }

    public int getRedstoneSignal() {
        int i = 0;

        for (int j = 0; j < this.getContainerSize(); j++) {
            ItemStack itemstack = this.getItem(j);
            if (!itemstack.isEmpty() || this.isSlotDisabled(j)) {
                i++;
            }
        }

        return i;
    }

    private boolean slotCanBeDisabled(int pSlot) {
        return pSlot > -1 && pSlot < 9 && this.items.get(pSlot).isEmpty();
    }
}
