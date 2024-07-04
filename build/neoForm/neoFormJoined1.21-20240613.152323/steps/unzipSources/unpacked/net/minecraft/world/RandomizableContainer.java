package net.minecraft.world;

import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public interface RandomizableContainer extends Container {
    String LOOT_TABLE_TAG = "LootTable";
    String LOOT_TABLE_SEED_TAG = "LootTableSeed";

    @Nullable
    ResourceKey<LootTable> getLootTable();

    void setLootTable(@Nullable ResourceKey<LootTable> pLootTable);

    default void setLootTable(ResourceKey<LootTable> pLootTable, long pSeed) {
        this.setLootTable(pLootTable);
        this.setLootTableSeed(pSeed);
    }

    long getLootTableSeed();

    void setLootTableSeed(long pSeed);

    BlockPos getBlockPos();

    @Nullable
    Level getLevel();

    static void setBlockEntityLootTable(BlockGetter pLevel, RandomSource pRandom, BlockPos pPs, ResourceKey<LootTable> pLootTable) {
        if (pLevel.getBlockEntity(pPs) instanceof RandomizableContainer randomizablecontainer) {
            randomizablecontainer.setLootTable(pLootTable, pRandom.nextLong());
        }
    }

    default boolean tryLoadLootTable(CompoundTag pTag) {
        if (pTag.contains("LootTable", 8)) {
            this.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(pTag.getString("LootTable"))));
            if (pTag.contains("LootTableSeed", 4)) {
                this.setLootTableSeed(pTag.getLong("LootTableSeed"));
            } else {
                this.setLootTableSeed(0L);
            }

            return true;
        } else {
            return false;
        }
    }

    default boolean trySaveLootTable(CompoundTag pTag) {
        ResourceKey<LootTable> resourcekey = this.getLootTable();
        if (resourcekey == null) {
            return false;
        } else {
            pTag.putString("LootTable", resourcekey.location().toString());
            long i = this.getLootTableSeed();
            if (i != 0L) {
                pTag.putLong("LootTableSeed", i);
            }

            return true;
        }
    }

    default void unpackLootTable(@Nullable Player pPlayer) {
        Level level = this.getLevel();
        BlockPos blockpos = this.getBlockPos();
        ResourceKey<LootTable> resourcekey = this.getLootTable();
        if (resourcekey != null && level != null && level.getServer() != null) {
            LootTable loottable = level.getServer().reloadableRegistries().getLootTable(resourcekey);
            if (pPlayer instanceof ServerPlayer) {
                CriteriaTriggers.GENERATE_LOOT.trigger((ServerPlayer)pPlayer, resourcekey);
            }

            this.setLootTable(null);
            LootParams.Builder lootparams$builder = new LootParams.Builder((ServerLevel)level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(blockpos));
            if (pPlayer != null) {
                lootparams$builder.withLuck(pPlayer.getLuck()).withParameter(LootContextParams.THIS_ENTITY, pPlayer);
            }

            loottable.fill(this, lootparams$builder.create(LootContextParamSets.CHEST), this.getLootTableSeed());
        }
    }
}
