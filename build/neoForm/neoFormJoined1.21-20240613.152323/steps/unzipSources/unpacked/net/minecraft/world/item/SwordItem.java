package net.minecraft.world.item;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class SwordItem extends TieredItem {
    public SwordItem(Tier pTier, Item.Properties pProperties) {
        super(pTier, pProperties.component(DataComponents.TOOL, createToolProperties()));
    }

    /**
     * Neo: Allow modded Swords to set exactly what Tool data component to use for their sword.
     */
    public SwordItem(Tier pTier, Item.Properties pProperties, Tool toolComponentData) {
        super(pTier, pProperties.component(DataComponents.TOOL, toolComponentData));
    }

    public static Tool createToolProperties() {
        return new Tool(List.of(Tool.Rule.minesAndDrops(List.of(Blocks.COBWEB), 15.0F), Tool.Rule.overrideSpeed(BlockTags.SWORD_EFFICIENT, 1.5F)), 1.0F, 2);
    }

    public static ItemAttributeModifiers createAttributes(Tier pTier, int pAttackDamage, float pAttackSpeed) {
        return createAttributes(pTier, (float)pAttackDamage, pAttackSpeed);
    }

    /**
     * Neo: Method overload to allow giving a float for damage instead of an int.
     */
    public static ItemAttributeModifiers createAttributes(Tier p_330371_, float p_331976_, float p_332104_) {
        return ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                    BASE_ATTACK_DAMAGE_ID, (double)((float)p_331976_ + p_330371_.getAttackDamageBonus()), AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            )
            .add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(BASE_ATTACK_SPEED_ID, (double)p_332104_, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND
            )
            .build();
    }

    @Override
    public boolean canAttackBlock(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
        return !pPlayer.isCreative();
    }

    /**
     * Current implementations of this method in child classes do not use the entry argument beside ev. They just raise the damage on the stack.
     */
    @Override
    public boolean hurtEnemy(ItemStack pStack, LivingEntity pTarget, LivingEntity pAttacker) {
        return true;
    }

    @Override
    public void postHurtEnemy(ItemStack p_345553_, LivingEntity p_345771_, LivingEntity p_346282_) {
        p_345553_.hurtAndBreak(1, p_346282_, EquipmentSlot.MAINHAND);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, net.neoforged.neoforge.common.ItemAbility itemAbility) {
        return net.neoforged.neoforge.common.ItemAbilities.DEFAULT_SWORD_ACTIONS.contains(itemAbility);
    }
}
