package net.minecraft.world.item;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class CompassItem extends Item {
    public CompassItem(Item.Properties pProperties) {
        super(pProperties);
    }

    @Nullable
    public static GlobalPos getSpawnPosition(Level pLevel) {
        return pLevel.dimensionType().natural() ? GlobalPos.of(pLevel.dimension(), pLevel.getSharedSpawnPos()) : null;
    }

    /**
     * Returns {@code true} if this item has an enchantment glint. By default, this returns <code>stack.isItemEnchanted()</code>, but other items can override it (for instance, written books always return true).
     *
     * Note that if you override this method, you generally want to also call the super version (on {@link Item}) to get the glint for enchanted items. Of course, that is unnecessary if the overwritten version always returns true.
     */
    @Override
    public boolean isFoil(ItemStack pStack) {
        return pStack.has(DataComponents.LODESTONE_TRACKER) || super.isFoil(pStack);
    }

    /**
     * Called each tick as long the item is in a player's inventory. Used by maps to check if it's in a player's hand and update its contents.
     */
    @Override
    public void inventoryTick(ItemStack pStack, Level pLevel, Entity pEntity, int pItemSlot, boolean pIsSelected) {
        if (pLevel instanceof ServerLevel serverlevel) {
            LodestoneTracker lodestonetracker = pStack.get(DataComponents.LODESTONE_TRACKER);
            if (lodestonetracker != null) {
                LodestoneTracker lodestonetracker1 = lodestonetracker.tick(serverlevel);
                if (lodestonetracker1 != lodestonetracker) {
                    pStack.set(DataComponents.LODESTONE_TRACKER, lodestonetracker1);
                }
            }
        }
    }

    /**
     * Called when this item is used when targeting a Block
     */
    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        BlockPos blockpos = pContext.getClickedPos();
        Level level = pContext.getLevel();
        if (!level.getBlockState(blockpos).is(Blocks.LODESTONE)) {
            return super.useOn(pContext);
        } else {
            level.playSound(null, blockpos, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
            Player player = pContext.getPlayer();
            ItemStack itemstack = pContext.getItemInHand();
            boolean flag = !player.hasInfiniteMaterials() && itemstack.getCount() == 1;
            LodestoneTracker lodestonetracker = new LodestoneTracker(Optional.of(GlobalPos.of(level.dimension(), blockpos)), true);
            if (flag) {
                itemstack.set(DataComponents.LODESTONE_TRACKER, lodestonetracker);
            } else {
                ItemStack itemstack1 = itemstack.transmuteCopy(Items.COMPASS, 1);
                itemstack.consume(1, player);
                itemstack1.set(DataComponents.LODESTONE_TRACKER, lodestonetracker);
                if (!player.getInventory().add(itemstack1)) {
                    player.drop(itemstack1, false);
                }
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    /**
     * Returns the unlocalized name of this item. This version accepts an ItemStack so different stacks can have different names based on their damage or NBT.
     */
    @Override
    public String getDescriptionId(ItemStack pStack) {
        return pStack.has(DataComponents.LODESTONE_TRACKER) ? "item.minecraft.lodestone_compass" : super.getDescriptionId(pStack);
    }
}
