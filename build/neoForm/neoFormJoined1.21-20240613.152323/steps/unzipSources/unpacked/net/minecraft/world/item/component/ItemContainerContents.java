package net.minecraft.world.item.component;

import com.google.common.collect.Iterables;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Stream;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public final class ItemContainerContents {
    private static final int NO_SLOT = -1;
    private static final int MAX_SIZE = 256;
    public static final ItemContainerContents EMPTY = new ItemContainerContents(NonNullList.create());
    public static final Codec<ItemContainerContents> CODEC = ItemContainerContents.Slot.CODEC
        .sizeLimitedListOf(256)
        .xmap(ItemContainerContents::fromSlots, ItemContainerContents::asSlots);
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemContainerContents> STREAM_CODEC = ItemStack.OPTIONAL_STREAM_CODEC
        .apply(ByteBufCodecs.list(256))
        .map(ItemContainerContents::new, p_331691_ -> p_331691_.items);
    private final NonNullList<ItemStack> items;
    private final int hashCode;

    private ItemContainerContents(NonNullList<ItemStack> pItems) {
        if (pItems.size() > 256) {
            throw new IllegalArgumentException("Got " + pItems.size() + " items, but maximum is 256");
        } else {
            this.items = pItems;
            this.hashCode = ItemStack.hashStackList(pItems);
        }
    }

    private ItemContainerContents(int pSize) {
        this(NonNullList.withSize(pSize, ItemStack.EMPTY));
    }

    private ItemContainerContents(List<ItemStack> p_331046_) {
        this(p_331046_.size());

        for (int i = 0; i < p_331046_.size(); i++) {
            this.items.set(i, p_331046_.get(i));
        }
    }

    private static ItemContainerContents fromSlots(List<ItemContainerContents.Slot> p_331424_) {
        OptionalInt optionalint = p_331424_.stream().mapToInt(ItemContainerContents.Slot::index).max();
        if (optionalint.isEmpty()) {
            return EMPTY;
        } else {
            ItemContainerContents itemcontainercontents = new ItemContainerContents(optionalint.getAsInt() + 1);

            for (ItemContainerContents.Slot itemcontainercontents$slot : p_331424_) {
                itemcontainercontents.items.set(itemcontainercontents$slot.index(), itemcontainercontents$slot.item());
            }

            return itemcontainercontents;
        }
    }

    public static ItemContainerContents fromItems(List<ItemStack> pItems) {
        int i = findLastNonEmptySlot(pItems);
        if (i == -1) {
            return EMPTY;
        } else {
            ItemContainerContents itemcontainercontents = new ItemContainerContents(i + 1);

            for (int j = 0; j <= i; j++) {
                itemcontainercontents.items.set(j, pItems.get(j).copy());
            }

            return itemcontainercontents;
        }
    }

    private static int findLastNonEmptySlot(List<ItemStack> pItems) {
        for (int i = pItems.size() - 1; i >= 0; i--) {
            if (!pItems.get(i).isEmpty()) {
                return i;
            }
        }

        return -1;
    }

    private List<ItemContainerContents.Slot> asSlots() {
        List<ItemContainerContents.Slot> list = new ArrayList<>();

        for (int i = 0; i < this.items.size(); i++) {
            ItemStack itemstack = this.items.get(i);
            if (!itemstack.isEmpty()) {
                list.add(new ItemContainerContents.Slot(i, itemstack));
            }
        }

        return list;
    }

    public void copyInto(NonNullList<ItemStack> pList) {
        for (int i = 0; i < pList.size(); i++) {
            ItemStack itemstack = i < this.items.size() ? this.items.get(i) : ItemStack.EMPTY;
            pList.set(i, itemstack.copy());
        }
    }

    public ItemStack copyOne() {
        return this.items.isEmpty() ? ItemStack.EMPTY : this.items.get(0).copy();
    }

    public Stream<ItemStack> stream() {
        return this.items.stream().map(ItemStack::copy);
    }

    public Stream<ItemStack> nonEmptyStream() {
        return this.items.stream().filter(p_331322_ -> !p_331322_.isEmpty()).map(ItemStack::copy);
    }

    public Iterable<ItemStack> nonEmptyItems() {
        return Iterables.filter(this.items, p_331420_ -> !p_331420_.isEmpty());
    }

    public Iterable<ItemStack> nonEmptyItemsCopy() {
        return Iterables.transform(this.nonEmptyItems(), ItemStack::copy);
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else {
            if (pOther instanceof ItemContainerContents itemcontainercontents && ItemStack.listMatches(this.items, itemcontainercontents.items)) {
                return true;
            }

            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    // Neo Start

    /**
     * {@return the number of slots in this container}
     */
    public int getSlots() {
        return this.items.size();
    }

    /**
     * Gets a copy of the stack at a particular slot.
     *
     * @param slot The slot to check. Must be within [0, {@link #getSlots()}]
     * @return A copy of the stack in that slot
     * @throws UnsupportedOperationException if the provided slot index is out-of-bounds.
     */
    public ItemStack getStackInSlot(int slot) {
        validateSlotIndex(slot);
        return this.items.get(slot).copy();
    }

    /**
     * Throws {@link UnsupportedOperationException} if the provided slot index is invalid.
     */
    private void validateSlotIndex(int slot) {
        if (slot < 0 || slot >= getSlots()) {
            throw new UnsupportedOperationException("Slot " + slot + " not in valid range - [0," + getSlots() + ")");
        }
    }

    // Neo End


    static record Slot(int index, ItemStack item) {
        public static final Codec<ItemContainerContents.Slot> CODEC = RecordCodecBuilder.create(
            p_331695_ -> p_331695_.group(
                        Codec.intRange(0, 255).fieldOf("slot").forGetter(ItemContainerContents.Slot::index),
                        ItemStack.CODEC.fieldOf("item").forGetter(ItemContainerContents.Slot::item)
                    )
                    .apply(p_331695_, ItemContainerContents.Slot::new)
        );
    }
}
