package com.mojang.realmsclient.gui.screens;

import com.mojang.realmsclient.util.task.LongRunningTask;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsLongRunningMcoTickTaskScreen extends RealmsLongRunningMcoTaskScreen {
    private final LongRunningTask task;

    public RealmsLongRunningMcoTickTaskScreen(Screen pLastScreen, LongRunningTask pTask) {
        super(pLastScreen, pTask);
        this.task = pTask;
    }

    @Override
    public void tick() {
        super.tick();
        this.task.tick();
    }

    @Override
    protected void cancel() {
        this.task.abortTask();
        super.cancel();
    }
}
