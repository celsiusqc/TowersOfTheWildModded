package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public class CrafterSlot extends Slot {
    private final CrafterMenu menu;

    public CrafterSlot(Container pContainer, int pSlot, int pX, int pY, CrafterMenu pMenu) {
        super(pContainer, pSlot, pX, pY);
        this.menu = pMenu;
    }

    /**
     * Check if the stack is allowed to be placed in this slot, used for armor slots as well as furnace fuel.
     */
    @Override
    public boolean mayPlace(ItemStack pStack) {
        return !this.menu.isSlotDisabled(this.index) && super.mayPlace(pStack);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        this.menu.slotsChanged(this.container);
    }
}
