package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.util.TimeUtil;

public class TickCommand {
    private static final float MAX_TICKRATE = 10000.0F;
    private static final String DEFAULT_TICKRATE = String.valueOf(20);

    public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
        pDispatcher.register(
            Commands.literal("tick")
                .requires(p_308941_ -> p_308941_.hasPermission(3))
                .then(Commands.literal("query").executes(p_308950_ -> tickQuery(p_308950_.getSource())))
                .then(
                    Commands.literal("rate")
                        .then(
                            Commands.argument("rate", FloatArgumentType.floatArg(1.0F, 10000.0F))
                                .suggests((p_308897_, p_308880_) -> SharedSuggestionProvider.suggest(new String[]{DEFAULT_TICKRATE}, p_308880_))
                                .executes(p_309119_ -> setTickingRate(p_309119_.getSource(), FloatArgumentType.getFloat(p_309119_, "rate")))
                        )
                )
                .then(
                    Commands.literal("step")
                        .executes(p_309496_ -> step(p_309496_.getSource(), 1))
                        .then(Commands.literal("stop").executes(p_309035_ -> stopStepping(p_309035_.getSource())))
                        .then(
                            Commands.argument("time", TimeArgument.time(1))
                                .suggests((p_309113_, p_309105_) -> SharedSuggestionProvider.suggest(new String[]{"1t", "1s"}, p_309105_))
                                .executes(p_308930_ -> step(p_308930_.getSource(), IntegerArgumentType.getInteger(p_308930_, "time")))
                        )
                )
                .then(
                    Commands.literal("sprint")
                        .then(Commands.literal("stop").executes(p_309190_ -> stopSprinting(p_309190_.getSource())))
                        .then(
                            Commands.argument("time", TimeArgument.time(1))
                                .suggests((p_308987_, p_309101_) -> SharedSuggestionProvider.suggest(new String[]{"60s", "1d", "3d"}, p_309101_))
                                .executes(p_308904_ -> sprint(p_308904_.getSource(), IntegerArgumentType.getInteger(p_308904_, "time")))
                        )
                )
                .then(Commands.literal("unfreeze").executes(p_309184_ -> setFreeze(p_309184_.getSource(), false)))
                .then(Commands.literal("freeze").executes(p_309070_ -> setFreeze(p_309070_.getSource(), true)))
        );
    }

    private static String nanosToMilisString(long pNanos) {
        return String.format("%.1f", (float)pNanos / (float)TimeUtil.NANOSECONDS_PER_MILLISECOND);
    }

    private static int setTickingRate(CommandSourceStack pSource, float pTickRate) {
        ServerTickRateManager servertickratemanager = pSource.getServer().tickRateManager();
        servertickratemanager.setTickRate(pTickRate);
        String s = String.format("%.1f", pTickRate);
        pSource.sendSuccess(() -> Component.translatable("commands.tick.rate.success", s), true);
        return (int)pTickRate;
    }

    private static int tickQuery(CommandSourceStack pSource) {
        ServerTickRateManager servertickratemanager = pSource.getServer().tickRateManager();
        String s = nanosToMilisString(pSource.getServer().getAverageTickTimeNanos());
        float f = servertickratemanager.tickrate();
        String s1 = String.format("%.1f", f);
        if (servertickratemanager.isSprinting()) {
            pSource.sendSuccess(() -> Component.translatable("commands.tick.status.sprinting"), false);
            pSource.sendSuccess(() -> Component.translatable("commands.tick.query.rate.sprinting", s1, s), false);
        } else {
            if (servertickratemanager.isFrozen()) {
                pSource.sendSuccess(() -> Component.translatable("commands.tick.status.frozen"), false);
            } else if (servertickratemanager.nanosecondsPerTick() < pSource.getServer().getAverageTickTimeNanos()) {
                pSource.sendSuccess(() -> Component.translatable("commands.tick.status.lagging"), false);
            } else {
                pSource.sendSuccess(() -> Component.translatable("commands.tick.status.running"), false);
            }

            String s2 = nanosToMilisString(servertickratemanager.nanosecondsPerTick());
            pSource.sendSuccess(() -> Component.translatable("commands.tick.query.rate.running", s1, s, s2), false);
        }

        long[] along = Arrays.copyOf(pSource.getServer().getTickTimesNanos(), pSource.getServer().getTickTimesNanos().length);
        Arrays.sort(along);
        String s3 = nanosToMilisString(along[along.length / 2]);
        String s4 = nanosToMilisString(along[(int)((double)along.length * 0.95)]);
        String s5 = nanosToMilisString(along[(int)((double)along.length * 0.99)]);
        pSource.sendSuccess(() -> Component.translatable("commands.tick.query.percentiles", s3, s4, s5, along.length), false);
        return (int)f;
    }

    private static int sprint(CommandSourceStack pSource, int pSprintTime) {
        boolean flag = pSource.getServer().tickRateManager().requestGameToSprint(pSprintTime);
        if (flag) {
            pSource.sendSuccess(() -> Component.translatable("commands.tick.sprint.stop.success"), true);
        }

        pSource.sendSuccess(() -> Component.translatable("commands.tick.status.sprinting"), true);
        return 1;
    }

    private static int setFreeze(CommandSourceStack pSource, boolean pFrozen) {
        ServerTickRateManager servertickratemanager = pSource.getServer().tickRateManager();
        if (pFrozen) {
            if (servertickratemanager.isSprinting()) {
                servertickratemanager.stopSprinting();
            }

            if (servertickratemanager.isSteppingForward()) {
                servertickratemanager.stopStepping();
            }
        }

        servertickratemanager.setFrozen(pFrozen);
        if (pFrozen) {
            pSource.sendSuccess(() -> Component.translatable("commands.tick.status.frozen"), true);
        } else {
            pSource.sendSuccess(() -> Component.translatable("commands.tick.status.running"), true);
        }

        return pFrozen ? 1 : 0;
    }

    private static int step(CommandSourceStack pSource, int pTicks) {
        ServerTickRateManager servertickratemanager = pSource.getServer().tickRateManager();
        boolean flag = servertickratemanager.stepGameIfPaused(pTicks);
        if (flag) {
            pSource.sendSuccess(() -> Component.translatable("commands.tick.step.success", pTicks), true);
        } else {
            pSource.sendFailure(Component.translatable("commands.tick.step.fail"));
        }

        return 1;
    }

    private static int stopStepping(CommandSourceStack pSource) {
        ServerTickRateManager servertickratemanager = pSource.getServer().tickRateManager();
        boolean flag = servertickratemanager.stopStepping();
        if (flag) {
            pSource.sendSuccess(() -> Component.translatable("commands.tick.step.stop.success"), true);
            return 1;
        } else {
            pSource.sendFailure(Component.translatable("commands.tick.step.stop.fail"));
            return 0;
        }
    }

    private static int stopSprinting(CommandSourceStack pSource) {
        ServerTickRateManager servertickratemanager = pSource.getServer().tickRateManager();
        boolean flag = servertickratemanager.stopSprinting();
        if (flag) {
            pSource.sendSuccess(() -> Component.translatable("commands.tick.sprint.stop.success"), true);
            return 1;
        } else {
            pSource.sendFailure(Component.translatable("commands.tick.sprint.stop.fail"));
            return 0;
        }
    }
}
