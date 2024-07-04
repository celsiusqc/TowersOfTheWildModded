package net.minecraft.world.level.storage;

import java.util.Locale;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.CrashReportCategory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.timers.TimerQueue;

public interface ServerLevelData extends WritableLevelData {
    String getLevelName();

    /**
     * Sets whether it is thundering or not.
     */
    void setThundering(boolean pThundering);

    int getRainTime();

    /**
     * Sets the number of ticks until rain.
     */
    void setRainTime(int pTime);

    /**
     * Defines the number of ticks until next thunderbolt.
     */
    void setThunderTime(int pTime);

    int getThunderTime();

    @Override
    default void fillCrashReportCategory(CrashReportCategory pCrashReportCategory, LevelHeightAccessor pLevel) {
        WritableLevelData.super.fillCrashReportCategory(pCrashReportCategory, pLevel);
        pCrashReportCategory.setDetail("Level name", this::getLevelName);
        pCrashReportCategory.setDetail(
            "Level game mode",
            () -> String.format(
                    Locale.ROOT,
                    "Game mode: %s (ID %d). Hardcore: %b. Commands: %b",
                    this.getGameType().getName(),
                    this.getGameType().getId(),
                    this.isHardcore(),
                    this.isAllowCommands()
                )
        );
        pCrashReportCategory.setDetail(
            "Level weather",
            () -> String.format(
                    Locale.ROOT,
                    "Rain time: %d (now: %b), thunder time: %d (now: %b)",
                    this.getRainTime(),
                    this.isRaining(),
                    this.getThunderTime(),
                    this.isThundering()
                )
        );
    }

    int getClearWeatherTime();

    void setClearWeatherTime(int pTime);

    int getWanderingTraderSpawnDelay();

    void setWanderingTraderSpawnDelay(int pDelay);

    int getWanderingTraderSpawnChance();

    void setWanderingTraderSpawnChance(int pChance);

    @Nullable
    UUID getWanderingTraderId();

    void setWanderingTraderId(UUID pId);

    GameType getGameType();

    void setWorldBorder(WorldBorder.Settings pSerializer);

    WorldBorder.Settings getWorldBorder();

    boolean isInitialized();

    /**
     * Sets the initialization status of the World.
     */
    void setInitialized(boolean pInitialized);

    boolean isAllowCommands();

    void setGameType(GameType pType);

    TimerQueue<MinecraftServer> getScheduledEvents();

    void setGameTime(long pTime);

    /**
     * Set current world time
     */
    void setDayTime(long pTime);
}
