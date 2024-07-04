package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.commands.arguments.ObjectiveCriteriaArgument;
import net.minecraft.commands.arguments.OperationArgument;
import net.minecraft.commands.arguments.ScoreHolderArgument;
import net.minecraft.commands.arguments.ScoreboardSlotArgument;
import net.minecraft.commands.arguments.StyleArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.chat.numbers.FixedFormat;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class ScoreboardCommand {
    private static final SimpleCommandExceptionType ERROR_OBJECTIVE_ALREADY_EXISTS = new SimpleCommandExceptionType(
        Component.translatable("commands.scoreboard.objectives.add.duplicate")
    );
    private static final SimpleCommandExceptionType ERROR_DISPLAY_SLOT_ALREADY_EMPTY = new SimpleCommandExceptionType(
        Component.translatable("commands.scoreboard.objectives.display.alreadyEmpty")
    );
    private static final SimpleCommandExceptionType ERROR_DISPLAY_SLOT_ALREADY_SET = new SimpleCommandExceptionType(
        Component.translatable("commands.scoreboard.objectives.display.alreadySet")
    );
    private static final SimpleCommandExceptionType ERROR_TRIGGER_ALREADY_ENABLED = new SimpleCommandExceptionType(
        Component.translatable("commands.scoreboard.players.enable.failed")
    );
    private static final SimpleCommandExceptionType ERROR_NOT_TRIGGER = new SimpleCommandExceptionType(
        Component.translatable("commands.scoreboard.players.enable.invalid")
    );
    private static final Dynamic2CommandExceptionType ERROR_NO_VALUE = new Dynamic2CommandExceptionType(
        (p_304296_, p_304297_) -> Component.translatableEscape("commands.scoreboard.players.get.null", p_304296_, p_304297_)
    );

    public static void register(CommandDispatcher<CommandSourceStack> pDispatcher, CommandBuildContext pContext) {
        pDispatcher.register(
            Commands.literal("scoreboard")
                .requires(p_138552_ -> p_138552_.hasPermission(2))
                .then(
                    Commands.literal("objectives")
                        .then(Commands.literal("list").executes(p_138585_ -> listObjectives(p_138585_.getSource())))
                        .then(
                            Commands.literal("add")
                                .then(
                                    Commands.argument("objective", StringArgumentType.word())
                                        .then(
                                            Commands.argument("criteria", ObjectiveCriteriaArgument.criteria())
                                                .executes(
                                                    p_138583_ -> addObjective(
                                                            p_138583_.getSource(),
                                                            StringArgumentType.getString(p_138583_, "objective"),
                                                            ObjectiveCriteriaArgument.getCriteria(p_138583_, "criteria"),
                                                            Component.literal(StringArgumentType.getString(p_138583_, "objective"))
                                                        )
                                                )
                                                .then(
                                                    Commands.argument("displayName", ComponentArgument.textComponent(pContext))
                                                        .executes(
                                                            p_138581_ -> addObjective(
                                                                    p_138581_.getSource(),
                                                                    StringArgumentType.getString(p_138581_, "objective"),
                                                                    ObjectiveCriteriaArgument.getCriteria(p_138581_, "criteria"),
                                                                    ComponentArgument.getComponent(p_138581_, "displayName")
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(
                            Commands.literal("modify")
                                .then(
                                    Commands.argument("objective", ObjectiveArgument.objective())
                                        .then(
                                            Commands.literal("displayname")
                                                .then(
                                                    Commands.argument("displayName", ComponentArgument.textComponent(pContext))
                                                        .executes(
                                                            p_138579_ -> setDisplayName(
                                                                    p_138579_.getSource(),
                                                                    ObjectiveArgument.getObjective(p_138579_, "objective"),
                                                                    ComponentArgument.getComponent(p_138579_, "displayName")
                                                                )
                                                        )
                                                )
                                        )
                                        .then(createRenderTypeModify())
                                        .then(
                                            Commands.literal("displayautoupdate")
                                                .then(
                                                    Commands.argument("value", BoolArgumentType.bool())
                                                        .executes(
                                                            p_313527_ -> setDisplayAutoUpdate(
                                                                    p_313527_.getSource(),
                                                                    ObjectiveArgument.getObjective(p_313527_, "objective"),
                                                                    BoolArgumentType.getBool(p_313527_, "value")
                                                                )
                                                        )
                                                )
                                        )
                                        .then(
                                            addNumberFormats(
                                                pContext,
                                                Commands.literal("numberformat"),
                                                (p_313531_, p_313532_) -> setObjectiveFormat(
                                                        p_313531_.getSource(), ObjectiveArgument.getObjective(p_313531_, "objective"), p_313532_
                                                    )
                                            )
                                        )
                                )
                        )
                        .then(
                            Commands.literal("remove")
                                .then(
                                    Commands.argument("objective", ObjectiveArgument.objective())
                                        .executes(p_138577_ -> removeObjective(p_138577_.getSource(), ObjectiveArgument.getObjective(p_138577_, "objective")))
                                )
                        )
                        .then(
                            Commands.literal("setdisplay")
                                .then(
                                    Commands.argument("slot", ScoreboardSlotArgument.displaySlot())
                                        .executes(
                                            p_293788_ -> clearDisplaySlot(p_293788_.getSource(), ScoreboardSlotArgument.getDisplaySlot(p_293788_, "slot"))
                                        )
                                        .then(
                                            Commands.argument("objective", ObjectiveArgument.objective())
                                                .executes(
                                                    p_293785_ -> setDisplaySlot(
                                                            p_293785_.getSource(),
                                                            ScoreboardSlotArgument.getDisplaySlot(p_293785_, "slot"),
                                                            ObjectiveArgument.getObjective(p_293785_, "objective")
                                                        )
                                                )
                                        )
                                )
                        )
                )
                .then(
                    Commands.literal("players")
                        .then(
                            Commands.literal("list")
                                .executes(p_138571_ -> listTrackedPlayers(p_138571_.getSource()))
                                .then(
                                    Commands.argument("target", ScoreHolderArgument.scoreHolder())
                                        .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                        .executes(p_313550_ -> listTrackedPlayerScores(p_313550_.getSource(), ScoreHolderArgument.getName(p_313550_, "target")))
                                )
                        )
                        .then(
                            Commands.literal("set")
                                .then(
                                    Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                                        .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                        .then(
                                            Commands.argument("objective", ObjectiveArgument.objective())
                                                .then(
                                                    Commands.argument("score", IntegerArgumentType.integer())
                                                        .executes(
                                                            p_138567_ -> setScore(
                                                                    p_138567_.getSource(),
                                                                    ScoreHolderArgument.getNamesWithDefaultWildcard(p_138567_, "targets"),
                                                                    ObjectiveArgument.getWritableObjective(p_138567_, "objective"),
                                                                    IntegerArgumentType.getInteger(p_138567_, "score")
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(
                            Commands.literal("get")
                                .then(
                                    Commands.argument("target", ScoreHolderArgument.scoreHolder())
                                        .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                        .then(
                                            Commands.argument("objective", ObjectiveArgument.objective())
                                                .executes(
                                                    p_313543_ -> getScore(
                                                            p_313543_.getSource(),
                                                            ScoreHolderArgument.getName(p_313543_, "target"),
                                                            ObjectiveArgument.getObjective(p_313543_, "objective")
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(
                            Commands.literal("add")
                                .then(
                                    Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                                        .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                        .then(
                                            Commands.argument("objective", ObjectiveArgument.objective())
                                                .then(
                                                    Commands.argument("score", IntegerArgumentType.integer(0))
                                                        .executes(
                                                            p_138563_ -> addScore(
                                                                    p_138563_.getSource(),
                                                                    ScoreHolderArgument.getNamesWithDefaultWildcard(p_138563_, "targets"),
                                                                    ObjectiveArgument.getWritableObjective(p_138563_, "objective"),
                                                                    IntegerArgumentType.getInteger(p_138563_, "score")
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(
                            Commands.literal("remove")
                                .then(
                                    Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                                        .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                        .then(
                                            Commands.argument("objective", ObjectiveArgument.objective())
                                                .then(
                                                    Commands.argument("score", IntegerArgumentType.integer(0))
                                                        .executes(
                                                            p_138561_ -> removeScore(
                                                                    p_138561_.getSource(),
                                                                    ScoreHolderArgument.getNamesWithDefaultWildcard(p_138561_, "targets"),
                                                                    ObjectiveArgument.getWritableObjective(p_138561_, "objective"),
                                                                    IntegerArgumentType.getInteger(p_138561_, "score")
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(
                            Commands.literal("reset")
                                .then(
                                    Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                                        .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                        .executes(
                                            p_138559_ -> resetScores(
                                                    p_138559_.getSource(), ScoreHolderArgument.getNamesWithDefaultWildcard(p_138559_, "targets")
                                                )
                                        )
                                        .then(
                                            Commands.argument("objective", ObjectiveArgument.objective())
                                                .executes(
                                                    p_138550_ -> resetScore(
                                                            p_138550_.getSource(),
                                                            ScoreHolderArgument.getNamesWithDefaultWildcard(p_138550_, "targets"),
                                                            ObjectiveArgument.getObjective(p_138550_, "objective")
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(
                            Commands.literal("enable")
                                .then(
                                    Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                                        .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                        .then(
                                            Commands.argument("objective", ObjectiveArgument.objective())
                                                .suggests(
                                                    (p_138473_, p_138474_) -> suggestTriggers(
                                                            p_138473_.getSource(),
                                                            ScoreHolderArgument.getNamesWithDefaultWildcard(p_138473_, "targets"),
                                                            p_138474_
                                                        )
                                                )
                                                .executes(
                                                    p_138537_ -> enableTrigger(
                                                            p_138537_.getSource(),
                                                            ScoreHolderArgument.getNamesWithDefaultWildcard(p_138537_, "targets"),
                                                            ObjectiveArgument.getObjective(p_138537_, "objective")
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(
                            Commands.literal("display")
                                .then(
                                    Commands.literal("name")
                                        .then(
                                            Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                                                .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                .then(
                                                    Commands.argument("objective", ObjectiveArgument.objective())
                                                        .then(
                                                            Commands.argument("name", ComponentArgument.textComponent(pContext))
                                                                .executes(
                                                                    p_313517_ -> setScoreDisplay(
                                                                            p_313517_.getSource(),
                                                                            ScoreHolderArgument.getNamesWithDefaultWildcard(p_313517_, "targets"),
                                                                            ObjectiveArgument.getObjective(p_313517_, "objective"),
                                                                            ComponentArgument.getComponent(p_313517_, "name")
                                                                        )
                                                                )
                                                        )
                                                        .executes(
                                                            p_313555_ -> setScoreDisplay(
                                                                    p_313555_.getSource(),
                                                                    ScoreHolderArgument.getNamesWithDefaultWildcard(p_313555_, "targets"),
                                                                    ObjectiveArgument.getObjective(p_313555_, "objective"),
                                                                    null
                                                                )
                                                        )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("numberformat")
                                        .then(
                                            Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                                                .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                .then(
                                                    addNumberFormats(
                                                        pContext,
                                                        Commands.argument("objective", ObjectiveArgument.objective()),
                                                        (p_313512_, p_313513_) -> setScoreNumberFormat(
                                                                p_313512_.getSource(),
                                                                ScoreHolderArgument.getNamesWithDefaultWildcard(p_313512_, "targets"),
                                                                ObjectiveArgument.getObjective(p_313512_, "objective"),
                                                                p_313513_
                                                            )
                                                    )
                                                )
                                        )
                                )
                        )
                        .then(
                            Commands.literal("operation")
                                .then(
                                    Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                                        .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                        .then(
                                            Commands.argument("targetObjective", ObjectiveArgument.objective())
                                                .then(
                                                    Commands.argument("operation", OperationArgument.operation())
                                                        .then(
                                                            Commands.argument("source", ScoreHolderArgument.scoreHolders())
                                                                .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                                .then(
                                                                    Commands.argument("sourceObjective", ObjectiveArgument.objective())
                                                                        .executes(
                                                                            p_138471_ -> performOperation(
                                                                                    p_138471_.getSource(),
                                                                                    ScoreHolderArgument.getNamesWithDefaultWildcard(p_138471_, "targets"),
                                                                                    ObjectiveArgument.getWritableObjective(p_138471_, "targetObjective"),
                                                                                    OperationArgument.getOperation(p_138471_, "operation"),
                                                                                    ScoreHolderArgument.getNamesWithDefaultWildcard(p_138471_, "source"),
                                                                                    ObjectiveArgument.getObjective(p_138471_, "sourceObjective")
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static ArgumentBuilder<CommandSourceStack, ?> addNumberFormats(
        CommandBuildContext pContext, ArgumentBuilder<CommandSourceStack, ?> pArgumentBuilder, ScoreboardCommand.NumberFormatCommandExecutor pExecutor
    ) {
        return pArgumentBuilder.then(Commands.literal("blank").executes(p_313547_ -> pExecutor.run(p_313547_, BlankFormat.INSTANCE)))
            .then(Commands.literal("fixed").then(Commands.argument("contents", ComponentArgument.textComponent(pContext)).executes(p_313560_ -> {
                Component component = ComponentArgument.getComponent(p_313560_, "contents");
                return pExecutor.run(p_313560_, new FixedFormat(component));
            })))
            .then(Commands.literal("styled").then(Commands.argument("style", StyleArgument.style(pContext)).executes(p_313511_ -> {
                Style style = StyleArgument.getStyle(p_313511_, "style");
                return pExecutor.run(p_313511_, new StyledFormat(style));
            })))
            .executes(p_313549_ -> pExecutor.run(p_313549_, null));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createRenderTypeModify() {
        LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder = Commands.literal("rendertype");

        for (ObjectiveCriteria.RenderType objectivecriteria$rendertype : ObjectiveCriteria.RenderType.values()) {
            literalargumentbuilder.then(
                Commands.literal(objectivecriteria$rendertype.getId())
                    .executes(
                        p_138532_ -> setRenderType(p_138532_.getSource(), ObjectiveArgument.getObjective(p_138532_, "objective"), objectivecriteria$rendertype)
                    )
            );
        }

        return literalargumentbuilder;
    }

    private static CompletableFuture<Suggestions> suggestTriggers(CommandSourceStack pSource, Collection<ScoreHolder> pTargets, SuggestionsBuilder pSuggestions) {
        List<String> list = Lists.newArrayList();
        Scoreboard scoreboard = pSource.getServer().getScoreboard();

        for (Objective objective : scoreboard.getObjectives()) {
            if (objective.getCriteria() == ObjectiveCriteria.TRIGGER) {
                boolean flag = false;

                for (ScoreHolder scoreholder : pTargets) {
                    ReadOnlyScoreInfo readonlyscoreinfo = scoreboard.getPlayerScoreInfo(scoreholder, objective);
                    if (readonlyscoreinfo == null || readonlyscoreinfo.isLocked()) {
                        flag = true;
                        break;
                    }
                }

                if (flag) {
                    list.add(objective.getName());
                }
            }
        }

        return SharedSuggestionProvider.suggest(list, pSuggestions);
    }

    private static int getScore(CommandSourceStack pSource, ScoreHolder pScoreHolder, Objective pObjective) throws CommandSyntaxException {
        Scoreboard scoreboard = pSource.getServer().getScoreboard();
        ReadOnlyScoreInfo readonlyscoreinfo = scoreboard.getPlayerScoreInfo(pScoreHolder, pObjective);
        if (readonlyscoreinfo == null) {
            throw ERROR_NO_VALUE.create(pObjective.getName(), pScoreHolder.getFeedbackDisplayName());
        } else {
            pSource.sendSuccess(
                () -> Component.translatable(
                        "commands.scoreboard.players.get.success",
                        pScoreHolder.getFeedbackDisplayName(),
                        readonlyscoreinfo.value(),
                        pObjective.getFormattedDisplayName()
                    ),
                false
            );
            return readonlyscoreinfo.value();
        }
    }

    private static Component getFirstTargetName(Collection<ScoreHolder> pScores) {
        return pScores.iterator().next().getFeedbackDisplayName();
    }

    private static int performOperation(
        CommandSourceStack pSource,
        Collection<ScoreHolder> pTargets,
        Objective pTargetObjectives,
        OperationArgument.Operation pOperation,
        Collection<ScoreHolder> pSourceEntities,
        Objective pSourceObjective
    ) throws CommandSyntaxException {
        Scoreboard scoreboard = pSource.getServer().getScoreboard();
        int i = 0;

        for (ScoreHolder scoreholder : pTargets) {
            ScoreAccess scoreaccess = scoreboard.getOrCreatePlayerScore(scoreholder, pTargetObjectives);

            for (ScoreHolder scoreholder1 : pSourceEntities) {
                ScoreAccess scoreaccess1 = scoreboard.getOrCreatePlayerScore(scoreholder1, pSourceObjective);
                pOperation.apply(scoreaccess, scoreaccess1);
            }

            i += scoreaccess.get();
        }

        if (pTargets.size() == 1) {
            int j = i;
            pSource.sendSuccess(
                () -> Component.translatable(
                        "commands.scoreboard.players.operation.success.single", pTargetObjectives.getFormattedDisplayName(), getFirstTargetName(pTargets), j
                    ),
                true
            );
        } else {
            pSource.sendSuccess(
                () -> Component.translatable("commands.scoreboard.players.operation.success.multiple", pTargetObjectives.getFormattedDisplayName(), pTargets.size()),
                true
            );
        }

        return i;
    }

    private static int enableTrigger(CommandSourceStack pSource, Collection<ScoreHolder> pTargets, Objective pObjective) throws CommandSyntaxException {
        if (pObjective.getCriteria() != ObjectiveCriteria.TRIGGER) {
            throw ERROR_NOT_TRIGGER.create();
        } else {
            Scoreboard scoreboard = pSource.getServer().getScoreboard();
            int i = 0;

            for (ScoreHolder scoreholder : pTargets) {
                ScoreAccess scoreaccess = scoreboard.getOrCreatePlayerScore(scoreholder, pObjective);
                if (scoreaccess.locked()) {
                    scoreaccess.unlock();
                    i++;
                }
            }

            if (i == 0) {
                throw ERROR_TRIGGER_ALREADY_ENABLED.create();
            } else {
                if (pTargets.size() == 1) {
                    pSource.sendSuccess(
                        () -> Component.translatable(
                                "commands.scoreboard.players.enable.success.single", pObjective.getFormattedDisplayName(), getFirstTargetName(pTargets)
                            ),
                        true
                    );
                } else {
                    pSource.sendSuccess(
                        () -> Component.translatable(
                                "commands.scoreboard.players.enable.success.multiple", pObjective.getFormattedDisplayName(), pTargets.size()
                            ),
                        true
                    );
                }

                return i;
            }
        }
    }

    private static int resetScores(CommandSourceStack pSource, Collection<ScoreHolder> pTargets) {
        Scoreboard scoreboard = pSource.getServer().getScoreboard();

        for (ScoreHolder scoreholder : pTargets) {
            scoreboard.resetAllPlayerScores(scoreholder);
        }

        if (pTargets.size() == 1) {
            pSource.sendSuccess(() -> Component.translatable("commands.scoreboard.players.reset.all.single", getFirstTargetName(pTargets)), true);
        } else {
            pSource.sendSuccess(() -> Component.translatable("commands.scoreboard.players.reset.all.multiple", pTargets.size()), true);
        }

        return pTargets.size();
    }

    private static int resetScore(CommandSourceStack pSource, Collection<ScoreHolder> pTargets, Objective pObjective) {
        Scoreboard scoreboard = pSource.getServer().getScoreboard();

        for (ScoreHolder scoreholder : pTargets) {
            scoreboard.resetSinglePlayerScore(scoreholder, pObjective);
        }

        if (pTargets.size() == 1) {
            pSource.sendSuccess(
                () -> Component.translatable(
                        "commands.scoreboard.players.reset.specific.single", pObjective.getFormattedDisplayName(), getFirstTargetName(pTargets)
                    ),
                true
            );
        } else {
            pSource.sendSuccess(
                () -> Component.translatable("commands.scoreboard.players.reset.specific.multiple", pObjective.getFormattedDisplayName(), pTargets.size()),
                true
            );
        }

        return pTargets.size();
    }

    private static int setScore(CommandSourceStack pSource, Collection<ScoreHolder> pTargets, Objective pObjective, int pNewValue) {
        Scoreboard scoreboard = pSource.getServer().getScoreboard();

        for (ScoreHolder scoreholder : pTargets) {
            scoreboard.getOrCreatePlayerScore(scoreholder, pObjective).set(pNewValue);
        }

        if (pTargets.size() == 1) {
            pSource.sendSuccess(
                () -> Component.translatable(
                        "commands.scoreboard.players.set.success.single", pObjective.getFormattedDisplayName(), getFirstTargetName(pTargets), pNewValue
                    ),
                true
            );
        } else {
            pSource.sendSuccess(
                () -> Component.translatable(
                        "commands.scoreboard.players.set.success.multiple", pObjective.getFormattedDisplayName(), pTargets.size(), pNewValue
                    ),
                true
            );
        }

        return pNewValue * pTargets.size();
    }

    private static int setScoreDisplay(CommandSourceStack pSource, Collection<ScoreHolder> pTargets, Objective pObjective, @Nullable Component pDisplayName) {
        Scoreboard scoreboard = pSource.getServer().getScoreboard();

        for (ScoreHolder scoreholder : pTargets) {
            scoreboard.getOrCreatePlayerScore(scoreholder, pObjective).display(pDisplayName);
        }

        if (pDisplayName == null) {
            if (pTargets.size() == 1) {
                pSource.sendSuccess(
                    () -> Component.translatable(
                            "commands.scoreboard.players.display.name.clear.success.single", getFirstTargetName(pTargets), pObjective.getFormattedDisplayName()
                        ),
                    true
                );
            } else {
                pSource.sendSuccess(
                    () -> Component.translatable(
                            "commands.scoreboard.players.display.name.clear.success.multiple", pTargets.size(), pObjective.getFormattedDisplayName()
                        ),
                    true
                );
            }
        } else if (pTargets.size() == 1) {
            pSource.sendSuccess(
                () -> Component.translatable(
                        "commands.scoreboard.players.display.name.set.success.single",
                        pDisplayName,
                        getFirstTargetName(pTargets),
                        pObjective.getFormattedDisplayName()
                    ),
                true
            );
        } else {
            pSource.sendSuccess(
                () -> Component.translatable(
                        "commands.scoreboard.players.display.name.set.success.multiple", pDisplayName, pTargets.size(), pObjective.getFormattedDisplayName()
                    ),
                true
            );
        }

        return pTargets.size();
    }

    private static int setScoreNumberFormat(
        CommandSourceStack pSource, Collection<ScoreHolder> pTargets, Objective pObjective, @Nullable NumberFormat pNumberFormat
    ) {
        Scoreboard scoreboard = pSource.getServer().getScoreboard();

        for (ScoreHolder scoreholder : pTargets) {
            scoreboard.getOrCreatePlayerScore(scoreholder, pObjective).numberFormatOverride(pNumberFormat);
        }

        if (pNumberFormat == null) {
            if (pTargets.size() == 1) {
                pSource.sendSuccess(
                    () -> Component.translatable(
                            "commands.scoreboard.players.display.numberFormat.clear.success.single",
                            getFirstTargetName(pTargets),
                            pObjective.getFormattedDisplayName()
                        ),
                    true
                );
            } else {
                pSource.sendSuccess(
                    () -> Component.translatable(
                            "commands.scoreboard.players.display.numberFormat.clear.success.multiple", pTargets.size(), pObjective.getFormattedDisplayName()
                        ),
                    true
                );
            }
        } else if (pTargets.size() == 1) {
            pSource.sendSuccess(
                () -> Component.translatable(
                        "commands.scoreboard.players.display.numberFormat.set.success.single",
                        getFirstTargetName(pTargets),
                        pObjective.getFormattedDisplayName()
                    ),
                true
            );
        } else {
            pSource.sendSuccess(
                () -> Component.translatable(
                        "commands.scoreboard.players.display.numberFormat.set.success.multiple", pTargets.size(), pObjective.getFormattedDisplayName()
                    ),
                true
            );
        }

        return pTargets.size();
    }

    private static int addScore(CommandSourceStack pSource, Collection<ScoreHolder> pTargets, Objective pObjective, int pAmount) {
        Scoreboard scoreboard = pSource.getServer().getScoreboard();
        int i = 0;

        for (ScoreHolder scoreholder : pTargets) {
            ScoreAccess scoreaccess = scoreboard.getOrCreatePlayerScore(scoreholder, pObjective);
            scoreaccess.set(scoreaccess.get() + pAmount);
            i += scoreaccess.get();
        }

        if (pTargets.size() == 1) {
            int j = i;
            pSource.sendSuccess(
                () -> Component.translatable(
                        "commands.scoreboard.players.add.success.single", pAmount, pObjective.getFormattedDisplayName(), getFirstTargetName(pTargets), j
                    ),
                true
            );
        } else {
            pSource.sendSuccess(
                () -> Component.translatable(
                        "commands.scoreboard.players.add.success.multiple", pAmount, pObjective.getFormattedDisplayName(), pTargets.size()
                    ),
                true
            );
        }

        return i;
    }

    private static int removeScore(CommandSourceStack pSource, Collection<ScoreHolder> pTargets, Objective pObjective, int pAmount) {
        Scoreboard scoreboard = pSource.getServer().getScoreboard();
        int i = 0;

        for (ScoreHolder scoreholder : pTargets) {
            ScoreAccess scoreaccess = scoreboard.getOrCreatePlayerScore(scoreholder, pObjective);
            scoreaccess.set(scoreaccess.get() - pAmount);
            i += scoreaccess.get();
        }

        if (pTargets.size() == 1) {
            int j = i;
            pSource.sendSuccess(
                () -> Component.translatable(
                        "commands.scoreboard.players.remove.success.single", pAmount, pObjective.getFormattedDisplayName(), getFirstTargetName(pTargets), j
                    ),
                true
            );
        } else {
            pSource.sendSuccess(
                () -> Component.translatable(
                        "commands.scoreboard.players.remove.success.multiple", pAmount, pObjective.getFormattedDisplayName(), pTargets.size()
                    ),
                true
            );
        }

        return i;
    }

    private static int listTrackedPlayers(CommandSourceStack pSource) {
        Collection<ScoreHolder> collection = pSource.getServer().getScoreboard().getTrackedPlayers();
        if (collection.isEmpty()) {
            pSource.sendSuccess(() -> Component.translatable("commands.scoreboard.players.list.empty"), false);
        } else {
            pSource.sendSuccess(
                () -> Component.translatable(
                        "commands.scoreboard.players.list.success",
                        collection.size(),
                        ComponentUtils.formatList(collection, ScoreHolder::getFeedbackDisplayName)
                    ),
                false
            );
        }

        return collection.size();
    }

    private static int listTrackedPlayerScores(CommandSourceStack pSource, ScoreHolder pScore) {
        Object2IntMap<Objective> object2intmap = pSource.getServer().getScoreboard().listPlayerScores(pScore);
        if (object2intmap.isEmpty()) {
            pSource.sendSuccess(() -> Component.translatable("commands.scoreboard.players.list.entity.empty", pScore.getFeedbackDisplayName()), false);
        } else {
            pSource.sendSuccess(
                () -> Component.translatable("commands.scoreboard.players.list.entity.success", pScore.getFeedbackDisplayName(), object2intmap.size()),
                false
            );
            Object2IntMaps.fastForEach(
                object2intmap,
                p_313504_ -> pSource.sendSuccess(
                        () -> Component.translatable(
                                "commands.scoreboard.players.list.entity.entry",
                                ((Objective)p_313504_.getKey()).getFormattedDisplayName(),
                                p_313504_.getIntValue()
                            ),
                        false
                    )
            );
        }

        return object2intmap.size();
    }

    private static int clearDisplaySlot(CommandSourceStack pSource, DisplaySlot pSlot) throws CommandSyntaxException {
        Scoreboard scoreboard = pSource.getServer().getScoreboard();
        if (scoreboard.getDisplayObjective(pSlot) == null) {
            throw ERROR_DISPLAY_SLOT_ALREADY_EMPTY.create();
        } else {
            scoreboard.setDisplayObjective(pSlot, null);
            pSource.sendSuccess(() -> Component.translatable("commands.scoreboard.objectives.display.cleared", pSlot.getSerializedName()), true);
            return 0;
        }
    }

    private static int setDisplaySlot(CommandSourceStack pSource, DisplaySlot pSlot, Objective pObjective) throws CommandSyntaxException {
        Scoreboard scoreboard = pSource.getServer().getScoreboard();
        if (scoreboard.getDisplayObjective(pSlot) == pObjective) {
            throw ERROR_DISPLAY_SLOT_ALREADY_SET.create();
        } else {
            scoreboard.setDisplayObjective(pSlot, pObjective);
            pSource.sendSuccess(
                () -> Component.translatable("commands.scoreboard.objectives.display.set", pSlot.getSerializedName(), pObjective.getDisplayName()), true
            );
            return 0;
        }
    }

    private static int setDisplayName(CommandSourceStack pSource, Objective pObjective, Component pDisplayName) {
        if (!pObjective.getDisplayName().equals(pDisplayName)) {
            pObjective.setDisplayName(pDisplayName);
            pSource.sendSuccess(
                () -> Component.translatable("commands.scoreboard.objectives.modify.displayname", pObjective.getName(), pObjective.getFormattedDisplayName()),
                true
            );
        }

        return 0;
    }

    private static int setDisplayAutoUpdate(CommandSourceStack pSource, Objective pObjective, boolean pDisplayAutoUpdate) {
        if (pObjective.displayAutoUpdate() != pDisplayAutoUpdate) {
            pObjective.setDisplayAutoUpdate(pDisplayAutoUpdate);
            if (pDisplayAutoUpdate) {
                pSource.sendSuccess(
                    () -> Component.translatable(
                            "commands.scoreboard.objectives.modify.displayAutoUpdate.enable", pObjective.getName(), pObjective.getFormattedDisplayName()
                        ),
                    true
                );
            } else {
                pSource.sendSuccess(
                    () -> Component.translatable(
                            "commands.scoreboard.objectives.modify.displayAutoUpdate.disable", pObjective.getName(), pObjective.getFormattedDisplayName()
                        ),
                    true
                );
            }
        }

        return 0;
    }

    private static int setObjectiveFormat(CommandSourceStack pSource, Objective pObjective, @Nullable NumberFormat pFormat) {
        pObjective.setNumberFormat(pFormat);
        if (pFormat != null) {
            pSource.sendSuccess(() -> Component.translatable("commands.scoreboard.objectives.modify.objectiveFormat.set", pObjective.getName()), true);
        } else {
            pSource.sendSuccess(() -> Component.translatable("commands.scoreboard.objectives.modify.objectiveFormat.clear", pObjective.getName()), true);
        }

        return 0;
    }

    private static int setRenderType(CommandSourceStack pSource, Objective pObjective, ObjectiveCriteria.RenderType pRenderType) {
        if (pObjective.getRenderType() != pRenderType) {
            pObjective.setRenderType(pRenderType);
            pSource.sendSuccess(() -> Component.translatable("commands.scoreboard.objectives.modify.rendertype", pObjective.getFormattedDisplayName()), true);
        }

        return 0;
    }

    private static int removeObjective(CommandSourceStack pSource, Objective pObjective) {
        Scoreboard scoreboard = pSource.getServer().getScoreboard();
        scoreboard.removeObjective(pObjective);
        pSource.sendSuccess(() -> Component.translatable("commands.scoreboard.objectives.remove.success", pObjective.getFormattedDisplayName()), true);
        return scoreboard.getObjectives().size();
    }

    private static int addObjective(CommandSourceStack pSource, String pName, ObjectiveCriteria pCriteria, Component pDisplayName) throws CommandSyntaxException {
        Scoreboard scoreboard = pSource.getServer().getScoreboard();
        if (scoreboard.getObjective(pName) != null) {
            throw ERROR_OBJECTIVE_ALREADY_EXISTS.create();
        } else {
            scoreboard.addObjective(pName, pCriteria, pDisplayName, pCriteria.getDefaultRenderType(), false, null);
            Objective objective = scoreboard.getObjective(pName);
            pSource.sendSuccess(() -> Component.translatable("commands.scoreboard.objectives.add.success", objective.getFormattedDisplayName()), true);
            return scoreboard.getObjectives().size();
        }
    }

    private static int listObjectives(CommandSourceStack pSource) {
        Collection<Objective> collection = pSource.getServer().getScoreboard().getObjectives();
        if (collection.isEmpty()) {
            pSource.sendSuccess(() -> Component.translatable("commands.scoreboard.objectives.list.empty"), false);
        } else {
            pSource.sendSuccess(
                () -> Component.translatable(
                        "commands.scoreboard.objectives.list.success",
                        collection.size(),
                        ComponentUtils.formatList(collection, Objective::getFormattedDisplayName)
                    ),
                false
            );
        }

        return collection.size();
    }

    @FunctionalInterface
    public interface NumberFormatCommandExecutor {
        int run(CommandContext<CommandSourceStack> pContext, @Nullable NumberFormat pFormat) throws CommandSyntaxException;
    }
}
