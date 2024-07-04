package com.mojang.realmsclient.gui.screens;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.exception.RealmsDefaultUncaughtExceptionHandler;
import com.mojang.realmsclient.util.task.LongRunningTask;
import java.time.Duration;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.LoadingDotsWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.realms.RepeatedNarrator;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsLongRunningMcoTaskScreen extends RealmsScreen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final RepeatedNarrator REPEATED_NARRATOR = new RepeatedNarrator(Duration.ofSeconds(5L));
    private final List<LongRunningTask> queuedTasks;
    private final Screen lastScreen;
    private final LinearLayout layout = LinearLayout.vertical();
    private volatile Component title;
    @Nullable
    private LoadingDotsWidget loadingDotsWidget;

    public RealmsLongRunningMcoTaskScreen(Screen pLastScreen, LongRunningTask... pQueuedTasks) {
        super(GameNarrator.NO_TITLE);
        this.lastScreen = pLastScreen;
        this.queuedTasks = List.of(pQueuedTasks);
        if (this.queuedTasks.isEmpty()) {
            throw new IllegalArgumentException("No tasks added");
        } else {
            this.title = this.queuedTasks.get(0).getTitle();
            Runnable runnable = () -> {
                for (LongRunningTask longrunningtask : pQueuedTasks) {
                    this.setTitle(longrunningtask.getTitle());
                    if (longrunningtask.aborted()) {
                        break;
                    }

                    longrunningtask.run();
                    if (longrunningtask.aborted()) {
                        return;
                    }
                }
            };
            Thread thread = new Thread(runnable, "Realms-long-running-task");
            thread.setUncaughtExceptionHandler(new RealmsDefaultUncaughtExceptionHandler(LOGGER));
            thread.start();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.loadingDotsWidget != null) {
            REPEATED_NARRATOR.narrate(this.minecraft.getNarrator(), this.loadingDotsWidget.getMessage());
        }
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (pKeyCode == 256) {
            this.cancel();
            return true;
        } else {
            return super.keyPressed(pKeyCode, pScanCode, pModifiers);
        }
    }

    @Override
    public void init() {
        this.layout.defaultCellSetting().alignHorizontallyCenter();
        this.loadingDotsWidget = new LoadingDotsWidget(this.font, this.title);
        this.layout.addChild(this.loadingDotsWidget, p_300631_ -> p_300631_.paddingBottom(30));
        this.layout.addChild(Button.builder(CommonComponents.GUI_CANCEL, p_300632_ -> this.cancel()).build());
        this.layout.visitWidgets(p_321340_ -> {
            AbstractWidget abstractwidget = this.addRenderableWidget(p_321340_);
        });
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        FrameLayout.centerInRectangle(this.layout, this.getRectangle());
    }

    protected void cancel() {
        for (LongRunningTask longrunningtask : this.queuedTasks) {
            longrunningtask.abortTask();
        }

        this.minecraft.setScreen(this.lastScreen);
    }

    public void setTitle(Component pTitle) {
        if (this.loadingDotsWidget != null) {
            this.loadingDotsWidget.setMessage(pTitle);
        }

        this.title = pTitle;
    }
}
