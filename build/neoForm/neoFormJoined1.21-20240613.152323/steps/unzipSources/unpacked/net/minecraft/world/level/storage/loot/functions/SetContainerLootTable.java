package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * LootItemFunction that sets the LootTable and optionally the loot table seed on the stack's {@code BlockEntityTag}. The effect of this is that containers such as chests will receive the given LootTable when placed.
 */
public class SetContainerLootTable extends LootItemConditionalFunction {
    public static final MapCodec<SetContainerLootTable> CODEC = RecordCodecBuilder.mapCodec(
        p_338147_ -> commonFields(p_338147_)
                .and(
                    p_338147_.group(
                        ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("name").forGetter(p_335347_ -> p_335347_.name),
                        Codec.LONG.optionalFieldOf("seed", Long.valueOf(0L)).forGetter(p_298105_ -> p_298105_.seed),
                        BuiltInRegistries.BLOCK_ENTITY_TYPE.holderByNameCodec().fieldOf("type").forGetter(p_298107_ -> p_298107_.type)
                    )
                )
                .apply(p_338147_, SetContainerLootTable::new)
    );
    private final ResourceKey<LootTable> name;
    private final long seed;
    private final Holder<BlockEntityType<?>> type;

    private SetContainerLootTable(List<LootItemCondition> p_298290_, ResourceKey<LootTable> p_335525_, long p_193047_, Holder<BlockEntityType<?>> p_298416_) {
        super(p_298290_);
        this.name = p_335525_;
        this.seed = p_193047_;
        this.type = p_298416_;
    }

    @Override
    public LootItemFunctionType<SetContainerLootTable> getType() {
        return LootItemFunctions.SET_LOOT_TABLE;
    }

    /**
     * Called to perform the actual action of this function, after conditions have been checked.
     */
    @Override
    public ItemStack run(ItemStack pStack, LootContext pContext) {
        if (pStack.isEmpty()) {
            return pStack;
        } else {
            pStack.set(DataComponents.CONTAINER_LOOT, new SeededContainerLoot(this.name, this.seed));
            return pStack;
        }
    }

    /**
     * Validate that this object is used correctly according to the given ValidationContext.
     */
    @Override
    public void validate(ValidationContext pContext) {
        super.validate(pContext);
        if (!pContext.allowsReferences()) {
            pContext.reportProblem("Uses reference to " + this.name.location() + ", but references are not allowed");
        } else {
            if (pContext.resolver().get(Registries.LOOT_TABLE, this.name).isEmpty()) {
                pContext.reportProblem("Missing loot table used for container: " + this.name.location());
            }
        }
    }

    public static LootItemConditionalFunction.Builder<?> withLootTable(BlockEntityType<?> pType, ResourceKey<LootTable> pToolTable) {
        return simpleBuilder(p_335345_ -> new SetContainerLootTable(p_335345_, pToolTable, 0L, pType.builtInRegistryHolder()));
    }

    public static LootItemConditionalFunction.Builder<?> withLootTable(BlockEntityType<?> pType, ResourceKey<LootTable> pLootTable, long pSeed) {
        return simpleBuilder(p_335351_ -> new SetContainerLootTable(p_335351_, pLootTable, pSeed, pType.builtInRegistryHolder()));
    }
}
