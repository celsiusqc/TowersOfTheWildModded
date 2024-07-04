package net.minecraft.world.item;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

public class LeadItem extends Item {
    public LeadItem(Item.Properties pProperties) {
        super(pProperties);
    }

    /**
     * Called when this item is used when targeting a Block
     */
    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        Level level = pContext.getLevel();
        BlockPos blockpos = pContext.getClickedPos();
        BlockState blockstate = level.getBlockState(blockpos);
        if (blockstate.is(BlockTags.FENCES)) {
            Player player = pContext.getPlayer();
            if (!level.isClientSide && player != null) {
                bindPlayerMobs(player, level, blockpos);
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        } else {
            return InteractionResult.PASS;
        }
    }

    public static InteractionResult bindPlayerMobs(Player pPlayer, Level pLevel, BlockPos pPos) {
        LeashFenceKnotEntity leashfenceknotentity = null;
        List<Leashable> list = leashableInArea(pLevel, pPos, p_353025_ -> p_353025_.getLeashHolder() == pPlayer);

        for (Leashable leashable : list) {
            if (leashfenceknotentity == null) {
                leashfenceknotentity = LeashFenceKnotEntity.getOrCreateKnot(pLevel, pPos);
                leashfenceknotentity.playPlacementSound();
            }

            leashable.setLeashedTo(leashfenceknotentity, true);
        }

        if (!list.isEmpty()) {
            pLevel.gameEvent(GameEvent.BLOCK_ATTACH, pPos, GameEvent.Context.of(pPlayer));
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }

    public static List<Leashable> leashableInArea(Level p_353033_, BlockPos p_353031_, Predicate<Leashable> p_353062_) {
        double d0 = 7.0;
        int i = p_353031_.getX();
        int j = p_353031_.getY();
        int k = p_353031_.getZ();
        AABB aabb = new AABB((double)i - 7.0, (double)j - 7.0, (double)k - 7.0, (double)i + 7.0, (double)j + 7.0, (double)k + 7.0);
        return p_353033_.getEntitiesOfClass(Entity.class, aabb, p_353023_ -> {
            if (p_353023_ instanceof Leashable leashable && p_353062_.test(leashable)) {
                return true;
            }

            return false;
        }).stream().map(Leashable.class::cast).toList();
    }
}
