package net.minecraft.world.level;

import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public abstract class BaseCommandBlock implements CommandSource {
    /**
     * The formatting for the timestamp on commands run.
     */
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final Component DEFAULT_NAME = Component.literal("@");
    private long lastExecution = -1L;
    private boolean updateLastExecution = true;
    /**
     * The number of successful commands run. (used for redstone output)
     */
    private int successCount;
    private boolean trackOutput = true;
    /**
     * The previously run command.
     */
    @Nullable
    private Component lastOutput;
    /**
     * The command stored in the command block.
     */
    private String command = "";
    @Nullable
    private Component customName;

    public int getSuccessCount() {
        return this.successCount;
    }

    public void setSuccessCount(int pSuccessCount) {
        this.successCount = pSuccessCount;
    }

    public Component getLastOutput() {
        return this.lastOutput == null ? CommonComponents.EMPTY : this.lastOutput;
    }

    public CompoundTag save(CompoundTag pTag, HolderLookup.Provider pLevelRegistry) {
        pTag.putString("Command", this.command);
        pTag.putInt("SuccessCount", this.successCount);
        if (this.customName != null) {
            pTag.putString("CustomName", Component.Serializer.toJson(this.customName, pLevelRegistry));
        }

        pTag.putBoolean("TrackOutput", this.trackOutput);
        if (this.lastOutput != null && this.trackOutput) {
            pTag.putString("LastOutput", Component.Serializer.toJson(this.lastOutput, pLevelRegistry));
        }

        pTag.putBoolean("UpdateLastExecution", this.updateLastExecution);
        if (this.updateLastExecution && this.lastExecution > 0L) {
            pTag.putLong("LastExecution", this.lastExecution);
        }

        return pTag;
    }

    public void load(CompoundTag pTag, HolderLookup.Provider pLevelRegistry) {
        this.command = pTag.getString("Command");
        this.successCount = pTag.getInt("SuccessCount");
        if (pTag.contains("CustomName", 8)) {
            this.setCustomName(BlockEntity.parseCustomNameSafe(pTag.getString("CustomName"), pLevelRegistry));
        } else {
            this.setCustomName(null);
        }

        if (pTag.contains("TrackOutput", 1)) {
            this.trackOutput = pTag.getBoolean("TrackOutput");
        }

        if (pTag.contains("LastOutput", 8) && this.trackOutput) {
            try {
                this.lastOutput = Component.Serializer.fromJson(pTag.getString("LastOutput"), pLevelRegistry);
            } catch (Throwable throwable) {
                this.lastOutput = Component.literal(throwable.getMessage());
            }
        } else {
            this.lastOutput = null;
        }

        if (pTag.contains("UpdateLastExecution")) {
            this.updateLastExecution = pTag.getBoolean("UpdateLastExecution");
        }

        if (this.updateLastExecution && pTag.contains("LastExecution")) {
            this.lastExecution = pTag.getLong("LastExecution");
        } else {
            this.lastExecution = -1L;
        }
    }

    /**
     * Sets the command.
     */
    public void setCommand(String pCommand) {
        this.command = pCommand;
        this.successCount = 0;
    }

    public String getCommand() {
        return this.command;
    }

    public boolean performCommand(Level pLevel) {
        if (pLevel.isClientSide || pLevel.getGameTime() == this.lastExecution) {
            return false;
        } else if ("Searge".equalsIgnoreCase(this.command)) {
            this.lastOutput = Component.literal("#itzlipofutzli");
            this.successCount = 1;
            return true;
        } else {
            this.successCount = 0;
            MinecraftServer minecraftserver = this.getLevel().getServer();
            if (minecraftserver.isCommandBlockEnabled() && !StringUtil.isNullOrEmpty(this.command)) {
                try {
                    this.lastOutput = null;
                    CommandSourceStack commandsourcestack = this.createCommandSourceStack().withCallback((p_45418_, p_45419_) -> {
                        if (p_45418_) {
                            this.successCount++;
                        }
                    });
                    minecraftserver.getCommands().performPrefixedCommand(commandsourcestack, this.command);
                } catch (Throwable throwable) {
                    CrashReport crashreport = CrashReport.forThrowable(throwable, "Executing command block");
                    CrashReportCategory crashreportcategory = crashreport.addCategory("Command to be executed");
                    crashreportcategory.setDetail("Command", this::getCommand);
                    crashreportcategory.setDetail("Name", () -> this.getName().getString());
                    throw new ReportedException(crashreport);
                }
            }

            if (this.updateLastExecution) {
                this.lastExecution = pLevel.getGameTime();
            } else {
                this.lastExecution = -1L;
            }

            return true;
        }
    }

    public Component getName() {
        return this.customName != null ? this.customName : DEFAULT_NAME;
    }

    @Nullable
    public Component getCustomName() {
        return this.customName;
    }

    public void setCustomName(@Nullable Component pCustomName) {
        this.customName = pCustomName;
    }

    @Override
    public void sendSystemMessage(Component pComponent) {
        if (this.trackOutput) {
            this.lastOutput = Component.literal("[" + TIME_FORMAT.format(new Date()) + "] ").append(pComponent);
            this.onUpdated();
        }
    }

    public abstract ServerLevel getLevel();

    public abstract void onUpdated();

    public void setLastOutput(@Nullable Component pLastOutputMessage) {
        this.lastOutput = pLastOutputMessage;
    }

    public void setTrackOutput(boolean pShouldTrackOutput) {
        this.trackOutput = pShouldTrackOutput;
    }

    public boolean isTrackOutput() {
        return this.trackOutput;
    }

    public InteractionResult usedBy(Player pPlayer) {
        if (!pPlayer.canUseGameMasterBlocks()) {
            return InteractionResult.PASS;
        } else {
            if (pPlayer.getCommandSenderWorld().isClientSide) {
                pPlayer.openMinecartCommandBlock(this);
            }

            return InteractionResult.sidedSuccess(pPlayer.level().isClientSide);
        }
    }

    public abstract Vec3 getPosition();

    public abstract CommandSourceStack createCommandSourceStack();

    @Override
    public boolean acceptsSuccess() {
        return this.getLevel().getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK) && this.trackOutput;
    }

    @Override
    public boolean acceptsFailure() {
        return this.trackOutput;
    }

    @Override
    public boolean shouldInformAdmins() {
        return this.getLevel().getGameRules().getBoolean(GameRules.RULE_COMMANDBLOCKOUTPUT);
    }

    public abstract boolean isValid();
}
