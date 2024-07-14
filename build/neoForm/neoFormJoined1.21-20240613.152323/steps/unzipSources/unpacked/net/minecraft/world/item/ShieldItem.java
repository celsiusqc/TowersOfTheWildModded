package net.minecraft.world.item;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public class ShieldItem extends Item implements Equipable {
    public static final int EFFECTIVE_BLOCK_DELAY = 5;
    public static final float MINIMUM_DURABILITY_DAMAGE = 3.0F;

    public ShieldItem(Item.Properties pProperties) {
        super(pProperties);
        DispenserBlock.registerBehavior(this, ArmorItem.DISPENSE_ITEM_BEHAVIOR);
    }

    /**
     * Returns the unlocalized name of this item. This version accepts an ItemStack so different stacks can have different names based on their damage or NBT.
     */
    @Override
    public String getDescriptionId(ItemStack pStack) {
        DyeColor dyecolor = pStack.get(DataComponents.BASE_COLOR);
        return dyecolor != null ? this.getDescriptionId() + "." + dyecolor.getName() : super.getDescriptionId(pStack);
    }

    @Override
    public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        BannerItem.appendHoverTextFromBannerBlockEntityTag(pStack, pTooltipComponents);
    }

    /**
     * Returns the action that specifies what animation to play when the item is being used.
     */
    @Override
    public UseAnim getUseAnimation(ItemStack pStack) {
        return UseAnim.BLOCK;
    }

    /**
     * How long it takes to use or consume an item
     */
    @Override
    public int getUseDuration(ItemStack pStack, LivingEntity p_346168_) {
        return 72000;
    }

    /**
     * Called to trigger the item's "innate" right click behavior. To handle when this item is used on a Block, see {@link #onItemUse}.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        pPlayer.startUsingItem(pHand);
        return InteractionResultHolder.consume(itemstack);
    }

    /**
     * Return whether this item is repairable in an anvil.
     */
    @Override
    public boolean isValidRepairItem(ItemStack pToRepair, ItemStack pRepair) {
        return pRepair.is(ItemTags.PLANKS) || super.isValidRepairItem(pToRepair, pRepair);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.OFFHAND;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, net.neoforged.neoforge.common.ItemAbility itemAbility) {
        return net.neoforged.neoforge.common.ItemAbilities.DEFAULT_SHIELD_ACTIONS.contains(itemAbility);
    }
}
