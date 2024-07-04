package net.minecraft.world.level.block.grower;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public final class TreeGrower {
    private static final Map<String, TreeGrower> GROWERS = new Object2ObjectArrayMap<>();
    public static final Codec<TreeGrower> CODEC = Codec.stringResolver(p_304625_ -> p_304625_.name, GROWERS::get);
    public static final TreeGrower OAK = new TreeGrower(
        "oak",
        0.1F,
        Optional.empty(),
        Optional.empty(),
        Optional.of(TreeFeatures.OAK),
        Optional.of(TreeFeatures.FANCY_OAK),
        Optional.of(TreeFeatures.OAK_BEES_005),
        Optional.of(TreeFeatures.FANCY_OAK_BEES_005)
    );
    public static final TreeGrower SPRUCE = new TreeGrower(
        "spruce",
        0.5F,
        Optional.of(TreeFeatures.MEGA_SPRUCE),
        Optional.of(TreeFeatures.MEGA_PINE),
        Optional.of(TreeFeatures.SPRUCE),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
    );
    public static final TreeGrower MANGROVE = new TreeGrower(
        "mangrove",
        0.85F,
        Optional.empty(),
        Optional.empty(),
        Optional.of(TreeFeatures.MANGROVE),
        Optional.of(TreeFeatures.TALL_MANGROVE),
        Optional.empty(),
        Optional.empty()
    );
    public static final TreeGrower AZALEA = new TreeGrower("azalea", Optional.empty(), Optional.of(TreeFeatures.AZALEA_TREE), Optional.empty());
    public static final TreeGrower BIRCH = new TreeGrower("birch", Optional.empty(), Optional.of(TreeFeatures.BIRCH), Optional.of(TreeFeatures.BIRCH_BEES_005));
    public static final TreeGrower JUNGLE = new TreeGrower(
        "jungle", Optional.of(TreeFeatures.MEGA_JUNGLE_TREE), Optional.of(TreeFeatures.JUNGLE_TREE_NO_VINE), Optional.empty()
    );
    public static final TreeGrower ACACIA = new TreeGrower("acacia", Optional.empty(), Optional.of(TreeFeatures.ACACIA), Optional.empty());
    public static final TreeGrower CHERRY = new TreeGrower(
        "cherry", Optional.empty(), Optional.of(TreeFeatures.CHERRY), Optional.of(TreeFeatures.CHERRY_BEES_005)
    );
    public static final TreeGrower DARK_OAK = new TreeGrower("dark_oak", Optional.of(TreeFeatures.DARK_OAK), Optional.empty(), Optional.empty());
    private final String name;
    private final float secondaryChance;
    private final Optional<ResourceKey<ConfiguredFeature<?, ?>>> megaTree;
    private final Optional<ResourceKey<ConfiguredFeature<?, ?>>> secondaryMegaTree;
    private final Optional<ResourceKey<ConfiguredFeature<?, ?>>> tree;
    private final Optional<ResourceKey<ConfiguredFeature<?, ?>>> secondaryTree;
    private final Optional<ResourceKey<ConfiguredFeature<?, ?>>> flowers;
    private final Optional<ResourceKey<ConfiguredFeature<?, ?>>> secondaryFlowers;

    public TreeGrower(
        String pName,
        Optional<ResourceKey<ConfiguredFeature<?, ?>>> pMegaTree,
        Optional<ResourceKey<ConfiguredFeature<?, ?>>> pTree,
        Optional<ResourceKey<ConfiguredFeature<?, ?>>> pFlowers
    ) {
        this(pName, 0.0F, pMegaTree, Optional.empty(), pTree, Optional.empty(), pFlowers, Optional.empty());
    }

    public TreeGrower(
        String pName,
        float pSecondaryChance,
        Optional<ResourceKey<ConfiguredFeature<?, ?>>> pMegaTree,
        Optional<ResourceKey<ConfiguredFeature<?, ?>>> pSecondaryMegaTree,
        Optional<ResourceKey<ConfiguredFeature<?, ?>>> pTree,
        Optional<ResourceKey<ConfiguredFeature<?, ?>>> pSecondaryTree,
        Optional<ResourceKey<ConfiguredFeature<?, ?>>> pFlowers,
        Optional<ResourceKey<ConfiguredFeature<?, ?>>> pSecondaryFlowers
    ) {
        this.name = pName;
        this.secondaryChance = pSecondaryChance;
        this.megaTree = pMegaTree;
        this.secondaryMegaTree = pSecondaryMegaTree;
        this.tree = pTree;
        this.secondaryTree = pSecondaryTree;
        this.flowers = pFlowers;
        this.secondaryFlowers = pSecondaryFlowers;
        GROWERS.put(pName, this);
    }

    @Nullable
    private ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource pRandom, boolean pFlowers) {
        if (pRandom.nextFloat() < this.secondaryChance) {
            if (pFlowers && this.secondaryFlowers.isPresent()) {
                return this.secondaryFlowers.get();
            }

            if (this.secondaryTree.isPresent()) {
                return this.secondaryTree.get();
            }
        }

        return pFlowers && this.flowers.isPresent() ? this.flowers.get() : this.tree.orElse(null);
    }

    @Nullable
    private ResourceKey<ConfiguredFeature<?, ?>> getConfiguredMegaFeature(RandomSource pRandom) {
        return this.secondaryMegaTree.isPresent() && pRandom.nextFloat() < this.secondaryChance ? this.secondaryMegaTree.get() : this.megaTree.orElse(null);
    }

    public boolean growTree(ServerLevel pLevel, ChunkGenerator pChunkGenerator, BlockPos pPos, BlockState pState, RandomSource pRandom) {
        ResourceKey<ConfiguredFeature<?, ?>> resourcekey = this.getConfiguredMegaFeature(pRandom);
        if (resourcekey != null) {
            Holder<ConfiguredFeature<?, ?>> holder = pLevel.registryAccess()
                .registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getHolder(resourcekey)
                .orElse(null);
            var event = net.neoforged.neoforge.event.EventHooks.fireBlockGrowFeature(pLevel, pRandom, pPos, holder);
            holder = event.getFeature();
            if (event.isCanceled()) return false;
            if (holder != null) {
                for (int i = 0; i >= -1; i--) {
                    for (int j = 0; j >= -1; j--) {
                        if (isTwoByTwoSapling(pState, pLevel, pPos, i, j)) {
                            ConfiguredFeature<?, ?> configuredfeature = holder.value();
                            BlockState blockstate = Blocks.AIR.defaultBlockState();
                            pLevel.setBlock(pPos.offset(i, 0, j), blockstate, 4);
                            pLevel.setBlock(pPos.offset(i + 1, 0, j), blockstate, 4);
                            pLevel.setBlock(pPos.offset(i, 0, j + 1), blockstate, 4);
                            pLevel.setBlock(pPos.offset(i + 1, 0, j + 1), blockstate, 4);
                            if (configuredfeature.place(pLevel, pChunkGenerator, pRandom, pPos.offset(i, 0, j))) {
                                return true;
                            }

                            pLevel.setBlock(pPos.offset(i, 0, j), pState, 4);
                            pLevel.setBlock(pPos.offset(i + 1, 0, j), pState, 4);
                            pLevel.setBlock(pPos.offset(i, 0, j + 1), pState, 4);
                            pLevel.setBlock(pPos.offset(i + 1, 0, j + 1), pState, 4);
                            return false;
                        }
                    }
                }
            }
        }

        ResourceKey<ConfiguredFeature<?, ?>> resourcekey1 = this.getConfiguredFeature(pRandom, this.hasFlowers(pLevel, pPos));
        if (resourcekey1 == null) {
            return false;
        } else {
            Holder<ConfiguredFeature<?, ?>> holder1 = pLevel.registryAccess()
                .registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getHolder(resourcekey1)
                .orElse(null);
            var event = net.neoforged.neoforge.event.EventHooks.fireBlockGrowFeature(pLevel, pRandom, pPos, holder1);
            holder1 = event.getFeature();
            if (event.isCanceled()) return false;
            if (holder1 == null) {
                return false;
            } else {
                ConfiguredFeature<?, ?> configuredfeature1 = holder1.value();
                BlockState blockstate1 = pLevel.getFluidState(pPos).createLegacyBlock();
                pLevel.setBlock(pPos, blockstate1, 4);
                if (configuredfeature1.place(pLevel, pChunkGenerator, pRandom, pPos)) {
                    if (pLevel.getBlockState(pPos) == blockstate1) {
                        pLevel.sendBlockUpdated(pPos, pState, blockstate1, 2);
                    }

                    return true;
                } else {
                    pLevel.setBlock(pPos, pState, 4);
                    return false;
                }
            }
        }
    }

    private static boolean isTwoByTwoSapling(BlockState pState, BlockGetter pLevel, BlockPos pPos, int pXOffset, int pYOffset) {
        Block block = pState.getBlock();
        return pLevel.getBlockState(pPos.offset(pXOffset, 0, pYOffset)).is(block)
            && pLevel.getBlockState(pPos.offset(pXOffset + 1, 0, pYOffset)).is(block)
            && pLevel.getBlockState(pPos.offset(pXOffset, 0, pYOffset + 1)).is(block)
            && pLevel.getBlockState(pPos.offset(pXOffset + 1, 0, pYOffset + 1)).is(block);
    }

    private boolean hasFlowers(LevelAccessor pLevel, BlockPos pPos) {
        for (BlockPos blockpos : BlockPos.MutableBlockPos.betweenClosed(pPos.below().north(2).west(2), pPos.above().south(2).east(2))) {
            if (pLevel.getBlockState(blockpos).is(BlockTags.FLOWERS)) {
                return true;
            }
        }

        return false;
    }
}
