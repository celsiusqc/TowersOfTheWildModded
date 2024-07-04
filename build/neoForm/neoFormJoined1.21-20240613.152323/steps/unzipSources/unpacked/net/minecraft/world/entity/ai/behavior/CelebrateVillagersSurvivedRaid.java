package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;

public class CelebrateVillagersSurvivedRaid extends Behavior<Villager> {
    @Nullable
    private Raid currentRaid;

    public CelebrateVillagersSurvivedRaid(int pMinDuration, int pMaxDuration) {
        super(ImmutableMap.of(), pMinDuration, pMaxDuration);
    }

    protected boolean checkExtraStartConditions(ServerLevel pLevel, Villager pOwner) {
        BlockPos blockpos = pOwner.blockPosition();
        this.currentRaid = pLevel.getRaidAt(blockpos);
        return this.currentRaid != null && this.currentRaid.isVictory() && MoveToSkySeeingSpot.hasNoBlocksAbove(pLevel, pOwner, blockpos);
    }

    protected boolean canStillUse(ServerLevel pLevel, Villager pEntity, long pGameTime) {
        return this.currentRaid != null && !this.currentRaid.isStopped();
    }

    protected void stop(ServerLevel pLevel, Villager pEntity, long pGameTime) {
        this.currentRaid = null;
        pEntity.getBrain().updateActivityFromSchedule(pLevel.getDayTime(), pLevel.getGameTime());
    }

    protected void tick(ServerLevel pLevel, Villager pOwner, long pGameTime) {
        RandomSource randomsource = pOwner.getRandom();
        if (randomsource.nextInt(100) == 0) {
            pOwner.playCelebrateSound();
        }

        if (randomsource.nextInt(200) == 0 && MoveToSkySeeingSpot.hasNoBlocksAbove(pLevel, pOwner, pOwner.blockPosition())) {
            DyeColor dyecolor = Util.getRandom(DyeColor.values(), randomsource);
            int i = randomsource.nextInt(3);
            ItemStack itemstack = this.getFirework(dyecolor, i);
            FireworkRocketEntity fireworkrocketentity = new FireworkRocketEntity(
                pOwner.level(), pOwner, pOwner.getX(), pOwner.getEyeY(), pOwner.getZ(), itemstack
            );
            pOwner.level().addFreshEntity(fireworkrocketentity);
        }
    }

    private ItemStack getFirework(DyeColor pColor, int pFlightTime) {
        ItemStack itemstack = new ItemStack(Items.FIREWORK_ROCKET);
        itemstack.set(
            DataComponents.FIREWORKS,
            new Fireworks(
                (byte)pFlightTime,
                List.of(new FireworkExplosion(FireworkExplosion.Shape.BURST, IntList.of(pColor.getFireworkColor()), IntList.of(), false, false))
            )
        );
        return itemstack;
    }
}
