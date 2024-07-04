package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceOrIdArgument;
import net.minecraft.commands.arguments.SlotArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class ItemCommands {
    static final Dynamic3CommandExceptionType ERROR_TARGET_NOT_A_CONTAINER = new Dynamic3CommandExceptionType(
        (p_304251_, p_304252_, p_304253_) -> Component.translatableEscape("commands.item.target.not_a_container", p_304251_, p_304252_, p_304253_)
    );
    static final Dynamic3CommandExceptionType ERROR_SOURCE_NOT_A_CONTAINER = new Dynamic3CommandExceptionType(
        (p_304247_, p_304248_, p_304249_) -> Component.translatableEscape("commands.item.source.not_a_container", p_304247_, p_304248_, p_304249_)
    );
    static final DynamicCommandExceptionType ERROR_TARGET_INAPPLICABLE_SLOT = new DynamicCommandExceptionType(
        p_304250_ -> Component.translatableEscape("commands.item.target.no_such_slot", p_304250_)
    );
    private static final DynamicCommandExceptionType ERROR_SOURCE_INAPPLICABLE_SLOT = new DynamicCommandExceptionType(
        p_304246_ -> Component.translatableEscape("commands.item.source.no_such_slot", p_304246_)
    );
    private static final DynamicCommandExceptionType ERROR_TARGET_NO_CHANGES = new DynamicCommandExceptionType(
        p_304245_ -> Component.translatableEscape("commands.item.target.no_changes", p_304245_)
    );
    private static final Dynamic2CommandExceptionType ERROR_TARGET_NO_CHANGES_KNOWN_ITEM = new Dynamic2CommandExceptionType(
        (p_304254_, p_304255_) -> Component.translatableEscape("commands.item.target.no_changed.known_item", p_304254_, p_304255_)
    );
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_MODIFIER = (p_335220_, p_335221_) -> {
        ReloadableServerRegistries.Holder reloadableserverregistries$holder = p_335220_.getSource().getServer().reloadableRegistries();
        return SharedSuggestionProvider.suggestResource(reloadableserverregistries$holder.getKeys(Registries.ITEM_MODIFIER), p_335221_);
    };

    public static void register(CommandDispatcher<CommandSourceStack> pDispatcher, CommandBuildContext pContext) {
        pDispatcher.register(
            Commands.literal("item")
                .requires(p_180256_ -> p_180256_.hasPermission(2))
                .then(
                    Commands.literal("replace")
                        .then(
                            Commands.literal("block")
                                .then(
                                    Commands.argument("pos", BlockPosArgument.blockPos())
                                        .then(
                                            Commands.argument("slot", SlotArgument.slot())
                                                .then(
                                                    Commands.literal("with")
                                                        .then(
                                                            Commands.argument("item", ItemArgument.item(pContext))
                                                                .executes(
                                                                    p_180383_ -> setBlockItem(
                                                                            p_180383_.getSource(),
                                                                            BlockPosArgument.getLoadedBlockPos(p_180383_, "pos"),
                                                                            SlotArgument.getSlot(p_180383_, "slot"),
                                                                            ItemArgument.getItem(p_180383_, "item").createItemStack(1, false)
                                                                        )
                                                                )
                                                                .then(
                                                                    Commands.argument("count", IntegerArgumentType.integer(1, 99))
                                                                        .executes(
                                                                            p_180381_ -> setBlockItem(
                                                                                    p_180381_.getSource(),
                                                                                    BlockPosArgument.getLoadedBlockPos(p_180381_, "pos"),
                                                                                    SlotArgument.getSlot(p_180381_, "slot"),
                                                                                    ItemArgument.getItem(p_180381_, "item")
                                                                                        .createItemStack(
                                                                                            IntegerArgumentType.getInteger(p_180381_, "count"), true
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                                .then(
                                                    Commands.literal("from")
                                                        .then(
                                                            Commands.literal("block")
                                                                .then(
                                                                    Commands.argument("source", BlockPosArgument.blockPos())
                                                                        .then(
                                                                            Commands.argument("sourceSlot", SlotArgument.slot())
                                                                                .executes(
                                                                                    p_180379_ -> blockToBlock(
                                                                                            p_180379_.getSource(),
                                                                                            BlockPosArgument.getLoadedBlockPos(p_180379_, "source"),
                                                                                            SlotArgument.getSlot(p_180379_, "sourceSlot"),
                                                                                            BlockPosArgument.getLoadedBlockPos(p_180379_, "pos"),
                                                                                            SlotArgument.getSlot(p_180379_, "slot")
                                                                                        )
                                                                                )
                                                                                .then(
                                                                                    Commands.argument("modifier", ResourceOrIdArgument.lootModifier(pContext))
                                                                                        .suggests(SUGGEST_MODIFIER)
                                                                                        .executes(
                                                                                            p_335226_ -> blockToBlock(
                                                                                                    (CommandSourceStack)p_335226_.getSource(),
                                                                                                    BlockPosArgument.getLoadedBlockPos(p_335226_, "source"),
                                                                                                    SlotArgument.getSlot(p_335226_, "sourceSlot"),
                                                                                                    BlockPosArgument.getLoadedBlockPos(p_335226_, "pos"),
                                                                                                    SlotArgument.getSlot(p_335226_, "slot"),
                                                                                                    ResourceOrIdArgument.getLootModifier(p_335226_, "modifier")
                                                                                                )
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                        .then(
                                                            Commands.literal("entity")
                                                                .then(
                                                                    Commands.argument("source", EntityArgument.entity())
                                                                        .then(
                                                                            Commands.argument("sourceSlot", SlotArgument.slot())
                                                                                .executes(
                                                                                    p_180375_ -> entityToBlock(
                                                                                            p_180375_.getSource(),
                                                                                            EntityArgument.getEntity(p_180375_, "source"),
                                                                                            SlotArgument.getSlot(p_180375_, "sourceSlot"),
                                                                                            BlockPosArgument.getLoadedBlockPos(p_180375_, "pos"),
                                                                                            SlotArgument.getSlot(p_180375_, "slot")
                                                                                        )
                                                                                )
                                                                                .then(
                                                                                    Commands.argument("modifier", ResourceOrIdArgument.lootModifier(pContext))
                                                                                        .suggests(SUGGEST_MODIFIER)
                                                                                        .executes(
                                                                                            p_335223_ -> entityToBlock(
                                                                                                    (CommandSourceStack)p_335223_.getSource(),
                                                                                                    EntityArgument.getEntity(p_335223_, "source"),
                                                                                                    SlotArgument.getSlot(p_335223_, "sourceSlot"),
                                                                                                    BlockPosArgument.getLoadedBlockPos(p_335223_, "pos"),
                                                                                                    SlotArgument.getSlot(p_335223_, "slot"),
                                                                                                    ResourceOrIdArgument.getLootModifier(p_335223_, "modifier")
                                                                                                )
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(
                            Commands.literal("entity")
                                .then(
                                    Commands.argument("targets", EntityArgument.entities())
                                        .then(
                                            Commands.argument("slot", SlotArgument.slot())
                                                .then(
                                                    Commands.literal("with")
                                                        .then(
                                                            Commands.argument("item", ItemArgument.item(pContext))
                                                                .executes(
                                                                    p_180371_ -> setEntityItem(
                                                                            p_180371_.getSource(),
                                                                            EntityArgument.getEntities(p_180371_, "targets"),
                                                                            SlotArgument.getSlot(p_180371_, "slot"),
                                                                            ItemArgument.getItem(p_180371_, "item").createItemStack(1, false)
                                                                        )
                                                                )
                                                                .then(
                                                                    Commands.argument("count", IntegerArgumentType.integer(1, 99))
                                                                        .executes(
                                                                            p_180369_ -> setEntityItem(
                                                                                    p_180369_.getSource(),
                                                                                    EntityArgument.getEntities(p_180369_, "targets"),
                                                                                    SlotArgument.getSlot(p_180369_, "slot"),
                                                                                    ItemArgument.getItem(p_180369_, "item")
                                                                                        .createItemStack(
                                                                                            IntegerArgumentType.getInteger(p_180369_, "count"), true
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                                .then(
                                                    Commands.literal("from")
                                                        .then(
                                                            Commands.literal("block")
                                                                .then(
                                                                    Commands.argument("source", BlockPosArgument.blockPos())
                                                                        .then(
                                                                            Commands.argument("sourceSlot", SlotArgument.slot())
                                                                                .executes(
                                                                                    p_180367_ -> blockToEntities(
                                                                                            p_180367_.getSource(),
                                                                                            BlockPosArgument.getLoadedBlockPos(p_180367_, "source"),
                                                                                            SlotArgument.getSlot(p_180367_, "sourceSlot"),
                                                                                            EntityArgument.getEntities(p_180367_, "targets"),
                                                                                            SlotArgument.getSlot(p_180367_, "slot")
                                                                                        )
                                                                                )
                                                                                .then(
                                                                                    Commands.argument("modifier", ResourceOrIdArgument.lootModifier(pContext))
                                                                                        .suggests(SUGGEST_MODIFIER)
                                                                                        .executes(
                                                                                            p_335224_ -> blockToEntities(
                                                                                                    (CommandSourceStack)p_335224_.getSource(),
                                                                                                    BlockPosArgument.getLoadedBlockPos(p_335224_, "source"),
                                                                                                    SlotArgument.getSlot(p_335224_, "sourceSlot"),
                                                                                                    EntityArgument.getEntities(p_335224_, "targets"),
                                                                                                    SlotArgument.getSlot(p_335224_, "slot"),
                                                                                                    ResourceOrIdArgument.getLootModifier(p_335224_, "modifier")
                                                                                                )
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                        .then(
                                                            Commands.literal("entity")
                                                                .then(
                                                                    Commands.argument("source", EntityArgument.entity())
                                                                        .then(
                                                                            Commands.argument("sourceSlot", SlotArgument.slot())
                                                                                .executes(
                                                                                    p_180363_ -> entityToEntities(
                                                                                            p_180363_.getSource(),
                                                                                            EntityArgument.getEntity(p_180363_, "source"),
                                                                                            SlotArgument.getSlot(p_180363_, "sourceSlot"),
                                                                                            EntityArgument.getEntities(p_180363_, "targets"),
                                                                                            SlotArgument.getSlot(p_180363_, "slot")
                                                                                        )
                                                                                )
                                                                                .then(
                                                                                    Commands.argument("modifier", ResourceOrIdArgument.lootModifier(pContext))
                                                                                        .suggests(SUGGEST_MODIFIER)
                                                                                        .executes(
                                                                                            p_335225_ -> entityToEntities(
                                                                                                    (CommandSourceStack)p_335225_.getSource(),
                                                                                                    EntityArgument.getEntity(p_335225_, "source"),
                                                                                                    SlotArgument.getSlot(p_335225_, "sourceSlot"),
                                                                                                    EntityArgument.getEntities(p_335225_, "targets"),
                                                                                                    SlotArgument.getSlot(p_335225_, "slot"),
                                                                                                    ResourceOrIdArgument.getLootModifier(p_335225_, "modifier")
                                                                                                )
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
                .then(
                    Commands.literal("modify")
                        .then(
                            Commands.literal("block")
                                .then(
                                    Commands.argument("pos", BlockPosArgument.blockPos())
                                        .then(
                                            Commands.argument("slot", SlotArgument.slot())
                                                .then(
                                                    Commands.argument("modifier", ResourceOrIdArgument.lootModifier(pContext))
                                                        .suggests(SUGGEST_MODIFIER)
                                                        .executes(
                                                            p_335222_ -> modifyBlockItem(
                                                                    (CommandSourceStack)p_335222_.getSource(),
                                                                    BlockPosArgument.getLoadedBlockPos(p_335222_, "pos"),
                                                                    SlotArgument.getSlot(p_335222_, "slot"),
                                                                    ResourceOrIdArgument.getLootModifier(p_335222_, "modifier")
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(
                            Commands.literal("entity")
                                .then(
                                    Commands.argument("targets", EntityArgument.entities())
                                        .then(
                                            Commands.argument("slot", SlotArgument.slot())
                                                .then(
                                                    Commands.argument("modifier", ResourceOrIdArgument.lootModifier(pContext))
                                                        .suggests(SUGGEST_MODIFIER)
                                                        .executes(
                                                            p_335227_ -> modifyEntityItem(
                                                                    (CommandSourceStack)p_335227_.getSource(),
                                                                    EntityArgument.getEntities(p_335227_, "targets"),
                                                                    SlotArgument.getSlot(p_335227_, "slot"),
                                                                    ResourceOrIdArgument.getLootModifier(p_335227_, "modifier")
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static int modifyBlockItem(CommandSourceStack pSource, BlockPos pPos, int pSlot, Holder<LootItemFunction> pModifier) throws CommandSyntaxException {
        Container container = getContainer(pSource, pPos, ERROR_TARGET_NOT_A_CONTAINER);
        if (pSlot >= 0 && pSlot < container.getContainerSize()) {
            ItemStack itemstack = applyModifier(pSource, pModifier, container.getItem(pSlot));
            container.setItem(pSlot, itemstack);
            pSource.sendSuccess(
                () -> Component.translatable(
                        "commands.item.block.set.success", pPos.getX(), pPos.getY(), pPos.getZ(), itemstack.getDisplayName()
                    ),
                true
            );
            return 1;
        } else {
            throw ERROR_TARGET_INAPPLICABLE_SLOT.create(pSlot);
        }
    }

    private static int modifyEntityItem(CommandSourceStack pSource, Collection<? extends Entity> pTargets, int pSourceSlot, Holder<LootItemFunction> pModifer) throws CommandSyntaxException {
        Map<Entity, ItemStack> map = Maps.newHashMapWithExpectedSize(pTargets.size());

        for (Entity entity : pTargets) {
            SlotAccess slotaccess = entity.getSlot(pSourceSlot);
            if (slotaccess != SlotAccess.NULL) {
                ItemStack itemstack = applyModifier(pSource, pModifer, slotaccess.get().copy());
                if (slotaccess.set(itemstack)) {
                    map.put(entity, itemstack);
                    if (entity instanceof ServerPlayer) {
                        ((ServerPlayer)entity).containerMenu.broadcastChanges();
                    }
                }
            }
        }

        if (map.isEmpty()) {
            throw ERROR_TARGET_NO_CHANGES.create(pSourceSlot);
        } else {
            if (map.size() == 1) {
                Entry<Entity, ItemStack> entry = map.entrySet().iterator().next();
                pSource.sendSuccess(
                    () -> Component.translatable("commands.item.entity.set.success.single", entry.getKey().getDisplayName(), entry.getValue().getDisplayName()),
                    true
                );
            } else {
                pSource.sendSuccess(() -> Component.translatable("commands.item.entity.set.success.multiple", map.size()), true);
            }

            return map.size();
        }
    }

    private static int setBlockItem(CommandSourceStack pSource, BlockPos pPos, int pSlot, ItemStack pItem) throws CommandSyntaxException {
        Container container = getContainer(pSource, pPos, ERROR_TARGET_NOT_A_CONTAINER);
        if (pSlot >= 0 && pSlot < container.getContainerSize()) {
            container.setItem(pSlot, pItem);
            pSource.sendSuccess(
                () -> Component.translatable(
                        "commands.item.block.set.success", pPos.getX(), pPos.getY(), pPos.getZ(), pItem.getDisplayName()
                    ),
                true
            );
            return 1;
        } else {
            throw ERROR_TARGET_INAPPLICABLE_SLOT.create(pSlot);
        }
    }

    static Container getContainer(CommandSourceStack pSource, BlockPos pPos, Dynamic3CommandExceptionType pException) throws CommandSyntaxException {
        BlockEntity blockentity = pSource.getLevel().getBlockEntity(pPos);
        if (!(blockentity instanceof Container)) {
            throw pException.create(pPos.getX(), pPos.getY(), pPos.getZ());
        } else {
            return (Container)blockentity;
        }
    }

    private static int setEntityItem(CommandSourceStack pSource, Collection<? extends Entity> pTargets, int pSlot, ItemStack pItem) throws CommandSyntaxException {
        List<Entity> list = Lists.newArrayListWithCapacity(pTargets.size());

        for (Entity entity : pTargets) {
            SlotAccess slotaccess = entity.getSlot(pSlot);
            if (slotaccess != SlotAccess.NULL && slotaccess.set(pItem.copy())) {
                list.add(entity);
                if (entity instanceof ServerPlayer) {
                    ((ServerPlayer)entity).containerMenu.broadcastChanges();
                }
            }
        }

        if (list.isEmpty()) {
            throw ERROR_TARGET_NO_CHANGES_KNOWN_ITEM.create(pItem.getDisplayName(), pSlot);
        } else {
            if (list.size() == 1) {
                pSource.sendSuccess(
                    () -> Component.translatable("commands.item.entity.set.success.single", list.iterator().next().getDisplayName(), pItem.getDisplayName()),
                    true
                );
            } else {
                pSource.sendSuccess(() -> Component.translatable("commands.item.entity.set.success.multiple", list.size(), pItem.getDisplayName()), true);
            }

            return list.size();
        }
    }

    private static int blockToEntities(CommandSourceStack pSource, BlockPos pPos, int pSourceSlot, Collection<? extends Entity> pTargets, int pSlot) throws CommandSyntaxException {
        return setEntityItem(pSource, pTargets, pSlot, getBlockItem(pSource, pPos, pSourceSlot));
    }

    private static int blockToEntities(
        CommandSourceStack pSource,
        BlockPos pPos,
        int pSourceSlot,
        Collection<? extends Entity> pTargets,
        int pSlot,
        Holder<LootItemFunction> pModifier
    ) throws CommandSyntaxException {
        return setEntityItem(pSource, pTargets, pSlot, applyModifier(pSource, pModifier, getBlockItem(pSource, pPos, pSourceSlot)));
    }

    private static int blockToBlock(CommandSourceStack pSource, BlockPos pSourcePos, int pSourceSlot, BlockPos pPos, int pSlot) throws CommandSyntaxException {
        return setBlockItem(pSource, pPos, pSlot, getBlockItem(pSource, pSourcePos, pSourceSlot));
    }

    private static int blockToBlock(
        CommandSourceStack pSource, BlockPos pSourcePos, int pSourceSlot, BlockPos pPos, int pSlot, Holder<LootItemFunction> pModifier
    ) throws CommandSyntaxException {
        return setBlockItem(pSource, pPos, pSlot, applyModifier(pSource, pModifier, getBlockItem(pSource, pSourcePos, pSourceSlot)));
    }

    private static int entityToBlock(CommandSourceStack pSource, Entity pSourceEntity, int pSourceSlot, BlockPos pPos, int pSlot) throws CommandSyntaxException {
        return setBlockItem(pSource, pPos, pSlot, getEntityItem(pSourceEntity, pSourceSlot));
    }

    private static int entityToBlock(
        CommandSourceStack pSource, Entity pSourceEntity, int pSourceSlot, BlockPos pPos, int pSlot, Holder<LootItemFunction> pModifier
    ) throws CommandSyntaxException {
        return setBlockItem(pSource, pPos, pSlot, applyModifier(pSource, pModifier, getEntityItem(pSourceEntity, pSourceSlot)));
    }

    private static int entityToEntities(CommandSourceStack pSource, Entity pSourceEntity, int pSourceSlot, Collection<? extends Entity> pTargets, int pSlot) throws CommandSyntaxException {
        return setEntityItem(pSource, pTargets, pSlot, getEntityItem(pSourceEntity, pSourceSlot));
    }

    private static int entityToEntities(
        CommandSourceStack pSource,
        Entity pSourceEntity,
        int pSourceSlot,
        Collection<? extends Entity> pTargets,
        int pSlot,
        Holder<LootItemFunction> pModifier
    ) throws CommandSyntaxException {
        return setEntityItem(pSource, pTargets, pSlot, applyModifier(pSource, pModifier, getEntityItem(pSourceEntity, pSourceSlot)));
    }

    private static ItemStack applyModifier(CommandSourceStack pSource, Holder<LootItemFunction> pModifier, ItemStack pStack) {
        ServerLevel serverlevel = pSource.getLevel();
        LootParams lootparams = new LootParams.Builder(serverlevel)
            .withParameter(LootContextParams.ORIGIN, pSource.getPosition())
            .withOptionalParameter(LootContextParams.THIS_ENTITY, pSource.getEntity())
            .create(LootContextParamSets.COMMAND);
        LootContext lootcontext = new LootContext.Builder(lootparams).create(Optional.empty());
        lootcontext.pushVisitedElement(LootContext.createVisitedEntry(pModifier.value()));
        ItemStack itemstack = pModifier.value().apply(pStack, lootcontext);
        itemstack.limitSize(itemstack.getMaxStackSize());
        return itemstack;
    }

    private static ItemStack getEntityItem(Entity pEntity, int pSlot) throws CommandSyntaxException {
        SlotAccess slotaccess = pEntity.getSlot(pSlot);
        if (slotaccess == SlotAccess.NULL) {
            throw ERROR_SOURCE_INAPPLICABLE_SLOT.create(pSlot);
        } else {
            return slotaccess.get().copy();
        }
    }

    private static ItemStack getBlockItem(CommandSourceStack pSource, BlockPos pPos, int pSlot) throws CommandSyntaxException {
        Container container = getContainer(pSource, pPos, ERROR_SOURCE_NOT_A_CONTAINER);
        if (pSlot >= 0 && pSlot < container.getContainerSize()) {
            return container.getItem(pSlot).copy();
        } else {
            throw ERROR_SOURCE_INAPPLICABLE_SLOT.create(pSlot);
        }
    }
}
