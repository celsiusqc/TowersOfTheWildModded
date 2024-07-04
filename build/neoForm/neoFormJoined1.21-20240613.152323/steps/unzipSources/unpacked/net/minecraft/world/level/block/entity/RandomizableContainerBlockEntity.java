package net.minecraft.world.level.block.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;

public abstract class RandomizableContainerBlockEntity extends BaseContainerBlockEntity implements RandomizableContainer {
    @Nullable
    protected ResourceKey<LootTable> lootTable;
    protected long lootTableSeed = 0L;

    protected RandomizableContainerBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }

    @Nullable
    @Override
    public ResourceKey<LootTable> getLootTable() {
        return this.lootTable;
    }

    @Override
    public void setLootTable(@Nullable ResourceKey<LootTable> pLootTable) {
        this.lootTable = pLootTable;
    }

    @Override
    public long getLootTableSeed() {
        return this.lootTableSeed;
    }

    @Override
    public void setLootTableSeed(long pSeed) {
        this.lootTableSeed = pSeed;
    }

    @Override
    public boolean isEmpty() {
        this.unpackLootTable(null);
        return super.isEmpty();
    }

    /**
     * Returns the stack in the given slot.
     */
    @Override
    public ItemStack getItem(int pIndex) {
        this.unpackLootTable(null);
        return super.getItem(pIndex);
    }

    /**
     * Removes up to a specified number of items from an inventory slot and returns them in a new stack.
     */
    @Override
    public ItemStack removeItem(int pIndex, int pCount) {
        this.unpackLootTable(null);
        return super.removeItem(pIndex, pCount);
    }

    /**
     * Removes a stack from the given slot and returns it.
     */
    @Override
    public ItemStack removeItemNoUpdate(int pIndex) {
        this.unpackLootTable(null);
        return super.removeItemNoUpdate(pIndex);
    }

    /**
     * Sets the given item stack to the specified slot in the inventory (can be crafting or armor sections).
     */
    @Override
    public void setItem(int pIndex, ItemStack pStack) {
        this.unpackLootTable(null);
        super.setItem(pIndex, pStack);
    }

    @Override
    public boolean canOpen(Player pPlayer) {
        return super.canOpen(pPlayer) && (this.lootTable == null || !pPlayer.isSpectator());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        if (this.canOpen(pPlayer)) {
            this.unpackLootTable(pPlayerInventory.player);
            return this.createMenu(pContainerId, pPlayerInventory);
        } else {
            return null;
        }
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput pComponentInput) {
        super.applyImplicitComponents(pComponentInput);
        SeededContainerLoot seededcontainerloot = pComponentInput.get(DataComponents.CONTAINER_LOOT);
        if (seededcontainerloot != null) {
            this.lootTable = seededcontainerloot.lootTable();
            this.lootTableSeed = seededcontainerloot.seed();
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder pComponents) {
        super.collectImplicitComponents(pComponents);
        if (this.lootTable != null) {
            pComponents.set(DataComponents.CONTAINER_LOOT, new SeededContainerLoot(this.lootTable, this.lootTableSeed));
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag pTag) {
        super.removeComponentsFromTag(pTag);
        pTag.remove("LootTable");
        pTag.remove("LootTableSeed");
    }
}
