package net.minecraft.world.inventory;

import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;

public class BrewingStandMenu extends AbstractContainerMenu {
    private static final int BOTTLE_SLOT_START = 0;
    private static final int BOTTLE_SLOT_END = 2;
    private static final int INGREDIENT_SLOT = 3;
    private static final int FUEL_SLOT = 4;
    private static final int SLOT_COUNT = 5;
    private static final int DATA_COUNT = 2;
    private static final int INV_SLOT_START = 5;
    private static final int INV_SLOT_END = 32;
    private static final int USE_ROW_SLOT_START = 32;
    private static final int USE_ROW_SLOT_END = 41;
    private final Container brewingStand;
    private final ContainerData brewingStandData;
    private final Slot ingredientSlot;

    public BrewingStandMenu(int pContainerId, Inventory pPlayerInventory) {
        this(pContainerId, pPlayerInventory, new SimpleContainer(5), new SimpleContainerData(2));
    }

    public BrewingStandMenu(int pContainerId, Inventory pPlayerInventory, Container pBrewingStandContainer, ContainerData pBrewingStandData) {
        super(MenuType.BREWING_STAND, pContainerId);
        checkContainerSize(pBrewingStandContainer, 5);
        checkContainerDataCount(pBrewingStandData, 2);
        this.brewingStand = pBrewingStandContainer;
        this.brewingStandData = pBrewingStandData;
        PotionBrewing potionbrewing = pPlayerInventory.player.level().potionBrewing();
        this.addSlot(new BrewingStandMenu.PotionSlot(potionbrewing, pBrewingStandContainer, 0, 56, 51));
        this.addSlot(new BrewingStandMenu.PotionSlot(potionbrewing, pBrewingStandContainer, 1, 79, 58));
        this.addSlot(new BrewingStandMenu.PotionSlot(potionbrewing, pBrewingStandContainer, 2, 102, 51));
        this.ingredientSlot = this.addSlot(new BrewingStandMenu.IngredientsSlot(potionbrewing, pBrewingStandContainer, 3, 79, 17));
        this.addSlot(new BrewingStandMenu.FuelSlot(pBrewingStandContainer, 4, 17, 17));
        this.addDataSlots(pBrewingStandData);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(pPlayerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (int k = 0; k < 9; k++) {
            this.addSlot(new Slot(pPlayerInventory, k, 8 + k * 18, 142));
        }
    }

    /**
     * Determines whether supplied player can use this container
     */
    @Override
    public boolean stillValid(Player pPlayer) {
        return this.brewingStand.stillValid(pPlayer);
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
            if ((pIndex < 0 || pIndex > 2) && pIndex != 3 && pIndex != 4) {
                if (BrewingStandMenu.FuelSlot.mayPlaceItem(itemstack)) {
                    if (this.moveItemStackTo(itemstack1, 4, 5, false)
                        || this.ingredientSlot.mayPlace(itemstack1) && !this.moveItemStackTo(itemstack1, 3, 4, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (this.ingredientSlot.mayPlace(itemstack1)) {
                    if (!this.moveItemStackTo(itemstack1, 3, 4, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (BrewingStandMenu.PotionSlot.mayPlaceItem(pPlayer.level().potionBrewing(), itemstack)) {
                    if (!this.moveItemStackTo(itemstack1, 0, 3, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (pIndex >= 5 && pIndex < 32) {
                    if (!this.moveItemStackTo(itemstack1, 32, 41, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (pIndex >= 32 && pIndex < 41) {
                    if (!this.moveItemStackTo(itemstack1, 5, 32, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemstack1, 5, 41, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(itemstack1, 5, 41, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemstack1, itemstack);
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(pPlayer, itemstack);
        }

        return itemstack;
    }

    public int getFuel() {
        return this.brewingStandData.get(1);
    }

    public int getBrewingTicks() {
        return this.brewingStandData.get(0);
    }

    static class FuelSlot extends Slot {
        public FuelSlot(Container pContainer, int pSlot, int pX, int pY) {
            super(pContainer, pSlot, pX, pY);
        }

        /**
         * Check if the stack is allowed to be placed in this slot, used for armor slots as well as furnace fuel.
         */
        @Override
        public boolean mayPlace(ItemStack pStack) {
            return mayPlaceItem(pStack);
        }

        /**
         * Returns {@code true} if the given {@link net.minecraft.world.item.ItemStack} is usable as fuel in the brewing stand.
         */
        public static boolean mayPlaceItem(ItemStack pItemStack) {
            return pItemStack.is(Items.BLAZE_POWDER);
        }
    }

    static class IngredientsSlot extends Slot {
        private final PotionBrewing potionBrewing;

        public IngredientsSlot(PotionBrewing pPotionBrewing, Container pContainer, int pSlot, int pX, int pY) {
            super(pContainer, pSlot, pX, pY);
            this.potionBrewing = pPotionBrewing;
        }

        /**
         * Check if the stack is allowed to be placed in this slot, used for armor slots as well as furnace fuel.
         */
        @Override
        public boolean mayPlace(ItemStack pStack) {
            return this.potionBrewing.isIngredient(pStack);
        }
    }

    static class PotionSlot extends Slot {
        private final PotionBrewing potionBrewing;

        public PotionSlot(Container pContainer, int pSlot, int pX, int pY) {
            this(PotionBrewing.EMPTY, pContainer, pSlot, pX, pY);
        }

        public PotionSlot(PotionBrewing potionBrewing, Container p_39123_, int p_39124_, int p_39125_, int p_39126_) {
            super(p_39123_, p_39124_, p_39125_, p_39126_);
            this.potionBrewing = potionBrewing;
        }

        /**
         * Check if the stack is allowed to be placed in this slot, used for armor slots as well as furnace fuel.
         */
        @Override
        public boolean mayPlace(ItemStack pStack) {
            return mayPlaceItem(this.potionBrewing, pStack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void onTake(Player pPlayer, ItemStack pStack) {
            Optional<Holder<Potion>> optional = pStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion();
            if (optional.isPresent() && pPlayer instanceof ServerPlayer serverplayer) {
                net.neoforged.neoforge.event.EventHooks.onPlayerBrewedPotion(pPlayer, pStack);
                CriteriaTriggers.BREWED_POTION.trigger(serverplayer, optional.get());
            }

            super.onTake(pPlayer, pStack);
        }

        /**
         * Returns {@code true} if this {@link net.minecraft.world.item.ItemStack} can be filled with a potion.
         */
        @Deprecated // Neo: use the overload that takes PotionBrewing instead
        public static boolean mayPlaceItem(ItemStack pStack) {
            return pStack.is(Items.POTION) || pStack.is(Items.SPLASH_POTION) || pStack.is(Items.LINGERING_POTION) || pStack.is(Items.GLASS_BOTTLE);
        }

        public static boolean mayPlaceItem(PotionBrewing potionBrewing, ItemStack p_39134_) {
            return potionBrewing.isInput(p_39134_) || p_39134_.is(Items.GLASS_BOTTLE);
        }
    }
}
