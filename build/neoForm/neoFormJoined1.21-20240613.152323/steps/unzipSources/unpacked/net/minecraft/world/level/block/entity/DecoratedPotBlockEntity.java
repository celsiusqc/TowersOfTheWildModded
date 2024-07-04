package net.minecraft.world.level.block.entity;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.ticks.ContainerSingleItem;

public class DecoratedPotBlockEntity extends BlockEntity implements RandomizableContainer, ContainerSingleItem.BlockContainerSingleItem {
    public static final String TAG_SHERDS = "sherds";
    public static final String TAG_ITEM = "item";
    public static final int EVENT_POT_WOBBLES = 1;
    public long wobbleStartedAtTick;
    @Nullable
    public DecoratedPotBlockEntity.WobbleStyle lastWobbleStyle;
    private PotDecorations decorations;
    private ItemStack item = ItemStack.EMPTY;
    @Nullable
    protected ResourceKey<LootTable> lootTable;
    protected long lootTableSeed;

    public DecoratedPotBlockEntity(BlockPos pPos, BlockState pState) {
        super(BlockEntityType.DECORATED_POT, pPos, pState);
        this.decorations = PotDecorations.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(pTag, pRegistries);
        this.decorations.save(pTag);
        if (!this.trySaveLootTable(pTag) && !this.item.isEmpty()) {
            pTag.put("item", this.item.save(pRegistries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);
        this.decorations = PotDecorations.load(pTag);
        if (!this.tryLoadLootTable(pTag)) {
            if (pTag.contains("item", 10)) {
                this.item = ItemStack.parse(pRegistries, pTag.getCompound("item")).orElse(ItemStack.EMPTY);
            } else {
                this.item = ItemStack.EMPTY;
            }
        }
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return this.saveCustomOnly(pRegistries);
    }

    public Direction getDirection() {
        return this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    public PotDecorations getDecorations() {
        return this.decorations;
    }

    public void setFromItem(ItemStack pItem) {
        this.applyComponentsFromItemStack(pItem);
    }

    public ItemStack getPotAsItem() {
        ItemStack itemstack = Items.DECORATED_POT.getDefaultInstance();
        itemstack.applyComponents(this.collectComponents());
        return itemstack;
    }

    public static ItemStack createDecoratedPotItem(PotDecorations p_330827_) {
        ItemStack itemstack = Items.DECORATED_POT.getDefaultInstance();
        itemstack.set(DataComponents.POT_DECORATIONS, p_330827_);
        return itemstack;
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
    protected void collectImplicitComponents(DataComponentMap.Builder pComponents) {
        super.collectImplicitComponents(pComponents);
        pComponents.set(DataComponents.POT_DECORATIONS, this.decorations);
        pComponents.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(this.item)));
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput pComponentInput) {
        super.applyImplicitComponents(pComponentInput);
        this.decorations = pComponentInput.getOrDefault(DataComponents.POT_DECORATIONS, PotDecorations.EMPTY);
        this.item = pComponentInput.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyOne();
    }

    @Override
    public void removeComponentsFromTag(CompoundTag pTag) {
        super.removeComponentsFromTag(pTag);
        pTag.remove("sherds");
        pTag.remove("item");
    }

    @Override
    public ItemStack getTheItem() {
        this.unpackLootTable(null);
        return this.item;
    }

    @Override
    public ItemStack splitTheItem(int pAmount) {
        this.unpackLootTable(null);
        ItemStack itemstack = this.item.split(pAmount);
        if (this.item.isEmpty()) {
            this.item = ItemStack.EMPTY;
        }

        return itemstack;
    }

    @Override
    public void setTheItem(ItemStack pItem) {
        this.unpackLootTable(null);
        this.item = pItem;
    }

    @Override
    public BlockEntity getContainerBlockEntity() {
        return this;
    }

    public void wobble(DecoratedPotBlockEntity.WobbleStyle pStyle) {
        if (this.level != null && !this.level.isClientSide()) {
            this.level.blockEvent(this.getBlockPos(), this.getBlockState().getBlock(), 1, pStyle.ordinal());
        }
    }

    @Override
    public boolean triggerEvent(int pId, int pType) {
        if (this.level != null && pId == 1 && pType >= 0 && pType < DecoratedPotBlockEntity.WobbleStyle.values().length) {
            this.wobbleStartedAtTick = this.level.getGameTime();
            this.lastWobbleStyle = DecoratedPotBlockEntity.WobbleStyle.values()[pType];
            return true;
        } else {
            return super.triggerEvent(pId, pType);
        }
    }

    public static enum WobbleStyle {
        POSITIVE(7),
        NEGATIVE(10);

        public final int duration;

        private WobbleStyle(int pDuration) {
            this.duration = pDuration;
        }
    }
}
