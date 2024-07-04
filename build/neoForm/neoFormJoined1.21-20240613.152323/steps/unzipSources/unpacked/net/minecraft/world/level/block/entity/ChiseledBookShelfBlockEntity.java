package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import org.slf4j.Logger;

public class ChiseledBookShelfBlockEntity extends BlockEntity implements Container {
    public static final int MAX_BOOKS_IN_STORAGE = 6;
    private static final Logger LOGGER = LogUtils.getLogger();
    private final NonNullList<ItemStack> items = NonNullList.withSize(6, ItemStack.EMPTY);
    private int lastInteractedSlot = -1;

    public ChiseledBookShelfBlockEntity(BlockPos pPos, BlockState pState) {
        super(BlockEntityType.CHISELED_BOOKSHELF, pPos, pState);
    }

    private void updateState(int pSlot) {
        if (pSlot >= 0 && pSlot < 6) {
            this.lastInteractedSlot = pSlot;
            BlockState blockstate = this.getBlockState();

            for (int i = 0; i < ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.size(); i++) {
                boolean flag = !this.getItem(i).isEmpty();
                BooleanProperty booleanproperty = ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(i);
                blockstate = blockstate.setValue(booleanproperty, Boolean.valueOf(flag));
            }

            Objects.requireNonNull(this.level).setBlock(this.worldPosition, blockstate, 3);
            this.level.gameEvent(GameEvent.BLOCK_CHANGE, this.worldPosition, GameEvent.Context.of(blockstate));
        } else {
            LOGGER.error("Expected slot 0-5, got {}", pSlot);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);
        this.items.clear();
        ContainerHelper.loadAllItems(pTag, this.items, pRegistries);
        this.lastInteractedSlot = pTag.getInt("last_interacted_slot");
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(pTag, pRegistries);
        ContainerHelper.saveAllItems(pTag, this.items, true, pRegistries);
        pTag.putInt("last_interacted_slot", this.lastInteractedSlot);
    }

    public int count() {
        return (int)this.items.stream().filter(Predicate.not(ItemStack::isEmpty)).count();
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    @Override
    public int getContainerSize() {
        return 6;
    }

    @Override
    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    /**
     * Returns the stack in the given slot.
     */
    @Override
    public ItemStack getItem(int pSlot) {
        return this.items.get(pSlot);
    }

    /**
     * Removes up to a specified number of items from an inventory slot and returns them in a new stack.
     */
    @Override
    public ItemStack removeItem(int pSlot, int pAmount) {
        ItemStack itemstack = Objects.requireNonNullElse(this.items.get(pSlot), ItemStack.EMPTY);
        this.items.set(pSlot, ItemStack.EMPTY);
        if (!itemstack.isEmpty()) {
            this.updateState(pSlot);
        }

        return itemstack;
    }

    /**
     * Removes a stack from the given slot and returns it.
     */
    @Override
    public ItemStack removeItemNoUpdate(int pSlot) {
        return this.removeItem(pSlot, 1);
    }

    /**
     * Sets the given item stack to the specified slot in the inventory (can be crafting or armor sections).
     */
    @Override
    public void setItem(int pSlot, ItemStack pStack) {
        if (pStack.is(ItemTags.BOOKSHELF_BOOKS)) {
            this.items.set(pSlot, pStack);
            this.updateState(pSlot);
        } else if (pStack.isEmpty()) {
            this.removeItem(pSlot, 1);
        }
    }

    /**
     * {@return {@code true} if the given stack can be extracted into the target inventory}
     *
     * @param pTarget the container into which the item should be extracted
     * @param pSlot   the slot from which to extract the item
     * @param pStack  the item to extract
     */
    @Override
    public boolean canTakeItem(Container pTarget, int pSlot, ItemStack pStack) {
        return pTarget.hasAnyMatching(
            p_335300_ -> p_335300_.isEmpty()
                    ? true
                    : ItemStack.isSameItemSameComponents(pStack, p_335300_)
                        && p_335300_.getCount() + pStack.getCount() <= pTarget.getMaxStackSize(p_335300_)
        );
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    /**
     * Don't rename this method to canInteractWith due to conflicts with Container
     */
    @Override
    public boolean stillValid(Player pPlayer) {
        return Container.stillValidBlockEntity(this, pPlayer);
    }

    /**
     * Returns {@code true} if automation is allowed to insert the given stack (ignoring stack size) into the given slot. For guis use Slot.isItemValid
     */
    @Override
    public boolean canPlaceItem(int pSlot, ItemStack pStack) {
        return pStack.is(ItemTags.BOOKSHELF_BOOKS) && this.getItem(pSlot).isEmpty() && pStack.getCount() == this.getMaxStackSize();
    }

    public int getLastInteractedSlot() {
        return this.lastInteractedSlot;
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput pComponentInput) {
        super.applyImplicitComponents(pComponentInput);
        pComponentInput.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(this.items);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder pComponents) {
        super.collectImplicitComponents(pComponents);
        pComponents.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.items));
    }

    @Override
    public void removeComponentsFromTag(CompoundTag pTag) {
        pTag.remove("Items");
    }
}
