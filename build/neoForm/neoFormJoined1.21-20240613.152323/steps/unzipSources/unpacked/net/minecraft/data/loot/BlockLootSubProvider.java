package net.minecraft.data.loot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.ItemEnchantmentsPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.ItemSubPredicates;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.ApplyExplosionDecay;
import net.minecraft.world.level.storage.loot.functions.CopyBlockState;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.FunctionUserBuilder;
import net.minecraft.world.level.storage.loot.functions.LimitCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import net.minecraft.world.level.storage.loot.predicates.ConditionUserBuilder;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public abstract class BlockLootSubProvider implements LootTableSubProvider {
    protected static final LootItemCondition.Builder HAS_SHEARS = MatchTool.toolMatches(ItemPredicate.Builder.item().of(Items.SHEARS));
    protected final HolderLookup.Provider registries;
    protected final Set<Item> explosionResistant;
    protected final FeatureFlagSet enabledFeatures;
    protected final Map<ResourceKey<LootTable>, LootTable.Builder> map;
    protected static final float[] NORMAL_LEAVES_SAPLING_CHANCES = new float[]{0.05F, 0.0625F, 0.083333336F, 0.1F};
    private static final float[] NORMAL_LEAVES_STICK_CHANCES = new float[]{0.02F, 0.022222223F, 0.025F, 0.033333335F, 0.1F};

    protected LootItemCondition.Builder hasSilkTouch() {
        HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return MatchTool.toolMatches(
            ItemPredicate.Builder.item()
                .withSubPredicate(
                    ItemSubPredicates.ENCHANTMENTS,
                    ItemEnchantmentsPredicate.enchantments(
                        List.of(new EnchantmentPredicate(registrylookup.getOrThrow(Enchantments.SILK_TOUCH), MinMaxBounds.Ints.atLeast(1)))
                    )
                )
        );
    }

    protected LootItemCondition.Builder doesNotHaveSilkTouch() {
        return this.hasSilkTouch().invert();
    }

    private LootItemCondition.Builder hasShearsOrSilkTouch() {
        return HAS_SHEARS.or(this.hasSilkTouch());
    }

    private LootItemCondition.Builder doesNotHaveShearsOrSilkTouch() {
        return this.hasShearsOrSilkTouch().invert();
    }

    protected BlockLootSubProvider(Set<Item> pExplosionResistant, FeatureFlagSet pEnabledFeatures, HolderLookup.Provider p_344943_) {
        this(pExplosionResistant, pEnabledFeatures, new HashMap<>(), p_344943_);
    }

    protected BlockLootSubProvider(
        Set<Item> pExplosionResistant, FeatureFlagSet pEnabledFeatures, Map<ResourceKey<LootTable>, LootTable.Builder> pMap, HolderLookup.Provider p_345191_
    ) {
        this.explosionResistant = pExplosionResistant;
        this.enabledFeatures = pEnabledFeatures;
        this.map = pMap;
        this.registries = p_345191_;
    }

    protected <T extends FunctionUserBuilder<T>> T applyExplosionDecay(ItemLike pItem, FunctionUserBuilder<T> pFunctionBuilder) {
        return !this.explosionResistant.contains(pItem.asItem()) ? pFunctionBuilder.apply(ApplyExplosionDecay.explosionDecay()) : pFunctionBuilder.unwrap();
    }

    protected <T extends ConditionUserBuilder<T>> T applyExplosionCondition(ItemLike pItem, ConditionUserBuilder<T> pConditionBuilder) {
        return !this.explosionResistant.contains(pItem.asItem()) ? pConditionBuilder.when(ExplosionCondition.survivesExplosion()) : pConditionBuilder.unwrap();
    }

    public LootTable.Builder createSingleItemTable(ItemLike pItem) {
        return LootTable.lootTable()
            .withPool(this.applyExplosionCondition(pItem, LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(pItem))));
    }

    /**
     * If the condition from {@code conditionBuilder} succeeds, drops 1 {@code block}.
     * Otherwise, drops loot specified by {@code alternativeBuilder}.
     */
    protected static LootTable.Builder createSelfDropDispatchTable(
        Block pBlock, LootItemCondition.Builder pConditionBuilder, LootPoolEntryContainer.Builder<?> pAlternativeBuilder
    ) {
        return LootTable.lootTable()
            .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(pBlock).when(pConditionBuilder).otherwise(pAlternativeBuilder)));
    }

    /**
     * If the block is mined with Silk Touch, drops 1 {@code block}.
     * Otherwise, drops loot specified by {@code builder}.
     */
    protected LootTable.Builder createSilkTouchDispatchTable(Block pBuilder, LootPoolEntryContainer.Builder<?> p_252089_) {
        return createSelfDropDispatchTable(pBuilder, this.hasSilkTouch(), p_252089_);
    }

    /**
     * If the block is mined with Shears, drops 1 {@code block}.
     * Otherwise, drops loot specified by {@code builder}.
     */
    protected LootTable.Builder createShearsDispatchTable(Block pBuilder, LootPoolEntryContainer.Builder<?> p_250102_) {
        return createSelfDropDispatchTable(pBuilder, HAS_SHEARS, p_250102_);
    }

    /**
     * If the block is mined either with Silk Touch or Shears, drops 1 {@code block}.
     * Otherwise, drops loot specified by {@code builder}.
     */
    protected LootTable.Builder createSilkTouchOrShearsDispatchTable(Block pBuilder, LootPoolEntryContainer.Builder<?> p_251459_) {
        return createSelfDropDispatchTable(pBuilder, this.hasShearsOrSilkTouch(), p_251459_);
    }

    protected LootTable.Builder createSingleItemTableWithSilkTouch(Block pBlock, ItemLike pItem) {
        return this.createSilkTouchDispatchTable(
            pBlock, (LootPoolEntryContainer.Builder<?>)this.applyExplosionCondition(pBlock, LootItem.lootTableItem(pItem))
        );
    }

    protected LootTable.Builder createSingleItemTable(ItemLike pItem, NumberProvider pCount) {
        return LootTable.lootTable()
            .withPool(
                LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0F))
                    .add(
                        (LootPoolEntryContainer.Builder<?>)this.applyExplosionDecay(
                            pItem, LootItem.lootTableItem(pItem).apply(SetItemCountFunction.setCount(pCount))
                        )
                    )
            );
    }

    protected LootTable.Builder createSingleItemTableWithSilkTouch(Block pBlock, ItemLike pItem, NumberProvider pCount) {
        return this.createSilkTouchDispatchTable(
            pBlock,
            (LootPoolEntryContainer.Builder<?>)this.applyExplosionDecay(
                pBlock, LootItem.lootTableItem(pItem).apply(SetItemCountFunction.setCount(pCount))
            )
        );
    }

    protected LootTable.Builder createSilkTouchOnlyTable(ItemLike p_252216_) {
        return LootTable.lootTable()
            .withPool(LootPool.lootPool().when(this.hasSilkTouch()).setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(p_252216_)));
    }

    protected LootTable.Builder createPotFlowerItemTable(ItemLike pItem) {
        return LootTable.lootTable()
            .withPool(
                this.applyExplosionCondition(
                    Blocks.FLOWER_POT, LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Blocks.FLOWER_POT))
                )
            )
            .withPool(this.applyExplosionCondition(pItem, LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(pItem))));
    }

    protected LootTable.Builder createSlabItemTable(Block pBlock) {
        return LootTable.lootTable()
            .withPool(
                LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0F))
                    .add(
                        (LootPoolEntryContainer.Builder<?>)this.applyExplosionDecay(
                            pBlock,
                            LootItem.lootTableItem(pBlock)
                                .apply(
                                    SetItemCountFunction.setCount(ConstantValue.exactly(2.0F))
                                        .when(
                                            LootItemBlockStatePropertyCondition.hasBlockStateProperties(pBlock)
                                                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(SlabBlock.TYPE, SlabType.DOUBLE))
                                        )
                                )
                        )
                    )
            );
    }

    protected <T extends Comparable<T> & StringRepresentable> LootTable.Builder createSinglePropConditionTable(
        Block pBlock, Property<T> pProperty, T pValue
    ) {
        return LootTable.lootTable()
            .withPool(
                this.applyExplosionCondition(
                    pBlock,
                    LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(
                            LootItem.lootTableItem(pBlock)
                                .when(
                                    LootItemBlockStatePropertyCondition.hasBlockStateProperties(pBlock)
                                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(pProperty, pValue))
                                )
                        )
                )
            );
    }

    protected LootTable.Builder createNameableBlockEntityTable(Block pBlock) {
        return LootTable.lootTable()
            .withPool(
                this.applyExplosionCondition(
                    pBlock,
                    LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(
                            LootItem.lootTableItem(pBlock)
                                .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY).include(DataComponents.CUSTOM_NAME))
                        )
                )
            );
    }

    protected LootTable.Builder createShulkerBoxDrop(Block pBlock) {
        return LootTable.lootTable()
            .withPool(
                this.applyExplosionCondition(
                    pBlock,
                    LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(
                            LootItem.lootTableItem(pBlock)
                                .apply(
                                    CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                                        .include(DataComponents.CUSTOM_NAME)
                                        .include(DataComponents.CONTAINER)
                                        .include(DataComponents.LOCK)
                                        .include(DataComponents.CONTAINER_LOOT)
                                )
                        )
                )
            );
    }

    protected LootTable.Builder createCopperOreDrops(Block pBlock) {
        HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return this.createSilkTouchDispatchTable(
            pBlock,
            (LootPoolEntryContainer.Builder<?>)this.applyExplosionDecay(
                pBlock,
                LootItem.lootTableItem(Items.RAW_COPPER)
                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F)))
                    .apply(ApplyBonusCount.addOreBonusCount(registrylookup.getOrThrow(Enchantments.FORTUNE)))
            )
        );
    }

    protected LootTable.Builder createLapisOreDrops(Block pBlock) {
        HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return this.createSilkTouchDispatchTable(
            pBlock,
            (LootPoolEntryContainer.Builder<?>)this.applyExplosionDecay(
                pBlock,
                LootItem.lootTableItem(Items.LAPIS_LAZULI)
                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 9.0F)))
                    .apply(ApplyBonusCount.addOreBonusCount(registrylookup.getOrThrow(Enchantments.FORTUNE)))
            )
        );
    }

    protected LootTable.Builder createRedstoneOreDrops(Block pBlock) {
        HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return this.createSilkTouchDispatchTable(
            pBlock,
            (LootPoolEntryContainer.Builder<?>)this.applyExplosionDecay(
                pBlock,
                LootItem.lootTableItem(Items.REDSTONE)
                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 5.0F)))
                    .apply(ApplyBonusCount.addUniformBonusCount(registrylookup.getOrThrow(Enchantments.FORTUNE)))
            )
        );
    }

    protected LootTable.Builder createBannerDrop(Block pBlock) {
        return LootTable.lootTable()
            .withPool(
                this.applyExplosionCondition(
                    pBlock,
                    LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(
                            LootItem.lootTableItem(pBlock)
                                .apply(
                                    CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                                        .include(DataComponents.CUSTOM_NAME)
                                        .include(DataComponents.ITEM_NAME)
                                        .include(DataComponents.HIDE_ADDITIONAL_TOOLTIP)
                                        .include(DataComponents.BANNER_PATTERNS)
                                )
                        )
                )
            );
    }

    protected LootTable.Builder createBeeNestDrop(Block p_250988_) {
        return LootTable.lootTable()
            .withPool(
                LootPool.lootPool()
                    .when(this.hasSilkTouch())
                    .setRolls(ConstantValue.exactly(1.0F))
                    .add(
                        LootItem.lootTableItem(p_250988_)
                            .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY).include(DataComponents.BEES))
                            .apply(CopyBlockState.copyState(p_250988_).copy(BeehiveBlock.HONEY_LEVEL))
                    )
            );
    }

    protected LootTable.Builder createBeeHiveDrop(Block p_248770_) {
        return LootTable.lootTable()
            .withPool(
                LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0F))
                    .add(
                        LootItem.lootTableItem(p_248770_)
                            .when(this.hasSilkTouch())
                            .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY).include(DataComponents.BEES))
                            .apply(CopyBlockState.copyState(p_248770_).copy(BeehiveBlock.HONEY_LEVEL))
                            .otherwise(LootItem.lootTableItem(p_248770_))
                    )
            );
    }

    protected LootTable.Builder createCaveVinesDrop(Block p_251070_) {
        return LootTable.lootTable()
            .withPool(
                LootPool.lootPool()
                    .add(LootItem.lootTableItem(Items.GLOW_BERRIES))
                    .when(
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(p_251070_)
                            .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(CaveVines.BERRIES, true))
                    )
            );
    }

    protected LootTable.Builder createOreDrop(Block pBlock, Item pItem) {
        HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return this.createSilkTouchDispatchTable(
            pBlock,
            (LootPoolEntryContainer.Builder<?>)this.applyExplosionDecay(
                pBlock, LootItem.lootTableItem(pItem).apply(ApplyBonusCount.addOreBonusCount(registrylookup.getOrThrow(Enchantments.FORTUNE)))
            )
        );
    }

    protected LootTable.Builder createMushroomBlockDrop(Block pBlock, ItemLike pItem) {
        return this.createSilkTouchDispatchTable(
            pBlock,
            (LootPoolEntryContainer.Builder<?>)this.applyExplosionDecay(
                pBlock,
                LootItem.lootTableItem(pItem)
                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(-6.0F, 2.0F)))
                    .apply(LimitCount.limitCount(IntRange.lowerBound(0)))
            )
        );
    }

    protected LootTable.Builder createGrassDrops(Block pBlock) {
        HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return this.createShearsDispatchTable(
            pBlock,
            (LootPoolEntryContainer.Builder<?>)this.applyExplosionDecay(
                pBlock,
                LootItem.lootTableItem(Items.WHEAT_SEEDS)
                    .when(LootItemRandomChanceCondition.randomChance(0.125F))
                    .apply(ApplyBonusCount.addUniformBonusCount(registrylookup.getOrThrow(Enchantments.FORTUNE), 2))
            )
        );
    }

    /**
     * Creates a builder that drops the given IItemProvider in amounts between 0 and 3, based on the AGE property. Only used in vanilla for pumpkin and melon stems.
     */
    public LootTable.Builder createStemDrops(Block pBlock, Item pItem) {
        return LootTable.lootTable()
            .withPool(
                this.applyExplosionDecay(
                    pBlock,
                    LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(
                            LootItem.lootTableItem(pItem)
                                .apply(
                                    StemBlock.AGE.getPossibleValues(),
                                    p_249795_ -> SetItemCountFunction.setCount(BinomialDistributionGenerator.binomial(3, (float)(p_249795_ + 1) / 15.0F))
                                            .when(
                                                LootItemBlockStatePropertyCondition.hasBlockStateProperties(pBlock)
                                                    .setProperties(
                                                        StatePropertiesPredicate.Builder.properties().hasProperty(StemBlock.AGE, p_249795_.intValue())
                                                    )
                                            )
                                )
                        )
                )
            );
    }

    public LootTable.Builder createAttachedStemDrops(Block pBlock, Item pItem) {
        return LootTable.lootTable()
            .withPool(
                this.applyExplosionDecay(
                    pBlock,
                    LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(pItem).apply(SetItemCountFunction.setCount(BinomialDistributionGenerator.binomial(3, 0.53333336F))))
                )
            );
    }

    protected static LootTable.Builder createShearsOnlyDrop(ItemLike pItem) {
        return LootTable.lootTable()
            .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).when(HAS_SHEARS).add(LootItem.lootTableItem(pItem)));
    }

    protected LootTable.Builder createMultifaceBlockDrops(Block pBlock, LootItemCondition.Builder pBuilder) {
        return LootTable.lootTable()
            .withPool(
                LootPool.lootPool()
                    .add(
                        (LootPoolEntryContainer.Builder<?>)this.applyExplosionDecay(
                            pBlock,
                            LootItem.lootTableItem(pBlock)
                                .when(pBuilder)
                                .apply(
                                    Direction.values(),
                                    p_251536_ -> SetItemCountFunction.setCount(ConstantValue.exactly(1.0F), true)
                                            .when(
                                                LootItemBlockStatePropertyCondition.hasBlockStateProperties(pBlock)
                                                    .setProperties(
                                                        StatePropertiesPredicate.Builder.properties()
                                                            .hasProperty(MultifaceBlock.getFaceProperty(p_251536_), true)
                                                    )
                                            )
                                )
                                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(-1.0F), true))
                        )
                    )
            );
    }

    /**
     * Used for all leaves, drops self with silk touch, otherwise drops the second Block param with the passed chances for fortune levels, adding in sticks.
     */
    protected LootTable.Builder createLeavesDrops(Block pLeavesBlock, Block pSaplingBlock, float... pChances) {
        HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return this.createSilkTouchOrShearsDispatchTable(
                pLeavesBlock,
                ((LootPoolSingletonContainer.Builder)this.applyExplosionCondition(pLeavesBlock, LootItem.lootTableItem(pSaplingBlock)))
                    .when(BonusLevelTableCondition.bonusLevelFlatChance(registrylookup.getOrThrow(Enchantments.FORTUNE), pChances))
            )
            .withPool(
                LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0F))
                    .when(this.doesNotHaveShearsOrSilkTouch())
                    .add(
                        ((LootPoolSingletonContainer.Builder)this.applyExplosionDecay(
                                pLeavesBlock, LootItem.lootTableItem(Items.STICK).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F)))
                            ))
                            .when(BonusLevelTableCondition.bonusLevelFlatChance(registrylookup.getOrThrow(Enchantments.FORTUNE), NORMAL_LEAVES_STICK_CHANCES))
                    )
            );
    }

    /**
     * Used for oak and dark oak, same as droppingWithChancesAndSticks but adding in apples.
     */
    protected LootTable.Builder createOakLeavesDrops(Block pOakLeavesBlock, Block pSaplingBlock, float... pChances) {
        HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return this.createLeavesDrops(pOakLeavesBlock, pSaplingBlock, pChances)
            .withPool(
                LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0F))
                    .when(this.doesNotHaveShearsOrSilkTouch())
                    .add(
                        ((LootPoolSingletonContainer.Builder)this.applyExplosionCondition(pOakLeavesBlock, LootItem.lootTableItem(Items.APPLE)))
                            .when(
                                BonusLevelTableCondition.bonusLevelFlatChance(
                                    registrylookup.getOrThrow(Enchantments.FORTUNE), 0.005F, 0.0055555557F, 0.00625F, 0.008333334F, 0.025F
                                )
                            )
                    )
            );
    }

    protected LootTable.Builder createMangroveLeavesDrops(Block pBlock) {
        HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return this.createSilkTouchOrShearsDispatchTable(
            pBlock,
            ((LootPoolSingletonContainer.Builder)this.applyExplosionDecay(
                    Blocks.MANGROVE_LEAVES, LootItem.lootTableItem(Items.STICK).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F)))
                ))
                .when(BonusLevelTableCondition.bonusLevelFlatChance(registrylookup.getOrThrow(Enchantments.FORTUNE), NORMAL_LEAVES_STICK_CHANCES))
        );
    }

    /**
     * If {@code dropGrownCropCondition} fails (i.e. crop is not ready), drops 1 {@code seedsItem}.
     * If {@code dropGrownCropCondition} succeeds (i.e. crop is ready), drops 1 {@code grownCropItem}, and 0-3 {@code seedsItem} with fortune applied.
     */
    protected LootTable.Builder createCropDrops(Block pCropBlock, Item pGrownCropItem, Item pSeedsItem, LootItemCondition.Builder pDropGrownCropCondition) {
        HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return this.applyExplosionDecay(
            pCropBlock,
            LootTable.lootTable()
                .withPool(LootPool.lootPool().add(LootItem.lootTableItem(pGrownCropItem).when(pDropGrownCropCondition).otherwise(LootItem.lootTableItem(pSeedsItem))))
                .withPool(
                    LootPool.lootPool()
                        .when(pDropGrownCropCondition)
                        .add(
                            LootItem.lootTableItem(pSeedsItem)
                                .apply(ApplyBonusCount.addBonusBinomialDistributionCount(registrylookup.getOrThrow(Enchantments.FORTUNE), 0.5714286F, 3))
                        )
                )
        );
    }

    protected LootTable.Builder createDoublePlantShearsDrop(Block p_248678_) {
        return LootTable.lootTable()
            .withPool(
                LootPool.lootPool().when(HAS_SHEARS).add(LootItem.lootTableItem(p_248678_).apply(SetItemCountFunction.setCount(ConstantValue.exactly(2.0F))))
            );
    }

    protected LootTable.Builder createDoublePlantWithSeedDrops(Block pBlock, Block pSheared) {
        LootPoolEntryContainer.Builder<?> builder = LootItem.lootTableItem(pSheared)
            .apply(SetItemCountFunction.setCount(ConstantValue.exactly(2.0F)))
            .when(HAS_SHEARS)
            .otherwise(
                ((LootPoolSingletonContainer.Builder)this.applyExplosionCondition(pBlock, LootItem.lootTableItem(Items.WHEAT_SEEDS)))
                    .when(LootItemRandomChanceCondition.randomChance(0.125F))
            );
        return LootTable.lootTable()
            .withPool(
                LootPool.lootPool()
                    .add(builder)
                    .when(
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(pBlock)
                            .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER))
                    )
                    .when(
                        LocationCheck.checkLocation(
                            LocationPredicate.Builder.location()
                                .setBlock(
                                    BlockPredicate.Builder.block()
                                        .of(pBlock)
                                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER))
                                ),
                            new BlockPos(0, 1, 0)
                        )
                    )
            )
            .withPool(
                LootPool.lootPool()
                    .add(builder)
                    .when(
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(pBlock)
                            .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER))
                    )
                    .when(
                        LocationCheck.checkLocation(
                            LocationPredicate.Builder.location()
                                .setBlock(
                                    BlockPredicate.Builder.block()
                                        .of(pBlock)
                                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER))
                                ),
                            new BlockPos(0, -1, 0)
                        )
                    )
            );
    }

    protected LootTable.Builder createCandleDrops(Block pCandleBlock) {
        return LootTable.lootTable()
            .withPool(
                LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0F))
                    .add(
                        (LootPoolEntryContainer.Builder<?>)this.applyExplosionDecay(
                            pCandleBlock,
                            LootItem.lootTableItem(pCandleBlock)
                                .apply(
                                    List.of(2, 3, 4),
                                    p_249985_ -> SetItemCountFunction.setCount(ConstantValue.exactly((float)p_249985_.intValue()))
                                            .when(
                                                LootItemBlockStatePropertyCondition.hasBlockStateProperties(pCandleBlock)
                                                    .setProperties(
                                                        StatePropertiesPredicate.Builder.properties().hasProperty(CandleBlock.CANDLES, p_249985_.intValue())
                                                    )
                                            )
                                )
                        )
                    )
            );
    }

    protected LootTable.Builder createPetalsDrops(Block pPetalBlock) {
        return LootTable.lootTable()
            .withPool(
                LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0F))
                    .add(
                        (LootPoolEntryContainer.Builder<?>)this.applyExplosionDecay(
                            pPetalBlock,
                            LootItem.lootTableItem(pPetalBlock)
                                .apply(
                                    IntStream.rangeClosed(1, 4).boxed().toList(),
                                    p_272348_ -> SetItemCountFunction.setCount(ConstantValue.exactly((float)p_272348_.intValue()))
                                            .when(
                                                LootItemBlockStatePropertyCondition.hasBlockStateProperties(pPetalBlock)
                                                    .setProperties(
                                                        StatePropertiesPredicate.Builder.properties().hasProperty(PinkPetalsBlock.AMOUNT, p_272348_.intValue())
                                                    )
                                            )
                                )
                        )
                    )
            );
    }

    protected static LootTable.Builder createCandleCakeDrops(Block pCandleCakeBlock) {
        return LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(pCandleCakeBlock)));
    }

    public static LootTable.Builder noDrop() {
        return LootTable.lootTable();
    }

    protected abstract void generate();

    protected Iterable<Block> getKnownBlocks() {
        return BuiltInRegistries.BLOCK;
    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> p_249322_) {
        this.generate();
        Set<ResourceKey<LootTable>> set = new HashSet<>();

        for(Block block : getKnownBlocks()) {
            if (block.isEnabled(this.enabledFeatures)) {
                ResourceKey<LootTable> resourcekey = block.getLootTable();
                if (resourcekey != BuiltInLootTables.EMPTY && set.add(resourcekey)) {
                    LootTable.Builder loottable$builder = this.map.remove(resourcekey);
                    if (loottable$builder == null) {
                        throw new IllegalStateException(
                            String.format(Locale.ROOT, "Missing loottable '%s' for '%s'", resourcekey.location(), BuiltInRegistries.BLOCK.getKey(block))
                        );
                    }

                    p_249322_.accept(resourcekey, loottable$builder);
                }
            }
        }

        if (!this.map.isEmpty()) {
            throw new IllegalStateException("Created block loot tables for non-blocks: " + this.map.keySet());
        }
    }

    protected void addNetherVinesDropTable(Block pVines, Block pPlant) {
        HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        LootTable.Builder loottable$builder = this.createSilkTouchOrShearsDispatchTable(
            pVines,
            LootItem.lootTableItem(pVines)
                .when(BonusLevelTableCondition.bonusLevelFlatChance(registrylookup.getOrThrow(Enchantments.FORTUNE), 0.33F, 0.55F, 0.77F, 1.0F))
        );
        this.add(pVines, loottable$builder);
        this.add(pPlant, loottable$builder);
    }

    protected LootTable.Builder createDoorTable(Block pDoorBlock) {
        return this.createSinglePropConditionTable(pDoorBlock, DoorBlock.HALF, DoubleBlockHalf.LOWER);
    }

    protected void dropPottedContents(Block pFlowerPot) {
        this.add(pFlowerPot, p_304146_ -> this.createPotFlowerItemTable(((FlowerPotBlock)p_304146_).getPotted()));
    }

    protected void otherWhenSilkTouch(Block pBlock, Block pOther) {
        this.add(pBlock, this.createSilkTouchOnlyTable(pOther));
    }

    protected void dropOther(Block pBlock, ItemLike pItem) {
        this.add(pBlock, this.createSingleItemTable(pItem));
    }

    protected void dropWhenSilkTouch(Block pBlock) {
        this.otherWhenSilkTouch(pBlock, pBlock);
    }

    protected void dropSelf(Block pBlock) {
        this.dropOther(pBlock, pBlock);
    }

    protected void add(Block pBlock, Function<Block, LootTable.Builder> pFactory) {
        this.add(pBlock, pFactory.apply(pBlock));
    }

    protected void add(Block pBlock, LootTable.Builder pBuilder) {
        this.map.put(pBlock.getLootTable(), pBuilder);
    }
}
