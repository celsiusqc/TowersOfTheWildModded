package net.minecraft.world.item;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.level.Level;

public class SuspiciousStewItem extends Item {
    public static final int DEFAULT_DURATION = 160;

    public SuspiciousStewItem(Item.Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
        if (pTooltipFlag.isCreative()) {
            List<MobEffectInstance> list = new ArrayList<>();
            SuspiciousStewEffects suspicioussteweffects = pStack.getOrDefault(DataComponents.SUSPICIOUS_STEW_EFFECTS, SuspiciousStewEffects.EMPTY);

            for (SuspiciousStewEffects.Entry suspicioussteweffects$entry : suspicioussteweffects.effects()) {
                list.add(suspicioussteweffects$entry.createEffectInstance());
            }

            PotionContents.addPotionTooltip(list, pTooltipComponents::add, 1.0F, pContext.tickRate());
        }
    }

    /**
     * Called when the player finishes using this Item (E.g. finishes eating.). Not called when the player stops using the Item before the action is complete.
     */
    @Override
    public ItemStack finishUsingItem(ItemStack pStack, Level pLevel, LivingEntity pEntityLiving) {
        SuspiciousStewEffects suspicioussteweffects = pStack.getOrDefault(DataComponents.SUSPICIOUS_STEW_EFFECTS, SuspiciousStewEffects.EMPTY);

        for (SuspiciousStewEffects.Entry suspicioussteweffects$entry : suspicioussteweffects.effects()) {
            pEntityLiving.addEffect(suspicioussteweffects$entry.createEffectInstance());
        }

        return super.finishUsingItem(pStack, pLevel, pEntityLiving);
    }
}
