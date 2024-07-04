package net.minecraft.world.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

public class NameTagItem extends Item {
    public NameTagItem(Item.Properties pProperties) {
        super(pProperties);
    }

    /**
     * Try interacting with given entity. Return {@code InteractionResult.PASS} if nothing should happen.
     */
    @Override
    public InteractionResult interactLivingEntity(ItemStack pStack, Player pPlayer, LivingEntity pTarget, InteractionHand pHand) {
        Component component = pStack.get(DataComponents.CUSTOM_NAME);
        if (component != null && !(pTarget instanceof Player)) {
            if (!pPlayer.level().isClientSide && pTarget.isAlive()) {
                pTarget.setCustomName(component);
                if (pTarget instanceof Mob mob) {
                    mob.setPersistenceRequired();
                }

                pStack.shrink(1);
            }

            return InteractionResult.sidedSuccess(pPlayer.level().isClientSide);
        } else {
            return InteractionResult.PASS;
        }
    }
}
