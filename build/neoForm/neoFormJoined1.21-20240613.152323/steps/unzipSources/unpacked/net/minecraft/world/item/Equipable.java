package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public interface Equipable {
    EquipmentSlot getEquipmentSlot();

    default Holder<SoundEvent> getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_GENERIC;
    }

    default InteractionResultHolder<ItemStack> swapWithEquipmentSlot(Item pItem, Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        EquipmentSlot equipmentslot = pPlayer.getEquipmentSlotForItem(itemstack);
        if (!pPlayer.canUseSlot(equipmentslot)) {
            return InteractionResultHolder.pass(itemstack);
        } else {
            ItemStack itemstack1 = pPlayer.getItemBySlot(equipmentslot);
            if ((!EnchantmentHelper.has(itemstack1, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE) || pPlayer.isCreative())
                && !ItemStack.matches(itemstack, itemstack1)) {
                if (!pLevel.isClientSide()) {
                    pPlayer.awardStat(Stats.ITEM_USED.get(pItem));
                }

                ItemStack itemstack2 = itemstack1.isEmpty() ? itemstack : itemstack1.copyAndClear();
                ItemStack itemstack3 = pPlayer.isCreative() ? itemstack.copy() : itemstack.copyAndClear();
                pPlayer.setItemSlot(equipmentslot, itemstack3);
                return InteractionResultHolder.sidedSuccess(itemstack2, pLevel.isClientSide());
            } else {
                return InteractionResultHolder.fail(itemstack);
            }
        }
    }

    @Nullable
    static Equipable get(ItemStack pStack) {
        Item $$3 = pStack.getItem();
        if ($$3 instanceof Equipable) {
            return (Equipable)$$3;
        } else {
            if (pStack.getItem() instanceof BlockItem blockitem) {
                Block block = blockitem.getBlock();
                if (block instanceof Equipable) {
                    return (Equipable)block;
                }
            }

            return null;
        }
    }
}
