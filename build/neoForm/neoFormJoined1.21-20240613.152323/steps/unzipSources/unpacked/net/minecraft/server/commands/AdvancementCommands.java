package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class AdvancementCommands {
    private static final DynamicCommandExceptionType ERROR_NO_ACTION_PERFORMED = new DynamicCommandExceptionType(p_311534_ -> (Component)p_311534_);
    private static final Dynamic2CommandExceptionType ERROR_CRITERION_NOT_FOUND = new Dynamic2CommandExceptionType(
        (p_351729_, p_351730_) -> Component.translatableEscape("commands.advancement.criterionNotFound", p_351729_, p_351730_)
    );
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_ADVANCEMENTS = (p_136344_, p_136345_) -> {
        Collection<AdvancementHolder> collection = p_136344_.getSource().getServer().getAdvancements().getAllAdvancements();
        return SharedSuggestionProvider.suggestResource(collection.stream().map(AdvancementHolder::id), p_136345_);
    };

    public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
        pDispatcher.register(
            Commands.literal("advancement")
                .requires(p_136318_ -> p_136318_.hasPermission(2))
                .then(
                    Commands.literal("grant")
                        .then(
                            Commands.argument("targets", EntityArgument.players())
                                .then(
                                    Commands.literal("only")
                                        .then(
                                            Commands.argument("advancement", ResourceLocationArgument.id())
                                                .suggests(SUGGEST_ADVANCEMENTS)
                                                .executes(
                                                    p_300759_ -> perform(
                                                            p_300759_.getSource(),
                                                            EntityArgument.getPlayers(p_300759_, "targets"),
                                                            AdvancementCommands.Action.GRANT,
                                                            getAdvancements(
                                                                p_300759_,
                                                                ResourceLocationArgument.getAdvancement(p_300759_, "advancement"),
                                                                AdvancementCommands.Mode.ONLY
                                                            )
                                                        )
                                                )
                                                .then(
                                                    Commands.argument("criterion", StringArgumentType.greedyString())
                                                        .suggests(
                                                            (p_300739_, p_300740_) -> SharedSuggestionProvider.suggest(
                                                                    ResourceLocationArgument.getAdvancement(p_300739_, "advancement")
                                                                        .value()
                                                                        .criteria()
                                                                        .keySet(),
                                                                    p_300740_
                                                                )
                                                        )
                                                        .executes(
                                                            p_300748_ -> performCriterion(
                                                                    p_300748_.getSource(),
                                                                    EntityArgument.getPlayers(p_300748_, "targets"),
                                                                    AdvancementCommands.Action.GRANT,
                                                                    ResourceLocationArgument.getAdvancement(p_300748_, "advancement"),
                                                                    StringArgumentType.getString(p_300748_, "criterion")
                                                                )
                                                        )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("from")
                                        .then(
                                            Commands.argument("advancement", ResourceLocationArgument.id())
                                                .suggests(SUGGEST_ADVANCEMENTS)
                                                .executes(
                                                    p_300760_ -> perform(
                                                            p_300760_.getSource(),
                                                            EntityArgument.getPlayers(p_300760_, "targets"),
                                                            AdvancementCommands.Action.GRANT,
                                                            getAdvancements(
                                                                p_300760_,
                                                                ResourceLocationArgument.getAdvancement(p_300760_, "advancement"),
                                                                AdvancementCommands.Mode.FROM
                                                            )
                                                        )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("until")
                                        .then(
                                            Commands.argument("advancement", ResourceLocationArgument.id())
                                                .suggests(SUGGEST_ADVANCEMENTS)
                                                .executes(
                                                    p_300749_ -> perform(
                                                            p_300749_.getSource(),
                                                            EntityArgument.getPlayers(p_300749_, "targets"),
                                                            AdvancementCommands.Action.GRANT,
                                                            getAdvancements(
                                                                p_300749_,
                                                                ResourceLocationArgument.getAdvancement(p_300749_, "advancement"),
                                                                AdvancementCommands.Mode.UNTIL
                                                            )
                                                        )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("through")
                                        .then(
                                            Commands.argument("advancement", ResourceLocationArgument.id())
                                                .suggests(SUGGEST_ADVANCEMENTS)
                                                .executes(
                                                    p_300741_ -> perform(
                                                            p_300741_.getSource(),
                                                            EntityArgument.getPlayers(p_300741_, "targets"),
                                                            AdvancementCommands.Action.GRANT,
                                                            getAdvancements(
                                                                p_300741_,
                                                                ResourceLocationArgument.getAdvancement(p_300741_, "advancement"),
                                                                AdvancementCommands.Mode.THROUGH
                                                            )
                                                        )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("everything")
                                        .executes(
                                            p_136353_ -> perform(
                                                    p_136353_.getSource(),
                                                    EntityArgument.getPlayers(p_136353_, "targets"),
                                                    AdvancementCommands.Action.GRANT,
                                                    p_136353_.getSource().getServer().getAdvancements().getAllAdvancements()
                                                )
                                        )
                                )
                        )
                )
                .then(
                    Commands.literal("revoke")
                        .then(
                            Commands.argument("targets", EntityArgument.players())
                                .then(
                                    Commands.literal("only")
                                        .then(
                                            Commands.argument("advancement", ResourceLocationArgument.id())
                                                .suggests(SUGGEST_ADVANCEMENTS)
                                                .executes(
                                                    p_300745_ -> perform(
                                                            p_300745_.getSource(),
                                                            EntityArgument.getPlayers(p_300745_, "targets"),
                                                            AdvancementCommands.Action.REVOKE,
                                                            getAdvancements(
                                                                p_300745_,
                                                                ResourceLocationArgument.getAdvancement(p_300745_, "advancement"),
                                                                AdvancementCommands.Mode.ONLY
                                                            )
                                                        )
                                                )
                                                .then(
                                                    Commands.argument("criterion", StringArgumentType.greedyString())
                                                        .suggests(
                                                            (p_300746_, p_300747_) -> SharedSuggestionProvider.suggest(
                                                                    ResourceLocationArgument.getAdvancement(p_300746_, "advancement")
                                                                        .value()
                                                                        .criteria()
                                                                        .keySet(),
                                                                    p_300747_
                                                                )
                                                        )
                                                        .executes(
                                                            p_300737_ -> performCriterion(
                                                                    p_300737_.getSource(),
                                                                    EntityArgument.getPlayers(p_300737_, "targets"),
                                                                    AdvancementCommands.Action.REVOKE,
                                                                    ResourceLocationArgument.getAdvancement(p_300737_, "advancement"),
                                                                    StringArgumentType.getString(p_300737_, "criterion")
                                                                )
                                                        )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("from")
                                        .then(
                                            Commands.argument("advancement", ResourceLocationArgument.id())
                                                .suggests(SUGGEST_ADVANCEMENTS)
                                                .executes(
                                                    p_300758_ -> perform(
                                                            p_300758_.getSource(),
                                                            EntityArgument.getPlayers(p_300758_, "targets"),
                                                            AdvancementCommands.Action.REVOKE,
                                                            getAdvancements(
                                                                p_300758_,
                                                                ResourceLocationArgument.getAdvancement(p_300758_, "advancement"),
                                                                AdvancementCommands.Mode.FROM
                                                            )
                                                        )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("until")
                                        .then(
                                            Commands.argument("advancement", ResourceLocationArgument.id())
                                                .suggests(SUGGEST_ADVANCEMENTS)
                                                .executes(
                                                    p_300738_ -> perform(
                                                            p_300738_.getSource(),
                                                            EntityArgument.getPlayers(p_300738_, "targets"),
                                                            AdvancementCommands.Action.REVOKE,
                                                            getAdvancements(
                                                                p_300738_,
                                                                ResourceLocationArgument.getAdvancement(p_300738_, "advancement"),
                                                                AdvancementCommands.Mode.UNTIL
                                                            )
                                                        )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("through")
                                        .then(
                                            Commands.argument("advancement", ResourceLocationArgument.id())
                                                .suggests(SUGGEST_ADVANCEMENTS)
                                                .executes(
                                                    p_300764_ -> perform(
                                                            p_300764_.getSource(),
                                                            EntityArgument.getPlayers(p_300764_, "targets"),
                                                            AdvancementCommands.Action.REVOKE,
                                                            getAdvancements(
                                                                p_300764_,
                                                                ResourceLocationArgument.getAdvancement(p_300764_, "advancement"),
                                                                AdvancementCommands.Mode.THROUGH
                                                            )
                                                        )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("everything")
                                        .executes(
                                            p_136313_ -> perform(
                                                    p_136313_.getSource(),
                                                    EntityArgument.getPlayers(p_136313_, "targets"),
                                                    AdvancementCommands.Action.REVOKE,
                                                    p_136313_.getSource().getServer().getAdvancements().getAllAdvancements()
                                                )
                                        )
                                )
                        )
                )
        );
    }

    /**
     * Performs the given action on each advancement in the list, for each player.
     *
     * @return The number of affected advancements across all players.
     */
    private static int perform(
        CommandSourceStack pSource, Collection<ServerPlayer> pTargets, AdvancementCommands.Action pAction, Collection<AdvancementHolder> pAdvancements
    ) throws CommandSyntaxException {
        int i = 0;

        for (ServerPlayer serverplayer : pTargets) {
            i += pAction.perform(serverplayer, pAdvancements);
        }

        if (i == 0) {
            if (pAdvancements.size() == 1) {
                if (pTargets.size() == 1) {
                    throw ERROR_NO_ACTION_PERFORMED.create(
                        Component.translatable(
                            pAction.getKey() + ".one.to.one.failure",
                            Advancement.name(pAdvancements.iterator().next()),
                            pTargets.iterator().next().getDisplayName()
                        )
                    );
                } else {
                    throw ERROR_NO_ACTION_PERFORMED.create(
                        Component.translatable(pAction.getKey() + ".one.to.many.failure", Advancement.name(pAdvancements.iterator().next()), pTargets.size())
                    );
                }
            } else if (pTargets.size() == 1) {
                throw ERROR_NO_ACTION_PERFORMED.create(
                    Component.translatable(pAction.getKey() + ".many.to.one.failure", pAdvancements.size(), pTargets.iterator().next().getDisplayName())
                );
            } else {
                throw ERROR_NO_ACTION_PERFORMED.create(Component.translatable(pAction.getKey() + ".many.to.many.failure", pAdvancements.size(), pTargets.size()));
            }
        } else {
            if (pAdvancements.size() == 1) {
                if (pTargets.size() == 1) {
                    pSource.sendSuccess(
                        () -> Component.translatable(
                                pAction.getKey() + ".one.to.one.success",
                                Advancement.name(pAdvancements.iterator().next()),
                                pTargets.iterator().next().getDisplayName()
                            ),
                        true
                    );
                } else {
                    pSource.sendSuccess(
                        () -> Component.translatable(
                                pAction.getKey() + ".one.to.many.success", Advancement.name(pAdvancements.iterator().next()), pTargets.size()
                            ),
                        true
                    );
                }
            } else if (pTargets.size() == 1) {
                pSource.sendSuccess(
                    () -> Component.translatable(pAction.getKey() + ".many.to.one.success", pAdvancements.size(), pTargets.iterator().next().getDisplayName()),
                    true
                );
            } else {
                pSource.sendSuccess(() -> Component.translatable(pAction.getKey() + ".many.to.many.success", pAdvancements.size(), pTargets.size()), true);
            }

            return i;
        }
    }

    private static int performCriterion(
        CommandSourceStack pSource, Collection<ServerPlayer> pTargets, AdvancementCommands.Action pAction, AdvancementHolder pAdvancement, String pCriterionName
    ) throws CommandSyntaxException {
        int i = 0;
        Advancement advancement = pAdvancement.value();
        if (!advancement.criteria().containsKey(pCriterionName)) {
            throw ERROR_CRITERION_NOT_FOUND.create(Advancement.name(pAdvancement), pCriterionName);
        } else {
            for (ServerPlayer serverplayer : pTargets) {
                if (pAction.performCriterion(serverplayer, pAdvancement, pCriterionName)) {
                    i++;
                }
            }

            if (i == 0) {
                if (pTargets.size() == 1) {
                    throw ERROR_NO_ACTION_PERFORMED.create(
                        Component.translatable(
                            pAction.getKey() + ".criterion.to.one.failure",
                            pCriterionName,
                            Advancement.name(pAdvancement),
                            pTargets.iterator().next().getDisplayName()
                        )
                    );
                } else {
                    throw ERROR_NO_ACTION_PERFORMED.create(
                        Component.translatable(pAction.getKey() + ".criterion.to.many.failure", pCriterionName, Advancement.name(pAdvancement), pTargets.size())
                    );
                }
            } else {
                if (pTargets.size() == 1) {
                    pSource.sendSuccess(
                        () -> Component.translatable(
                                pAction.getKey() + ".criterion.to.one.success",
                                pCriterionName,
                                Advancement.name(pAdvancement),
                                pTargets.iterator().next().getDisplayName()
                            ),
                        true
                    );
                } else {
                    pSource.sendSuccess(
                        () -> Component.translatable(
                                pAction.getKey() + ".criterion.to.many.success", pCriterionName, Advancement.name(pAdvancement), pTargets.size()
                            ),
                        true
                    );
                }

                return i;
            }
        }
    }

    private static List<AdvancementHolder> getAdvancements(
        CommandContext<CommandSourceStack> pContext, AdvancementHolder pAdvancement, AdvancementCommands.Mode pMode
    ) {
        AdvancementTree advancementtree = pContext.getSource().getServer().getAdvancements().tree();
        AdvancementNode advancementnode = advancementtree.get(pAdvancement);
        if (advancementnode == null) {
            return List.of(pAdvancement);
        } else {
            List<AdvancementHolder> list = new ArrayList<>();
            if (pMode.parents) {
                for (AdvancementNode advancementnode1 = advancementnode.parent(); advancementnode1 != null; advancementnode1 = advancementnode1.parent()) {
                    list.add(advancementnode1.holder());
                }
            }

            list.add(pAdvancement);
            if (pMode.children) {
                addChildren(advancementnode, list);
            }

            return list;
        }
    }

    private static void addChildren(AdvancementNode pNode, List<AdvancementHolder> pOutput) {
        for (AdvancementNode advancementnode : pNode.children()) {
            pOutput.add(advancementnode.holder());
            addChildren(advancementnode, pOutput);
        }
    }

    static enum Action {
        GRANT("grant") {
            @Override
            protected boolean perform(ServerPlayer p_136395_, AdvancementHolder p_301029_) {
                AdvancementProgress advancementprogress = p_136395_.getAdvancements().getOrStartProgress(p_301029_);
                if (advancementprogress.isDone()) {
                    return false;
                } else {
                    for (String s : advancementprogress.getRemainingCriteria()) {
                        p_136395_.getAdvancements().award(p_301029_, s);
                    }

                    return true;
                }
            }

            @Override
            protected boolean performCriterion(ServerPlayer p_136398_, AdvancementHolder p_301300_, String p_136400_) {
                return p_136398_.getAdvancements().award(p_301300_, p_136400_);
            }
        },
        REVOKE("revoke") {
            @Override
            protected boolean perform(ServerPlayer p_136406_, AdvancementHolder p_301273_) {
                AdvancementProgress advancementprogress = p_136406_.getAdvancements().getOrStartProgress(p_301273_);
                if (!advancementprogress.hasProgress()) {
                    return false;
                } else {
                    for (String s : advancementprogress.getCompletedCriteria()) {
                        p_136406_.getAdvancements().revoke(p_301273_, s);
                    }

                    return true;
                }
            }

            @Override
            protected boolean performCriterion(ServerPlayer p_136409_, AdvancementHolder p_301094_, String p_136411_) {
                return p_136409_.getAdvancements().revoke(p_301094_, p_136411_);
            }
        };

        private final String key;

        Action(String pKey) {
            this.key = "commands.advancement." + pKey;
        }

        /**
         * Applies this action to all the given advancements.
         *
         * @return The number of players affected.
         */
        public int perform(ServerPlayer pPlayer, Iterable<AdvancementHolder> pAdvancements) {
            int i = 0;

            for (AdvancementHolder advancementholder : pAdvancements) {
                if (this.perform(pPlayer, advancementholder)) {
                    i++;
                }
            }

            return i;
        }

        protected abstract boolean perform(ServerPlayer pPlayer, AdvancementHolder pAdvancement);

        protected abstract boolean performCriterion(ServerPlayer pPlayer, AdvancementHolder pAdvancement, String pCriterionName);

        protected String getKey() {
            return this.key;
        }
    }

    static enum Mode {
        ONLY(false, false),
        THROUGH(true, true),
        FROM(false, true),
        UNTIL(true, false),
        EVERYTHING(true, true);

        final boolean parents;
        final boolean children;

        private Mode(boolean pParents, boolean pChildren) {
            this.parents = pParents;
            this.children = pChildren;
        }
    }
}
