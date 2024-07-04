package net.minecraft.world.item;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;

public class SaddleItem extends Item {
    public SaddleItem(Item.Properties pProperties) {
        super(pProperties);
    }

    /**
     * Try interacting with given entity. Return {@code InteractionResult.PASS} if nothing should happen.
     */
    @Override
    public InteractionResult interactLivingEntity(ItemStack pStack, Player pPlayer, LivingEntity pTarget, InteractionHand pHand) {
        if (pTarget instanceof Saddleable saddleable && pTarget.isAlive() && !saddleable.isSaddled() && saddleable.isSaddleable()) {
            if (!pPlayer.level().isClientSide) {
                saddleable.equipSaddle(pStack.split(1), SoundSource.NEUTRAL);
                pTarget.level().gameEvent(pTarget, GameEvent.EQUIP, pTarget.position());
            }

            return InteractionResult.sidedSuccess(pPlayer.level().isClientSide);
        }

        return InteractionResult.PASS;
    }
}
