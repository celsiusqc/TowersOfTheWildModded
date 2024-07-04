package net.minecraft.server.commands;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.HeightmapTypeArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.commands.arguments.RangeArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.ResourceOrIdArgument;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.commands.arguments.ScoreHolderArgument;
import net.minecraft.commands.arguments.SlotsArgument;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.SwizzleArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.CustomModifierExecutor;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.tasks.BuildContexts;
import net.minecraft.commands.execution.tasks.CallFunction;
import net.minecraft.commands.execution.tasks.FallthroughTask;
import net.minecraft.commands.execution.tasks.IsolatedCall;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.InstantiatedFunction;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.DataCommands;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Attackable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.Targeting;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.inventory.SlotRange;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

public class ExecuteCommand {
    private static final int MAX_TEST_AREA = 32768;
    private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType(
        (p_304211_, p_304212_) -> Component.translatableEscape("commands.execute.blocks.toobig", p_304211_, p_304212_)
    );
    private static final SimpleCommandExceptionType ERROR_CONDITIONAL_FAILED = new SimpleCommandExceptionType(
        Component.translatable("commands.execute.conditional.fail")
    );
    private static final DynamicCommandExceptionType ERROR_CONDITIONAL_FAILED_COUNT = new DynamicCommandExceptionType(
        p_304210_ -> Component.translatableEscape("commands.execute.conditional.fail_count", p_304210_)
    );
    @VisibleForTesting
    public static final Dynamic2CommandExceptionType ERROR_FUNCTION_CONDITION_INSTANTATION_FAILURE = new Dynamic2CommandExceptionType(
        (p_305676_, p_305677_) -> Component.translatableEscape("commands.execute.function.instantiationFailure", p_305676_, p_305677_)
    );
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_PREDICATE = (p_335218_, p_335219_) -> {
        ReloadableServerRegistries.Holder reloadableserverregistries$holder = p_335218_.getSource().getServer().reloadableRegistries();
        return SharedSuggestionProvider.suggestResource(reloadableserverregistries$holder.getKeys(Registries.PREDICATE), p_335219_);
    };

    public static void register(CommandDispatcher<CommandSourceStack> pDispatcher, CommandBuildContext pContext) {
        LiteralCommandNode<CommandSourceStack> literalcommandnode = pDispatcher.register(
            Commands.literal("execute").requires(p_137197_ -> p_137197_.hasPermission(2))
        );
        pDispatcher.register(
            Commands.literal("execute")
                .requires(p_137103_ -> p_137103_.hasPermission(2))
                .then(Commands.literal("run").redirect(pDispatcher.getRoot()))
                .then(addConditionals(literalcommandnode, Commands.literal("if"), true, pContext))
                .then(addConditionals(literalcommandnode, Commands.literal("unless"), false, pContext))
                .then(Commands.literal("as").then(Commands.argument("targets", EntityArgument.entities()).fork(literalcommandnode, p_137299_ -> {
                    List<CommandSourceStack> list = Lists.newArrayList();

                    for (Entity entity : EntityArgument.getOptionalEntities(p_137299_, "targets")) {
                        list.add(p_137299_.getSource().withEntity(entity));
                    }

                    return list;
                })))
                .then(
                    Commands.literal("at")
                        .then(
                            Commands.argument("targets", EntityArgument.entities())
                                .fork(
                                    literalcommandnode,
                                    p_284653_ -> {
                                        List<CommandSourceStack> list = Lists.newArrayList();

                                        for (Entity entity : EntityArgument.getOptionalEntities(p_284653_, "targets")) {
                                            list.add(
                                                p_284653_.getSource()
                                                    .withLevel((ServerLevel)entity.level())
                                                    .withPosition(entity.position())
                                                    .withRotation(entity.getRotationVector())
                                            );
                                        }

                                        return list;
                                    }
                                )
                        )
                )
                .then(
                    Commands.literal("store")
                        .then(wrapStores(literalcommandnode, Commands.literal("result"), true))
                        .then(wrapStores(literalcommandnode, Commands.literal("success"), false))
                )
                .then(
                    Commands.literal("positioned")
                        .then(
                            Commands.argument("pos", Vec3Argument.vec3())
                                .redirect(
                                    literalcommandnode,
                                    p_137295_ -> p_137295_.getSource()
                                            .withPosition(Vec3Argument.getVec3(p_137295_, "pos"))
                                            .withAnchor(EntityAnchorArgument.Anchor.FEET)
                                )
                        )
                        .then(Commands.literal("as").then(Commands.argument("targets", EntityArgument.entities()).fork(literalcommandnode, p_137293_ -> {
                            List<CommandSourceStack> list = Lists.newArrayList();

                            for (Entity entity : EntityArgument.getOptionalEntities(p_137293_, "targets")) {
                                list.add(p_137293_.getSource().withPosition(entity.position()));
                            }

                            return list;
                        })))
                        .then(
                            Commands.literal("over")
                                .then(Commands.argument("heightmap", HeightmapTypeArgument.heightmap()).redirect(literalcommandnode, p_274814_ -> {
                                    Vec3 vec3 = p_274814_.getSource().getPosition();
                                    ServerLevel serverlevel = p_274814_.getSource().getLevel();
                                    double d0 = vec3.x();
                                    double d1 = vec3.z();
                                    if (!serverlevel.hasChunk(SectionPos.blockToSectionCoord(d0), SectionPos.blockToSectionCoord(d1))) {
                                        throw BlockPosArgument.ERROR_NOT_LOADED.create();
                                    } else {
                                        int i = serverlevel.getHeight(HeightmapTypeArgument.getHeightmap(p_274814_, "heightmap"), Mth.floor(d0), Mth.floor(d1));
                                        return p_274814_.getSource().withPosition(new Vec3(d0, (double)i, d1));
                                    }
                                }))
                        )
                )
                .then(
                    Commands.literal("rotated")
                        .then(
                            Commands.argument("rot", RotationArgument.rotation())
                                .redirect(
                                    literalcommandnode,
                                    p_137291_ -> p_137291_.getSource()
                                            .withRotation(RotationArgument.getRotation(p_137291_, "rot").getRotation(p_137291_.getSource()))
                                )
                        )
                        .then(Commands.literal("as").then(Commands.argument("targets", EntityArgument.entities()).fork(literalcommandnode, p_137289_ -> {
                            List<CommandSourceStack> list = Lists.newArrayList();

                            for (Entity entity : EntityArgument.getOptionalEntities(p_137289_, "targets")) {
                                list.add(p_137289_.getSource().withRotation(entity.getRotationVector()));
                            }

                            return list;
                        })))
                )
                .then(
                    Commands.literal("facing")
                        .then(
                            Commands.literal("entity")
                                .then(
                                    Commands.argument("targets", EntityArgument.entities())
                                        .then(Commands.argument("anchor", EntityAnchorArgument.anchor()).fork(literalcommandnode, p_137287_ -> {
                                            List<CommandSourceStack> list = Lists.newArrayList();
                                            EntityAnchorArgument.Anchor entityanchorargument$anchor = EntityAnchorArgument.getAnchor(p_137287_, "anchor");

                                            for (Entity entity : EntityArgument.getOptionalEntities(p_137287_, "targets")) {
                                                list.add(p_137287_.getSource().facing(entity, entityanchorargument$anchor));
                                            }

                                            return list;
                                        }))
                                )
                        )
                        .then(
                            Commands.argument("pos", Vec3Argument.vec3())
                                .redirect(literalcommandnode, p_137285_ -> p_137285_.getSource().facing(Vec3Argument.getVec3(p_137285_, "pos")))
                        )
                )
                .then(
                    Commands.literal("align")
                        .then(
                            Commands.argument("axes", SwizzleArgument.swizzle())
                                .redirect(
                                    literalcommandnode,
                                    p_137283_ -> p_137283_.getSource()
                                            .withPosition(p_137283_.getSource().getPosition().align(SwizzleArgument.getSwizzle(p_137283_, "axes")))
                                )
                        )
                )
                .then(
                    Commands.literal("anchored")
                        .then(
                            Commands.argument("anchor", EntityAnchorArgument.anchor())
                                .redirect(
                                    literalcommandnode, p_137281_ -> p_137281_.getSource().withAnchor(EntityAnchorArgument.getAnchor(p_137281_, "anchor"))
                                )
                        )
                )
                .then(
                    Commands.literal("in")
                        .then(
                            Commands.argument("dimension", DimensionArgument.dimension())
                                .redirect(
                                    literalcommandnode, p_137279_ -> p_137279_.getSource().withLevel(DimensionArgument.getDimension(p_137279_, "dimension"))
                                )
                        )
                )
                .then(
                    Commands.literal("summon")
                        .then(
                            Commands.argument("entity", ResourceArgument.resource(pContext, Registries.ENTITY_TYPE))
                                .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                .redirect(
                                    literalcommandnode,
                                    p_269759_ -> spawnEntityAndRedirect(p_269759_.getSource(), ResourceArgument.getSummonableEntityType(p_269759_, "entity"))
                                )
                        )
                )
                .then(createRelationOperations(literalcommandnode, Commands.literal("on")))
        );
    }

    private static ArgumentBuilder<CommandSourceStack, ?> wrapStores(
        LiteralCommandNode<CommandSourceStack> pParent, LiteralArgumentBuilder<CommandSourceStack> pLiteral, boolean pStoringResult
    ) {
        pLiteral.then(
            Commands.literal("score")
                .then(
                    Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                        .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                        .then(
                            Commands.argument("objective", ObjectiveArgument.objective())
                                .redirect(
                                    pParent,
                                    p_137271_ -> storeValue(
                                            p_137271_.getSource(),
                                            ScoreHolderArgument.getNamesWithDefaultWildcard(p_137271_, "targets"),
                                            ObjectiveArgument.getObjective(p_137271_, "objective"),
                                            pStoringResult
                                        )
                                )
                        )
                )
        );
        pLiteral.then(
            Commands.literal("bossbar")
                .then(
                    Commands.argument("id", ResourceLocationArgument.id())
                        .suggests(BossBarCommands.SUGGEST_BOSS_BAR)
                        .then(
                            Commands.literal("value")
                                .redirect(pParent, p_137259_ -> storeValue(p_137259_.getSource(), BossBarCommands.getBossBar(p_137259_), true, pStoringResult))
                        )
                        .then(
                            Commands.literal("max")
                                .redirect(pParent, p_137247_ -> storeValue(p_137247_.getSource(), BossBarCommands.getBossBar(p_137247_), false, pStoringResult))
                        )
                )
        );

        for (DataCommands.DataProvider datacommands$dataprovider : DataCommands.TARGET_PROVIDERS) {
            datacommands$dataprovider.wrap(
                pLiteral,
                p_137101_ -> p_137101_.then(
                        Commands.argument("path", NbtPathArgument.nbtPath())
                            .then(
                                Commands.literal("int")
                                    .then(
                                        Commands.argument("scale", DoubleArgumentType.doubleArg())
                                            .redirect(
                                                pParent,
                                                p_180216_ -> storeData(
                                                        p_180216_.getSource(),
                                                        datacommands$dataprovider.access(p_180216_),
                                                        NbtPathArgument.getPath(p_180216_, "path"),
                                                        p_180219_ -> IntTag.valueOf((int)((double)p_180219_ * DoubleArgumentType.getDouble(p_180216_, "scale"))),
                                                        pStoringResult
                                                    )
                                            )
                                    )
                            )
                            .then(
                                Commands.literal("float")
                                    .then(
                                        Commands.argument("scale", DoubleArgumentType.doubleArg())
                                            .redirect(
                                                pParent,
                                                p_180209_ -> storeData(
                                                        p_180209_.getSource(),
                                                        datacommands$dataprovider.access(p_180209_),
                                                        NbtPathArgument.getPath(p_180209_, "path"),
                                                        p_180212_ -> FloatTag.valueOf(
                                                                (float)((double)p_180212_ * DoubleArgumentType.getDouble(p_180209_, "scale"))
                                                            ),
                                                        pStoringResult
                                                    )
                                            )
                                    )
                            )
                            .then(
                                Commands.literal("short")
                                    .then(
                                        Commands.argument("scale", DoubleArgumentType.doubleArg())
                                            .redirect(
                                                pParent,
                                                p_180199_ -> storeData(
                                                        p_180199_.getSource(),
                                                        datacommands$dataprovider.access(p_180199_),
                                                        NbtPathArgument.getPath(p_180199_, "path"),
                                                        p_180202_ -> ShortTag.valueOf(
                                                                (short)((int)((double)p_180202_ * DoubleArgumentType.getDouble(p_180199_, "scale")))
                                                            ),
                                                        pStoringResult
                                                    )
                                            )
                                    )
                            )
                            .then(
                                Commands.literal("long")
                                    .then(
                                        Commands.argument("scale", DoubleArgumentType.doubleArg())
                                            .redirect(
                                                pParent,
                                                p_180189_ -> storeData(
                                                        p_180189_.getSource(),
                                                        datacommands$dataprovider.access(p_180189_),
                                                        NbtPathArgument.getPath(p_180189_, "path"),
                                                        p_180192_ -> LongTag.valueOf(
                                                                (long)((double)p_180192_ * DoubleArgumentType.getDouble(p_180189_, "scale"))
                                                            ),
                                                        pStoringResult
                                                    )
                                            )
                                    )
                            )
                            .then(
                                Commands.literal("double")
                                    .then(
                                        Commands.argument("scale", DoubleArgumentType.doubleArg())
                                            .redirect(
                                                pParent,
                                                p_180179_ -> storeData(
                                                        p_180179_.getSource(),
                                                        datacommands$dataprovider.access(p_180179_),
                                                        NbtPathArgument.getPath(p_180179_, "path"),
                                                        p_180182_ -> DoubleTag.valueOf((double)p_180182_ * DoubleArgumentType.getDouble(p_180179_, "scale")),
                                                        pStoringResult
                                                    )
                                            )
                                    )
                            )
                            .then(
                                Commands.literal("byte")
                                    .then(
                                        Commands.argument("scale", DoubleArgumentType.doubleArg())
                                            .redirect(
                                                pParent,
                                                p_180156_ -> storeData(
                                                        p_180156_.getSource(),
                                                        datacommands$dataprovider.access(p_180156_),
                                                        NbtPathArgument.getPath(p_180156_, "path"),
                                                        p_180165_ -> ByteTag.valueOf(
                                                                (byte)((int)((double)p_180165_ * DoubleArgumentType.getDouble(p_180156_, "scale")))
                                                            ),
                                                        pStoringResult
                                                    )
                                            )
                                    )
                            )
                    )
            );
        }

        return pLiteral;
    }

    private static CommandSourceStack storeValue(CommandSourceStack pSource, Collection<ScoreHolder> pTargets, Objective pObjective, boolean pStoringResult) {
        Scoreboard scoreboard = pSource.getServer().getScoreboard();
        return pSource.withCallback((p_137137_, p_137138_) -> {
            for (ScoreHolder scoreholder : pTargets) {
                ScoreAccess scoreaccess = scoreboard.getOrCreatePlayerScore(scoreholder, pObjective);
                int i = pStoringResult ? p_137138_ : (p_137137_ ? 1 : 0);
                scoreaccess.set(i);
            }
        }, CommandResultCallback::chain);
    }

    private static CommandSourceStack storeValue(CommandSourceStack pSource, CustomBossEvent pBar, boolean pStoringValue, boolean pStoringResult) {
        return pSource.withCallback((p_137186_, p_137187_) -> {
            int i = pStoringResult ? p_137187_ : (p_137186_ ? 1 : 0);
            if (pStoringValue) {
                pBar.setValue(i);
            } else {
                pBar.setMax(i);
            }
        }, CommandResultCallback::chain);
    }

    private static CommandSourceStack storeData(
        CommandSourceStack pSource, DataAccessor pAccessor, NbtPathArgument.NbtPath pPath, IntFunction<Tag> pTagConverter, boolean pStoringResult
    ) {
        return pSource.withCallback((p_137154_, p_137155_) -> {
            try {
                CompoundTag compoundtag = pAccessor.getData();
                int i = pStoringResult ? p_137155_ : (p_137154_ ? 1 : 0);
                pPath.set(compoundtag, pTagConverter.apply(i));
                pAccessor.setData(compoundtag);
            } catch (CommandSyntaxException commandsyntaxexception) {
            }
        }, CommandResultCallback::chain);
    }

    private static boolean isChunkLoaded(ServerLevel pLevel, BlockPos pPos) {
        ChunkPos chunkpos = new ChunkPos(pPos);
        LevelChunk levelchunk = pLevel.getChunkSource().getChunkNow(chunkpos.x, chunkpos.z);
        return levelchunk == null ? false : levelchunk.getFullStatus() == FullChunkStatus.ENTITY_TICKING && pLevel.areEntitiesLoaded(chunkpos.toLong());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> addConditionals(
        CommandNode<CommandSourceStack> pParent, LiteralArgumentBuilder<CommandSourceStack> pLiteral, boolean pIsIf, CommandBuildContext pContext
    ) {
        pLiteral.then(
                Commands.literal("block")
                    .then(
                        Commands.argument("pos", BlockPosArgument.blockPos())
                            .then(
                                addConditional(
                                    pParent,
                                    Commands.argument("block", BlockPredicateArgument.blockPredicate(pContext)),
                                    pIsIf,
                                    p_137277_ -> BlockPredicateArgument.getBlockPredicate(p_137277_, "block")
                                            .test(
                                                new BlockInWorld(p_137277_.getSource().getLevel(), BlockPosArgument.getLoadedBlockPos(p_137277_, "pos"), true)
                                            )
                                )
                            )
                    )
            )
            .then(
                Commands.literal("biome")
                    .then(
                        Commands.argument("pos", BlockPosArgument.blockPos())
                            .then(
                                addConditional(
                                    pParent,
                                    Commands.argument("biome", ResourceOrTagArgument.resourceOrTag(pContext, Registries.BIOME)),
                                    pIsIf,
                                    p_313477_ -> ResourceOrTagArgument.getResourceOrTag(p_313477_, "biome", Registries.BIOME)
                                            .test(p_313477_.getSource().getLevel().getBiome(BlockPosArgument.getLoadedBlockPos(p_313477_, "pos")))
                                )
                            )
                    )
            )
            .then(
                Commands.literal("loaded")
                    .then(
                        addConditional(
                            pParent,
                            Commands.argument("pos", BlockPosArgument.blockPos()),
                            pIsIf,
                            p_269757_ -> isChunkLoaded(p_269757_.getSource().getLevel(), BlockPosArgument.getBlockPos(p_269757_, "pos"))
                        )
                    )
            )
            .then(
                Commands.literal("dimension")
                    .then(
                        addConditional(
                            pParent,
                            Commands.argument("dimension", DimensionArgument.dimension()),
                            pIsIf,
                            p_264789_ -> DimensionArgument.getDimension(p_264789_, "dimension") == p_264789_.getSource().getLevel()
                        )
                    )
            )
            .then(
                Commands.literal("score")
                    .then(
                        Commands.argument("target", ScoreHolderArgument.scoreHolder())
                            .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                            .then(
                                Commands.argument("targetObjective", ObjectiveArgument.objective())
                                    .then(
                                        Commands.literal("=")
                                            .then(
                                                Commands.argument("source", ScoreHolderArgument.scoreHolder())
                                                    .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                    .then(
                                                        addConditional(
                                                            pParent,
                                                            Commands.argument("sourceObjective", ObjectiveArgument.objective()),
                                                            pIsIf,
                                                            p_313481_ -> checkScore(p_313481_, (p_313485_, p_313486_) -> p_313485_ == p_313486_)
                                                        )
                                                    )
                                            )
                                    )
                                    .then(
                                        Commands.literal("<")
                                            .then(
                                                Commands.argument("source", ScoreHolderArgument.scoreHolder())
                                                    .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                    .then(
                                                        addConditional(
                                                            pParent,
                                                            Commands.argument("sourceObjective", ObjectiveArgument.objective()),
                                                            pIsIf,
                                                            p_313476_ -> checkScore(p_313476_, (p_313478_, p_313479_) -> p_313478_ < p_313479_)
                                                        )
                                                    )
                                            )
                                    )
                                    .then(
                                        Commands.literal("<=")
                                            .then(
                                                Commands.argument("source", ScoreHolderArgument.scoreHolder())
                                                    .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                    .then(
                                                        addConditional(
                                                            pParent,
                                                            Commands.argument("sourceObjective", ObjectiveArgument.objective()),
                                                            pIsIf,
                                                            p_313482_ -> checkScore(p_313482_, (p_313473_, p_313474_) -> p_313473_ <= p_313474_)
                                                        )
                                                    )
                                            )
                                    )
                                    .then(
                                        Commands.literal(">")
                                            .then(
                                                Commands.argument("source", ScoreHolderArgument.scoreHolder())
                                                    .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                    .then(
                                                        addConditional(
                                                            pParent,
                                                            Commands.argument("sourceObjective", ObjectiveArgument.objective()),
                                                            pIsIf,
                                                            p_313475_ -> checkScore(p_313475_, (p_313487_, p_313488_) -> p_313487_ > p_313488_)
                                                        )
                                                    )
                                            )
                                    )
                                    .then(
                                        Commands.literal(">=")
                                            .then(
                                                Commands.argument("source", ScoreHolderArgument.scoreHolder())
                                                    .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                    .then(
                                                        addConditional(
                                                            pParent,
                                                            Commands.argument("sourceObjective", ObjectiveArgument.objective()),
                                                            pIsIf,
                                                            p_313480_ -> checkScore(p_313480_, (p_313483_, p_313484_) -> p_313483_ >= p_313484_)
                                                        )
                                                    )
                                            )
                                    )
                                    .then(
                                        Commands.literal("matches")
                                            .then(
                                                addConditional(
                                                    pParent,
                                                    Commands.argument("range", RangeArgument.intRange()),
                                                    pIsIf,
                                                    p_137216_ -> checkScore(p_137216_, RangeArgument.Ints.getRange(p_137216_, "range"))
                                                )
                                            )
                                    )
                            )
                    )
            )
            .then(
                Commands.literal("blocks")
                    .then(
                        Commands.argument("start", BlockPosArgument.blockPos())
                            .then(
                                Commands.argument("end", BlockPosArgument.blockPos())
                                    .then(
                                        Commands.argument("destination", BlockPosArgument.blockPos())
                                            .then(addIfBlocksConditional(pParent, Commands.literal("all"), pIsIf, false))
                                            .then(addIfBlocksConditional(pParent, Commands.literal("masked"), pIsIf, true))
                                    )
                            )
                    )
            )
            .then(
                Commands.literal("entity")
                    .then(
                        Commands.argument("entities", EntityArgument.entities())
                            .fork(pParent, p_137232_ -> expect(p_137232_, pIsIf, !EntityArgument.getOptionalEntities(p_137232_, "entities").isEmpty()))
                            .executes(createNumericConditionalHandler(pIsIf, p_137189_ -> EntityArgument.getOptionalEntities(p_137189_, "entities").size()))
                    )
            )
            .then(
                Commands.literal("predicate")
                    .then(
                        addConditional(
                            pParent,
                            Commands.argument("predicate", ResourceOrIdArgument.lootPredicate(pContext)).suggests(SUGGEST_PREDICATE),
                            pIsIf,
                            p_335217_ -> checkCustomPredicate(p_335217_.getSource(), ResourceOrIdArgument.getLootPredicate(p_335217_, "predicate"))
                        )
                    )
            )
            .then(
                Commands.literal("function")
                    .then(
                        Commands.argument("name", FunctionArgument.functions())
                            .suggests(FunctionCommand.SUGGEST_FUNCTION)
                            .fork(pParent, new ExecuteCommand.ExecuteIfFunctionCustomModifier(pIsIf))
                    )
            )
            .then(
                Commands.literal("items")
                    .then(
                        Commands.literal("entity")
                            .then(
                                Commands.argument("entities", EntityArgument.entities())
                                    .then(
                                        Commands.argument("slots", SlotsArgument.slots())
                                            .then(
                                                Commands.argument("item_predicate", ItemPredicateArgument.itemPredicate(pContext))
                                                    .fork(
                                                        pParent,
                                                        p_332574_ -> expect(
                                                                p_332574_,
                                                                pIsIf,
                                                                countItems(
                                                                        EntityArgument.getEntities(p_332574_, "entities"),
                                                                        SlotsArgument.getSlots(p_332574_, "slots"),
                                                                        ItemPredicateArgument.getItemPredicate(p_332574_, "item_predicate")
                                                                    )
                                                                    > 0
                                                            )
                                                    )
                                                    .executes(
                                                        createNumericConditionalHandler(
                                                            pIsIf,
                                                            p_332569_ -> countItems(
                                                                    EntityArgument.getEntities(p_332569_, "entities"),
                                                                    SlotsArgument.getSlots(p_332569_, "slots"),
                                                                    ItemPredicateArgument.getItemPredicate(p_332569_, "item_predicate")
                                                                )
                                                        )
                                                    )
                                            )
                                    )
                            )
                    )
                    .then(
                        Commands.literal("block")
                            .then(
                                Commands.argument("pos", BlockPosArgument.blockPos())
                                    .then(
                                        Commands.argument("slots", SlotsArgument.slots())
                                            .then(
                                                Commands.argument("item_predicate", ItemPredicateArgument.itemPredicate(pContext))
                                                    .fork(
                                                        pParent,
                                                        p_332571_ -> expect(
                                                                p_332571_,
                                                                pIsIf,
                                                                countItems(
                                                                        p_332571_.getSource(),
                                                                        BlockPosArgument.getLoadedBlockPos(p_332571_, "pos"),
                                                                        SlotsArgument.getSlots(p_332571_, "slots"),
                                                                        ItemPredicateArgument.getItemPredicate(p_332571_, "item_predicate")
                                                                    )
                                                                    > 0
                                                            )
                                                    )
                                                    .executes(
                                                        createNumericConditionalHandler(
                                                            pIsIf,
                                                            p_332572_ -> countItems(
                                                                    p_332572_.getSource(),
                                                                    BlockPosArgument.getLoadedBlockPos(p_332572_, "pos"),
                                                                    SlotsArgument.getSlots(p_332572_, "slots"),
                                                                    ItemPredicateArgument.getItemPredicate(p_332572_, "item_predicate")
                                                                )
                                                        )
                                                    )
                                            )
                                    )
                            )
                    )
            );

        for (DataCommands.DataProvider datacommands$dataprovider : DataCommands.SOURCE_PROVIDERS) {
            pLiteral.then(
                datacommands$dataprovider.wrap(
                    Commands.literal("data"),
                    p_137092_ -> p_137092_.then(
                            Commands.argument("path", NbtPathArgument.nbtPath())
                                .fork(
                                    pParent,
                                    p_180175_ -> expect(
                                            p_180175_,
                                            pIsIf,
                                            checkMatchingData(datacommands$dataprovider.access(p_180175_), NbtPathArgument.getPath(p_180175_, "path")) > 0
                                        )
                                )
                                .executes(
                                    createNumericConditionalHandler(
                                        pIsIf,
                                        p_180152_ -> checkMatchingData(datacommands$dataprovider.access(p_180152_), NbtPathArgument.getPath(p_180152_, "path"))
                                    )
                                )
                        )
                )
            );
        }

        return pLiteral;
    }

    private static int countItems(Iterable<? extends Entity> pTargets, SlotRange pSlotRange, Predicate<ItemStack> pFilter) {
        int i = 0;

        for (Entity entity : pTargets) {
            IntList intlist = pSlotRange.slots();

            for (int j = 0; j < intlist.size(); j++) {
                int k = intlist.getInt(j);
                SlotAccess slotaccess = entity.getSlot(k);
                ItemStack itemstack = slotaccess.get();
                if (pFilter.test(itemstack)) {
                    i += itemstack.getCount();
                }
            }
        }

        return i;
    }

    private static int countItems(CommandSourceStack pSource, BlockPos pPos, SlotRange pSlotRange, Predicate<ItemStack> pFilter) throws CommandSyntaxException {
        int i = 0;
        Container container = ItemCommands.getContainer(pSource, pPos, ItemCommands.ERROR_SOURCE_NOT_A_CONTAINER);
        int j = container.getContainerSize();
        IntList intlist = pSlotRange.slots();

        for (int k = 0; k < intlist.size(); k++) {
            int l = intlist.getInt(k);
            if (l >= 0 && l < j) {
                ItemStack itemstack = container.getItem(l);
                if (pFilter.test(itemstack)) {
                    i += itemstack.getCount();
                }
            }
        }

        return i;
    }

    private static Command<CommandSourceStack> createNumericConditionalHandler(boolean pIsIf, ExecuteCommand.CommandNumericPredicate pPredicate) {
        return pIsIf ? p_288391_ -> {
            int i = pPredicate.test(p_288391_);
            if (i > 0) {
                p_288391_.getSource().sendSuccess(() -> Component.translatable("commands.execute.conditional.pass_count", i), false);
                return i;
            } else {
                throw ERROR_CONDITIONAL_FAILED.create();
            }
        } : p_288393_ -> {
            int i = pPredicate.test(p_288393_);
            if (i == 0) {
                p_288393_.getSource().sendSuccess(() -> Component.translatable("commands.execute.conditional.pass"), false);
                return 1;
            } else {
                throw ERROR_CONDITIONAL_FAILED_COUNT.create(i);
            }
        };
    }

    private static int checkMatchingData(DataAccessor pAccessor, NbtPathArgument.NbtPath pPath) throws CommandSyntaxException {
        return pPath.countMatching(pAccessor.getData());
    }

    private static boolean checkScore(CommandContext<CommandSourceStack> pSource, ExecuteCommand.IntBiPredicate pPredicate) throws CommandSyntaxException {
        ScoreHolder scoreholder = ScoreHolderArgument.getName(pSource, "target");
        Objective objective = ObjectiveArgument.getObjective(pSource, "targetObjective");
        ScoreHolder scoreholder1 = ScoreHolderArgument.getName(pSource, "source");
        Objective objective1 = ObjectiveArgument.getObjective(pSource, "sourceObjective");
        Scoreboard scoreboard = pSource.getSource().getServer().getScoreboard();
        ReadOnlyScoreInfo readonlyscoreinfo = scoreboard.getPlayerScoreInfo(scoreholder, objective);
        ReadOnlyScoreInfo readonlyscoreinfo1 = scoreboard.getPlayerScoreInfo(scoreholder1, objective1);
        return readonlyscoreinfo != null && readonlyscoreinfo1 != null ? pPredicate.test(readonlyscoreinfo.value(), readonlyscoreinfo1.value()) : false;
    }

    private static boolean checkScore(CommandContext<CommandSourceStack> pContext, MinMaxBounds.Ints pBounds) throws CommandSyntaxException {
        ScoreHolder scoreholder = ScoreHolderArgument.getName(pContext, "target");
        Objective objective = ObjectiveArgument.getObjective(pContext, "targetObjective");
        Scoreboard scoreboard = pContext.getSource().getServer().getScoreboard();
        ReadOnlyScoreInfo readonlyscoreinfo = scoreboard.getPlayerScoreInfo(scoreholder, objective);
        return readonlyscoreinfo == null ? false : pBounds.matches(readonlyscoreinfo.value());
    }

    private static boolean checkCustomPredicate(CommandSourceStack pSource, Holder<LootItemCondition> pCondition) {
        ServerLevel serverlevel = pSource.getLevel();
        LootParams lootparams = new LootParams.Builder(serverlevel)
            .withParameter(LootContextParams.ORIGIN, pSource.getPosition())
            .withOptionalParameter(LootContextParams.THIS_ENTITY, pSource.getEntity())
            .create(LootContextParamSets.COMMAND);
        LootContext lootcontext = new LootContext.Builder(lootparams).create(Optional.empty());
        lootcontext.pushVisitedElement(LootContext.createVisitedEntry(pCondition.value()));
        return pCondition.value().test(lootcontext);
    }

    /**
     * If actual and expected match, returns a collection containing only the source player.
     */
    private static Collection<CommandSourceStack> expect(CommandContext<CommandSourceStack> pContext, boolean pActual, boolean pExpected) {
        return (Collection<CommandSourceStack>)(pExpected == pActual ? Collections.singleton(pContext.getSource()) : Collections.emptyList());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> addConditional(
        CommandNode<CommandSourceStack> pCommandNode,
        ArgumentBuilder<CommandSourceStack, ?> pBuilder,
        boolean pValue,
        ExecuteCommand.CommandPredicate pTest
    ) {
        return pBuilder.fork(pCommandNode, p_137214_ -> expect(p_137214_, pValue, pTest.test(p_137214_))).executes(p_288396_ -> {
            if (pValue == pTest.test(p_288396_)) {
                p_288396_.getSource().sendSuccess(() -> Component.translatable("commands.execute.conditional.pass"), false);
                return 1;
            } else {
                throw ERROR_CONDITIONAL_FAILED.create();
            }
        });
    }

    private static ArgumentBuilder<CommandSourceStack, ?> addIfBlocksConditional(
        CommandNode<CommandSourceStack> pCommandNode, ArgumentBuilder<CommandSourceStack, ?> pLiteral, boolean pIsIf, boolean pIsMasked
    ) {
        return pLiteral.fork(pCommandNode, p_137180_ -> expect(p_137180_, pIsIf, checkRegions(p_137180_, pIsMasked).isPresent()))
            .executes(pIsIf ? p_137210_ -> checkIfRegions(p_137210_, pIsMasked) : p_137165_ -> checkUnlessRegions(p_137165_, pIsMasked));
    }

    private static int checkIfRegions(CommandContext<CommandSourceStack> pContext, boolean pIsMasked) throws CommandSyntaxException {
        OptionalInt optionalint = checkRegions(pContext, pIsMasked);
        if (optionalint.isPresent()) {
            pContext.getSource().sendSuccess(() -> Component.translatable("commands.execute.conditional.pass_count", optionalint.getAsInt()), false);
            return optionalint.getAsInt();
        } else {
            throw ERROR_CONDITIONAL_FAILED.create();
        }
    }

    private static int checkUnlessRegions(CommandContext<CommandSourceStack> pContext, boolean pIsMasked) throws CommandSyntaxException {
        OptionalInt optionalint = checkRegions(pContext, pIsMasked);
        if (optionalint.isPresent()) {
            throw ERROR_CONDITIONAL_FAILED_COUNT.create(optionalint.getAsInt());
        } else {
            pContext.getSource().sendSuccess(() -> Component.translatable("commands.execute.conditional.pass"), false);
            return 1;
        }
    }

    private static OptionalInt checkRegions(CommandContext<CommandSourceStack> pContext, boolean pIsMasked) throws CommandSyntaxException {
        return checkRegions(
            pContext.getSource().getLevel(),
            BlockPosArgument.getLoadedBlockPos(pContext, "start"),
            BlockPosArgument.getLoadedBlockPos(pContext, "end"),
            BlockPosArgument.getLoadedBlockPos(pContext, "destination"),
            pIsMasked
        );
    }

    private static OptionalInt checkRegions(ServerLevel pLevel, BlockPos pBegin, BlockPos pEnd, BlockPos pDestination, boolean pIsMasked) throws CommandSyntaxException {
        BoundingBox boundingbox = BoundingBox.fromCorners(pBegin, pEnd);
        BoundingBox boundingbox1 = BoundingBox.fromCorners(pDestination, pDestination.offset(boundingbox.getLength()));
        BlockPos blockpos = new BlockPos(
            boundingbox1.minX() - boundingbox.minX(), boundingbox1.minY() - boundingbox.minY(), boundingbox1.minZ() - boundingbox.minZ()
        );
        int i = boundingbox.getXSpan() * boundingbox.getYSpan() * boundingbox.getZSpan();
        if (i > 32768) {
            throw ERROR_AREA_TOO_LARGE.create(32768, i);
        } else {
            RegistryAccess registryaccess = pLevel.registryAccess();
            int j = 0;

            for (int k = boundingbox.minZ(); k <= boundingbox.maxZ(); k++) {
                for (int l = boundingbox.minY(); l <= boundingbox.maxY(); l++) {
                    for (int i1 = boundingbox.minX(); i1 <= boundingbox.maxX(); i1++) {
                        BlockPos blockpos1 = new BlockPos(i1, l, k);
                        BlockPos blockpos2 = blockpos1.offset(blockpos);
                        BlockState blockstate = pLevel.getBlockState(blockpos1);
                        if (!pIsMasked || !blockstate.is(Blocks.AIR)) {
                            if (blockstate != pLevel.getBlockState(blockpos2)) {
                                return OptionalInt.empty();
                            }

                            BlockEntity blockentity = pLevel.getBlockEntity(blockpos1);
                            BlockEntity blockentity1 = pLevel.getBlockEntity(blockpos2);
                            if (blockentity != null) {
                                if (blockentity1 == null) {
                                    return OptionalInt.empty();
                                }

                                if (blockentity1.getType() != blockentity.getType()) {
                                    return OptionalInt.empty();
                                }

                                if (!blockentity.components().equals(blockentity1.components())) {
                                    return OptionalInt.empty();
                                }

                                CompoundTag compoundtag = blockentity.saveCustomOnly(registryaccess);
                                CompoundTag compoundtag1 = blockentity1.saveCustomOnly(registryaccess);
                                if (!compoundtag.equals(compoundtag1)) {
                                    return OptionalInt.empty();
                                }
                            }

                            j++;
                        }
                    }
                }
            }

            return OptionalInt.of(j);
        }
    }

    private static RedirectModifier<CommandSourceStack> expandOneToOneEntityRelation(Function<Entity, Optional<Entity>> pRelation) {
        return p_264786_ -> {
            CommandSourceStack commandsourcestack = p_264786_.getSource();
            Entity entity = commandsourcestack.getEntity();
            return entity == null
                ? List.of()
                : pRelation.apply(entity)
                    .filter(p_264783_ -> !p_264783_.isRemoved())
                    .map(p_264775_ -> List.of(commandsourcestack.withEntity(p_264775_)))
                    .orElse(List.of());
        };
    }

    private static RedirectModifier<CommandSourceStack> expandOneToManyEntityRelation(Function<Entity, Stream<Entity>> pRelation) {
        return p_264780_ -> {
            CommandSourceStack commandsourcestack = p_264780_.getSource();
            Entity entity = commandsourcestack.getEntity();
            return entity == null
                ? List.of()
                : pRelation.apply(entity).filter(p_264784_ -> !p_264784_.isRemoved()).map(commandsourcestack::withEntity).toList();
        };
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createRelationOperations(
        CommandNode<CommandSourceStack> pNode, LiteralArgumentBuilder<CommandSourceStack> pArgumentBuilder
    ) {
        return pArgumentBuilder.then(
                Commands.literal("owner")
                    .fork(
                        pNode,
                        expandOneToOneEntityRelation(
                            p_269758_ -> p_269758_ instanceof OwnableEntity ownableentity ? Optional.ofNullable(ownableentity.getOwner()) : Optional.empty()
                        )
                    )
            )
            .then(
                Commands.literal("leasher")
                    .fork(
                        pNode,
                        expandOneToOneEntityRelation(
                            p_353021_ -> p_353021_ instanceof Leashable leashable ? Optional.ofNullable(leashable.getLeashHolder()) : Optional.empty()
                        )
                    )
            )
            .then(
                Commands.literal("target")
                    .fork(
                        pNode,
                        expandOneToOneEntityRelation(
                            p_272389_ -> p_272389_ instanceof Targeting targeting ? Optional.ofNullable(targeting.getTarget()) : Optional.empty()
                        )
                    )
            )
            .then(
                Commands.literal("attacker")
                    .fork(
                        pNode,
                        expandOneToOneEntityRelation(
                            p_272388_ -> p_272388_ instanceof Attackable attackable ? Optional.ofNullable(attackable.getLastAttacker()) : Optional.empty()
                        )
                    )
            )
            .then(Commands.literal("vehicle").fork(pNode, expandOneToOneEntityRelation(p_264776_ -> Optional.ofNullable(p_264776_.getVehicle()))))
            .then(
                Commands.literal("controller")
                    .fork(pNode, expandOneToOneEntityRelation(p_274815_ -> Optional.ofNullable(p_274815_.getControllingPassenger())))
            )
            .then(
                Commands.literal("origin")
                    .fork(
                        pNode,
                        expandOneToOneEntityRelation(
                            p_266631_ -> p_266631_ instanceof TraceableEntity traceableentity
                                    ? Optional.ofNullable(traceableentity.getOwner())
                                    : Optional.empty()
                        )
                    )
            )
            .then(Commands.literal("passengers").fork(pNode, expandOneToManyEntityRelation(p_264777_ -> p_264777_.getPassengers().stream())));
    }

    private static CommandSourceStack spawnEntityAndRedirect(CommandSourceStack pSource, Holder.Reference<EntityType<?>> pEntityType) throws CommandSyntaxException {
        Entity entity = SummonCommand.createEntity(pSource, pEntityType, pSource.getPosition(), new CompoundTag(), true);
        return pSource.withEntity(entity);
    }

    public static <T extends ExecutionCommandSource<T>> void scheduleFunctionConditionsAndTest(
        T pOriginalSource,
        List<T> pSources,
        Function<T, T> pSourceModifier,
        IntPredicate pSuccessCheck,
        ContextChain<T> pContextChain,
        @Nullable CompoundTag pArguments,
        ExecutionControl<T> pExecutionControl,
        ExecuteCommand.CommandGetter<T, Collection<CommandFunction<T>>> pFunctions,
        ChainModifiers pChainModifiers
    ) {
        List<T> list = new ArrayList<>(pSources.size());

        Collection<CommandFunction<T>> collection;
        try {
            collection = pFunctions.get(pContextChain.getTopContext().copyFor(pOriginalSource));
        } catch (CommandSyntaxException commandsyntaxexception) {
            pOriginalSource.handleError(commandsyntaxexception, pChainModifiers.isForked(), pExecutionControl.tracer());
            return;
        }

        int i = collection.size();
        if (i != 0) {
            List<InstantiatedFunction<T>> list1 = new ArrayList<>(i);

            try {
                for (CommandFunction<T> commandfunction : collection) {
                    try {
                        list1.add(commandfunction.instantiate(pArguments, pOriginalSource.dispatcher()));
                    } catch (FunctionInstantiationException functioninstantiationexception) {
                        throw ERROR_FUNCTION_CONDITION_INSTANTATION_FAILURE.create(commandfunction.id(), functioninstantiationexception.messageComponent());
                    }
                }
            } catch (CommandSyntaxException commandsyntaxexception1) {
                pOriginalSource.handleError(commandsyntaxexception1, pChainModifiers.isForked(), pExecutionControl.tracer());
            }

            for (T t1 : pSources) {
                T t = (T)pSourceModifier.apply(t1.clearCallbacks());
                CommandResultCallback commandresultcallback = (p_309685_, p_305691_) -> {
                    if (pSuccessCheck.test(p_305691_)) {
                        list.add(t1);
                    }
                };
                pExecutionControl.queueNext(new IsolatedCall<>(p_309456_ -> {
                    for (InstantiatedFunction<T> instantiatedfunction : list1) {
                        p_309456_.queueNext(new CallFunction<>(instantiatedfunction, p_309456_.currentFrame().returnValueConsumer(), true).bind(t));
                    }

                    p_309456_.queueNext(FallthroughTask.instance());
                }, commandresultcallback));
            }

            ContextChain<T> contextchain = pContextChain.nextStage();
            String s = pContextChain.getTopContext().getInput();
            pExecutionControl.queueNext(new BuildContexts.Continuation<>(s, contextchain, pChainModifiers, pOriginalSource, list));
        }
    }

    @FunctionalInterface
    public interface CommandGetter<T, R> {
        R get(CommandContext<T> pContext) throws CommandSyntaxException;
    }

    @FunctionalInterface
    interface CommandNumericPredicate {
        int test(CommandContext<CommandSourceStack> pContext) throws CommandSyntaxException;
    }

    @FunctionalInterface
    interface CommandPredicate {
        boolean test(CommandContext<CommandSourceStack> pContext) throws CommandSyntaxException;
    }

    static class ExecuteIfFunctionCustomModifier implements CustomModifierExecutor.ModifierAdapter<CommandSourceStack> {
        private final IntPredicate check;

        ExecuteIfFunctionCustomModifier(boolean pInvert) {
            this.check = pInvert ? p_305777_ -> p_305777_ != 0 : p_306070_ -> p_306070_ == 0;
        }

        public void apply(
            CommandSourceStack pOriginalSource,
            List<CommandSourceStack> pSoruces,
            ContextChain<CommandSourceStack> pContextChain,
            ChainModifiers pChainModifiers,
            ExecutionControl<CommandSourceStack> pExecutionControl
        ) {
            ExecuteCommand.scheduleFunctionConditionsAndTest(
                pOriginalSource,
                pSoruces,
                FunctionCommand::modifySenderForExecution,
                this.check,
                pContextChain,
                null,
                pExecutionControl,
                p_305997_ -> FunctionArgument.getFunctions(p_305997_, "name"),
                pChainModifiers
            );
        }
    }

    @FunctionalInterface
    interface IntBiPredicate {
        boolean test(int pValue1, int pValue2);
    }
}
