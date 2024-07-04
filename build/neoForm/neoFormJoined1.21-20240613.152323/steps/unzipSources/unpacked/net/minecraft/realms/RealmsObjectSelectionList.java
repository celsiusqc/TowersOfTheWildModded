package net.minecraft.realms;

import java.util.Collection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class RealmsObjectSelectionList<E extends ObjectSelectionList.Entry<E>> extends ObjectSelectionList<E> {
    protected RealmsObjectSelectionList(int pWidth, int pHeight, int pY, int pItemHeight) {
        super(Minecraft.getInstance(), pWidth, pHeight, pY, pItemHeight);
    }

    public void setSelectedItem(int pIndex) {
        if (pIndex == -1) {
            this.setSelected(null);
        } else if (super.getItemCount() != 0) {
            this.setSelected(this.getEntry(pIndex));
        }
    }

    public void selectItem(int pIndex) {
        this.setSelectedItem(pIndex);
    }

    @Override
    public int getMaxPosition() {
        return 0;
    }

    @Override
    public int getRowWidth() {
        return (int)((double)this.width * 0.6);
    }

    @Override
    public void replaceEntries(Collection<E> pEntries) {
        super.replaceEntries(pEntries);
    }

    @Override
    public int getItemCount() {
        return super.getItemCount();
    }

    @Override
    public int getRowTop(int pIndex) {
        return super.getRowTop(pIndex);
    }

    @Override
    public int getRowLeft() {
        return super.getRowLeft();
    }

    public int addEntry(E pEntry) {
        return super.addEntry(pEntry);
    }

    public void clear() {
        this.clearEntries();
    }
}
