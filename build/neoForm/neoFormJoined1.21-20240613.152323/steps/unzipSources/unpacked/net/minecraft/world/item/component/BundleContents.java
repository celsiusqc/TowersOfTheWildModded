package net.minecraft.world.item.component;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import org.apache.commons.lang3.math.Fraction;

public final class BundleContents implements TooltipComponent {
    public static final BundleContents EMPTY = new BundleContents(List.of());
    public static final Codec<BundleContents> CODEC = ItemStack.CODEC.listOf().xmap(BundleContents::new, p_331551_ -> p_331551_.items);
    public static final StreamCodec<RegistryFriendlyByteBuf, BundleContents> STREAM_CODEC = ItemStack.STREAM_CODEC
        .apply(ByteBufCodecs.list())
        .map(BundleContents::new, p_331649_ -> p_331649_.items);
    private static final Fraction BUNDLE_IN_BUNDLE_WEIGHT = Fraction.getFraction(1, 16);
    private static final int NO_STACK_INDEX = -1;
    final List<ItemStack> items;
    final Fraction weight;

    BundleContents(List<ItemStack> pItems, Fraction pWeight) {
        this.items = pItems;
        this.weight = pWeight;
    }

    public BundleContents(List<ItemStack> p_331417_) {
        this(p_331417_, computeContentWeight(p_331417_));
    }

    private static Fraction computeContentWeight(List<ItemStack> pContent) {
        Fraction fraction = Fraction.ZERO;

        for (ItemStack itemstack : pContent) {
            fraction = fraction.add(getWeight(itemstack).multiplyBy(Fraction.getFraction(itemstack.getCount(), 1)));
        }

        return fraction;
    }

    static Fraction getWeight(ItemStack pStack) {
        BundleContents bundlecontents = pStack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundlecontents != null) {
            return BUNDLE_IN_BUNDLE_WEIGHT.add(bundlecontents.weight());
        } else {
            List<BeehiveBlockEntity.Occupant> list = pStack.getOrDefault(DataComponents.BEES, List.of());
            return !list.isEmpty() ? Fraction.ONE : Fraction.getFraction(1, pStack.getMaxStackSize());
        }
    }

    public ItemStack getItemUnsafe(int pIndex) {
        return this.items.get(pIndex);
    }

    public Stream<ItemStack> itemCopyStream() {
        return this.items.stream().map(ItemStack::copy);
    }

    public Iterable<ItemStack> items() {
        return this.items;
    }

    public Iterable<ItemStack> itemsCopy() {
        return Lists.transform(this.items, ItemStack::copy);
    }

    public int size() {
        return this.items.size();
    }

    public Fraction weight() {
        return this.weight;
    }

    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else {
            return !(pOther instanceof BundleContents bundlecontents)
                ? false
                : this.weight.equals(bundlecontents.weight) && ItemStack.listMatches(this.items, bundlecontents.items);
        }
    }

    @Override
    public int hashCode() {
        return ItemStack.hashStackList(this.items);
    }

    @Override
    public String toString() {
        return "BundleContents" + this.items;
    }

    public static class Mutable {
        private final List<ItemStack> items;
        private Fraction weight;

        public Mutable(BundleContents pContents) {
            this.items = new ArrayList<>(pContents.items);
            this.weight = pContents.weight;
        }

        public BundleContents.Mutable clearItems() {
            this.items.clear();
            this.weight = Fraction.ZERO;
            return this;
        }

        private int findStackIndex(ItemStack pStack) {
            if (!pStack.isStackable()) {
                return -1;
            } else {
                for (int i = 0; i < this.items.size(); i++) {
                    if (ItemStack.isSameItemSameComponents(this.items.get(i), pStack)) {
                        return i;
                    }
                }

                return -1;
            }
        }

        private int getMaxAmountToAdd(ItemStack pStack) {
            Fraction fraction = Fraction.ONE.subtract(this.weight);
            return Math.max(fraction.divideBy(BundleContents.getWeight(pStack)).intValue(), 0);
        }

        public int tryInsert(ItemStack pStack) {
            if (!pStack.isEmpty() && pStack.getItem().canFitInsideContainerItems()) {
                int i = Math.min(pStack.getCount(), this.getMaxAmountToAdd(pStack));
                if (i == 0) {
                    return 0;
                } else {
                    this.weight = this.weight.add(BundleContents.getWeight(pStack).multiplyBy(Fraction.getFraction(i, 1)));
                    int j = this.findStackIndex(pStack);
                    if (j != -1) {
                        ItemStack itemstack = this.items.remove(j);
                        ItemStack itemstack1 = itemstack.copyWithCount(itemstack.getCount() + i);
                        pStack.shrink(i);
                        this.items.add(0, itemstack1);
                    } else {
                        this.items.add(0, pStack.split(i));
                    }

                    return i;
                }
            } else {
                return 0;
            }
        }

        public int tryTransfer(Slot pSlot, Player pPlayer) {
            ItemStack itemstack = pSlot.getItem();
            int i = this.getMaxAmountToAdd(itemstack);
            return this.tryInsert(pSlot.safeTake(itemstack.getCount(), i, pPlayer));
        }

        @Nullable
        public ItemStack removeOne() {
            if (this.items.isEmpty()) {
                return null;
            } else {
                ItemStack itemstack = this.items.remove(0).copy();
                this.weight = this.weight.subtract(BundleContents.getWeight(itemstack).multiplyBy(Fraction.getFraction(itemstack.getCount(), 1)));
                return itemstack;
            }
        }

        public Fraction weight() {
            return this.weight;
        }

        public BundleContents toImmutable() {
            return new BundleContents(List.copyOf(this.items), this.weight);
        }
    }
}
