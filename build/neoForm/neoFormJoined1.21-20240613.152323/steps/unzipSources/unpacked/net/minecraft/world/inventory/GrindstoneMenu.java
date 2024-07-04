package net.minecraft.world.inventory;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class GrindstoneMenu extends AbstractContainerMenu {
    public static final int MAX_NAME_LENGTH = 35;
    public static final int INPUT_SLOT = 0;
    public static final int ADDITIONAL_SLOT = 1;
    public static final int RESULT_SLOT = 2;
    private static final int INV_SLOT_START = 3;
    private static final int INV_SLOT_END = 30;
    private static final int USE_ROW_SLOT_START = 30;
    private static final int USE_ROW_SLOT_END = 39;
    /**
     * The inventory slot that stores the output of the crafting recipe.
     */
    private final Container resultSlots = new ResultContainer();
    final Container repairSlots = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            GrindstoneMenu.this.slotsChanged(this);
        }
    };
    private final ContainerLevelAccess access;
    private int xp = -1;

    public GrindstoneMenu(int pContainerId, Inventory pPlayerInventory) {
        this(pContainerId, pPlayerInventory, ContainerLevelAccess.NULL);
    }

    public GrindstoneMenu(int pContainerId, Inventory pPlayerInventory, final ContainerLevelAccess pAccess) {
        super(MenuType.GRINDSTONE, pContainerId);
        this.access = pAccess;
        this.addSlot(new Slot(this.repairSlots, 0, 49, 19) {
            @Override
            public boolean mayPlace(ItemStack p_39607_) {
                return p_39607_.isDamageableItem() || EnchantmentHelper.hasAnyEnchantments(p_39607_) || p_39607_.canGrindstoneRepair();
            }
        });
        this.addSlot(new Slot(this.repairSlots, 1, 49, 40) {
            @Override
            public boolean mayPlace(ItemStack p_39616_) {
                return p_39616_.isDamageableItem() || EnchantmentHelper.hasAnyEnchantments(p_39616_) || p_39616_.canGrindstoneRepair();
            }
        });
        this.addSlot(new Slot(this.resultSlots, 2, 129, 34) {
            /**
             * Check if the stack is allowed to be placed in this slot, used for armor slots as well as furnace fuel.
             */
            @Override
            public boolean mayPlace(ItemStack p_39630_) {
                return false;
            }

            @Override
            public void onTake(Player p_150574_, ItemStack p_150575_) {
                if (net.neoforged.neoforge.common.CommonHooks.onGrindstoneTake(GrindstoneMenu.this.repairSlots, pAccess, this::getExperienceAmount)) return;
                pAccess.execute((p_39634_, p_39635_) -> {
                    if (p_39634_ instanceof ServerLevel) {
                        ExperienceOrb.award((ServerLevel)p_39634_, Vec3.atCenterOf(p_39635_), this.getExperienceAmount(p_39634_));
                    }

                    p_39634_.levelEvent(1042, p_39635_, 0);
                });
                GrindstoneMenu.this.repairSlots.setItem(0, ItemStack.EMPTY);
                GrindstoneMenu.this.repairSlots.setItem(1, ItemStack.EMPTY);
            }

            /**
             * Returns the total amount of XP stored in all the input slots of this container. The return value is randomized, so that it returns between 50% and 100% of the total XP.
             */
            private int getExperienceAmount(Level p_39632_) {
                if (xp > -1) return xp;
                int l = 0;
                l += this.getExperienceFromItem(GrindstoneMenu.this.repairSlots.getItem(0));
                l += this.getExperienceFromItem(GrindstoneMenu.this.repairSlots.getItem(1));
                if (l > 0) {
                    int i1 = (int)Math.ceil((double)l / 2.0);
                    return i1 + p_39632_.random.nextInt(i1);
                } else {
                    return 0;
                }
            }

            /**
             * Returns the total amount of XP stored in the enchantments of this stack.
             */
            private int getExperienceFromItem(ItemStack p_39637_) {
                int l = 0;
                ItemEnchantments itemenchantments = EnchantmentHelper.getEnchantmentsForCrafting(p_39637_);

                for (Entry<Holder<Enchantment>> entry : itemenchantments.entrySet()) {
                    Holder<Enchantment> holder = entry.getKey();
                    int i1 = entry.getIntValue();
                    if (!holder.is(EnchantmentTags.CURSE)) {
                        l += holder.value().getMinCost(i1);
                    }
                }

                return l;
            }
        });

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
     * Callback for when the crafting matrix is changed.
     */
    @Override
    public void slotsChanged(Container pInventory) {
        super.slotsChanged(pInventory);
        if (pInventory == this.repairSlots) {
            this.createResult();
        }
    }

    private void createResult() {
        this.resultSlots.setItem(0, this.computeResult(this.repairSlots.getItem(0), this.repairSlots.getItem(1)));
        this.broadcastChanges();
    }

    private ItemStack computeResult(ItemStack pInputItem, ItemStack pAdditionalItem) {
        boolean flag = !pInputItem.isEmpty() || !pAdditionalItem.isEmpty();
        this.xp = net.neoforged.neoforge.common.CommonHooks.onGrindstoneChange(pInputItem, pAdditionalItem, this.resultSlots, -1);
        if (this.xp != Integer.MIN_VALUE) return ItemStack.EMPTY; // Porting 1.20.5 check if this is correct
        if (!flag) {
            return ItemStack.EMPTY;
        } else if (pInputItem.getCount() <= 1 && pAdditionalItem.getCount() <= 1) {
            boolean flag1 = !pInputItem.isEmpty() && !pAdditionalItem.isEmpty();
            if (!flag1) {
                ItemStack itemstack = !pInputItem.isEmpty() ? pInputItem : pAdditionalItem;
                return !EnchantmentHelper.hasAnyEnchantments(itemstack) ? ItemStack.EMPTY : this.removeNonCursesFrom(itemstack.copy());
            } else {
                return this.mergeItems(pInputItem, pAdditionalItem);
            }
        } else {
            return ItemStack.EMPTY;
        }
    }

    private ItemStack mergeItems(ItemStack pInputItem, ItemStack pAdditionalItem) {
        if (!pInputItem.is(pAdditionalItem.getItem())) {
            return ItemStack.EMPTY;
        } else {
            int i = Math.max(pInputItem.getMaxDamage(), pAdditionalItem.getMaxDamage());
            int j = pInputItem.getMaxDamage() - pInputItem.getDamageValue();
            int k = pAdditionalItem.getMaxDamage() - pAdditionalItem.getDamageValue();
            int l = j + k + i * 5 / 100;
            int i1 = 1;
            if (!pInputItem.isDamageableItem() || !pInputItem.isRepairable()) {
                if (pInputItem.getMaxStackSize() < 2 || !ItemStack.matches(pInputItem, pAdditionalItem)) {
                    return ItemStack.EMPTY;
                }

                i1 = 2;
            }

            ItemStack itemstack = pInputItem.copyWithCount(i1);
            if (itemstack.isDamageableItem()) {
                itemstack.set(DataComponents.MAX_DAMAGE, i);
                itemstack.setDamageValue(Math.max(i - l, 0));
                if (!pAdditionalItem.isRepairable()) itemstack.setDamageValue(pInputItem.getDamageValue());
            }

            this.mergeEnchantsFrom(itemstack, pAdditionalItem);
            return this.removeNonCursesFrom(itemstack);
        }
    }

    private void mergeEnchantsFrom(ItemStack pInputItem, ItemStack pAdditionalItem) {
        EnchantmentHelper.updateEnchantments(pInputItem, p_344370_ -> {
            ItemEnchantments itemenchantments = EnchantmentHelper.getEnchantmentsForCrafting(pAdditionalItem);

            for (Entry<Holder<Enchantment>> entry : itemenchantments.entrySet()) {
                Holder<Enchantment> holder = entry.getKey();
                if (!holder.is(EnchantmentTags.CURSE) || p_344370_.getLevel(holder) == 0) {
                    p_344370_.upgrade(holder, entry.getIntValue());
                }
            }
        });
    }

    private ItemStack removeNonCursesFrom(ItemStack pItem) {
        ItemEnchantments itemenchantments = EnchantmentHelper.updateEnchantments(
            pItem, p_330066_ -> p_330066_.removeIf(p_344368_ -> !p_344368_.is(EnchantmentTags.CURSE))
        );
        if (pItem.is(Items.ENCHANTED_BOOK) && itemenchantments.isEmpty()) {
            pItem = pItem.transmuteCopy(Items.BOOK);
        }

        int i = 0;

        for (int j = 0; j < itemenchantments.size(); j++) {
            i = AnvilMenu.calculateIncreasedRepairCost(i);
        }

        pItem.set(DataComponents.REPAIR_COST, i);
        return pItem;
    }

    /**
     * Called when the container is closed.
     */
    @Override
    public void removed(Player pPlayer) {
        super.removed(pPlayer);
        this.access.execute((p_39575_, p_39576_) -> this.clearContainer(pPlayer, this.repairSlots));
    }

    /**
     * Determines whether supplied player can use this container
     */
    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(this.access, pPlayer, Blocks.GRINDSTONE);
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
            ItemStack itemstack2 = this.repairSlots.getItem(0);
            ItemStack itemstack3 = this.repairSlots.getItem(1);
            if (pIndex == 2) {
                if (!this.moveItemStackTo(itemstack1, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemstack1, itemstack);
            } else if (pIndex != 0 && pIndex != 1) {
                if (!itemstack2.isEmpty() && !itemstack3.isEmpty()) {
                    if (pIndex >= 3 && pIndex < 30) {
                        if (!this.moveItemStackTo(itemstack1, 30, 39, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (pIndex >= 30 && pIndex < 39 && !this.moveItemStackTo(itemstack1, 3, 30, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemstack1, 0, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 3, 39, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
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
}
