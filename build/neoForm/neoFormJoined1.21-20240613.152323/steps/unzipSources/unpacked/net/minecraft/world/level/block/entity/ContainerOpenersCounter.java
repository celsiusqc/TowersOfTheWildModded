package net.minecraft.world.level.block.entity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

public abstract class ContainerOpenersCounter {
    private static final int CHECK_TICK_DELAY = 5;
    private int openCount;
    private double maxInteractionRange;

    protected abstract void onOpen(Level pLevel, BlockPos pPos, BlockState pState);

    protected abstract void onClose(Level pLevel, BlockPos pPos, BlockState pState);

    protected abstract void openerCountChanged(Level pLevel, BlockPos pPos, BlockState pState, int pCount, int pOpenCount);

    protected abstract boolean isOwnContainer(Player p_155451_);

    public void incrementOpeners(Player pPlayer, Level pLevel, BlockPos pPos, BlockState pState) {
        int i = this.openCount++;
        if (i == 0) {
            this.onOpen(pLevel, pPos, pState);
            pLevel.gameEvent(pPlayer, GameEvent.CONTAINER_OPEN, pPos);
            scheduleRecheck(pLevel, pPos, pState);
        }

        this.openerCountChanged(pLevel, pPos, pState, i, this.openCount);
        this.maxInteractionRange = Math.max(pPlayer.blockInteractionRange(), this.maxInteractionRange);
    }

    public void decrementOpeners(Player pPlayer, Level pLevel, BlockPos pPos, BlockState pState) {
        int i = this.openCount--;
        if (this.openCount == 0) {
            this.onClose(pLevel, pPos, pState);
            pLevel.gameEvent(pPlayer, GameEvent.CONTAINER_CLOSE, pPos);
            this.maxInteractionRange = 0.0;
        }

        this.openerCountChanged(pLevel, pPos, pState, i, this.openCount);
    }

    private List<Player> getPlayersWithContainerOpen(Level pLevel, BlockPos pPos) {
        double d0 = this.maxInteractionRange + 4.0;
        AABB aabb = new AABB(pPos).inflate(d0);
        return pLevel.getEntities(EntityTypeTest.forClass(Player.class), aabb, this::isOwnContainer);
    }

    public void recheckOpeners(Level pLevel, BlockPos pPos, BlockState pState) {
        List<Player> list = this.getPlayersWithContainerOpen(pLevel, pPos);
        this.maxInteractionRange = 0.0;

        for (Player player : list) {
            this.maxInteractionRange = Math.max(player.blockInteractionRange(), this.maxInteractionRange);
        }

        int i = list.size();
        int j = this.openCount;
        if (j != i) {
            boolean flag = i != 0;
            boolean flag1 = j != 0;
            if (flag && !flag1) {
                this.onOpen(pLevel, pPos, pState);
                pLevel.gameEvent(null, GameEvent.CONTAINER_OPEN, pPos);
            } else if (!flag) {
                this.onClose(pLevel, pPos, pState);
                pLevel.gameEvent(null, GameEvent.CONTAINER_CLOSE, pPos);
            }

            this.openCount = i;
        }

        this.openerCountChanged(pLevel, pPos, pState, j, i);
        if (i > 0) {
            scheduleRecheck(pLevel, pPos, pState);
        }
    }

    public int getOpenerCount() {
        return this.openCount;
    }

    private static void scheduleRecheck(Level pLevel, BlockPos pPos, BlockState pState) {
        pLevel.scheduleTick(pPos, pState.getBlock(), 5);
    }
}
