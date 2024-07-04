package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class DispenserBlockEntity extends RandomizableContainerBlockEntity {
    public static final int CONTAINER_SIZE = 9;
    private NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);

    protected DispenserBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }

    public DispenserBlockEntity(BlockPos pPos, BlockState pBlockState) {
        this(BlockEntityType.DISPENSER, pPos, pBlockState);
    }

    @Override
    public int getContainerSize() {
        return 9;
    }

    public int getRandomSlot(RandomSource pRandom) {
        this.unpackLootTable(null);
        int i = -1;
        int j = 1;

        for (int k = 0; k < this.items.size(); k++) {
            if (!this.items.get(k).isEmpty() && pRandom.nextInt(j++) == 0) {
                i = k;
            }
        }

        return i;
    }

    public ItemStack insertItem(ItemStack p_347519_) {
        int i = this.getMaxStackSize(p_347519_);

        for (int j = 0; j < this.items.size(); j++) {
            ItemStack itemstack = this.items.get(j);
            if (itemstack.isEmpty() || ItemStack.isSameItemSameComponents(p_347519_, itemstack)) {
                int k = Math.min(p_347519_.getCount(), i - itemstack.getCount());
                if (k > 0) {
                    if (itemstack.isEmpty()) {
                        this.setItem(j, p_347519_.split(k));
                    } else {
                        p_347519_.shrink(k);
                        itemstack.grow(k);
                    }
                }

                if (p_347519_.isEmpty()) {
                    break;
                }
            }
        }

        return p_347519_;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.dispenser");
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(pTag)) {
            ContainerHelper.loadAllItems(pTag, this.items, pRegistries);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(pTag, pRegistries);
        if (!this.trySaveLootTable(pTag)) {
            ContainerHelper.saveAllItems(pTag, this.items, pRegistries);
        }
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> pItems) {
        this.items = pItems;
    }

    @Override
    protected AbstractContainerMenu createMenu(int pId, Inventory pPlayer) {
        return new DispenserMenu(pId, pPlayer, this);
    }
}
