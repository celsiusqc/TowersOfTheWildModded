package net.minecraft.world.level.storage.loot;

import com.google.common.collect.Sets;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * LootContext stores various context information for loot generation.
 * This includes the Level as well as any known {@link LootContextParam}s.
 */
public class LootContext {
    private final LootParams params;
    private final RandomSource random;
    private final HolderGetter.Provider lootDataResolver;
    private final Set<LootContext.VisitedEntry<?>> visitedElements = Sets.newLinkedHashSet();

    LootContext(LootParams pParams, RandomSource pRandom, HolderGetter.Provider pLootDataResolver) {
        this.params = pParams;
        this.random = pRandom;
        this.lootDataResolver = pLootDataResolver;
    }

    /**
     * Check whether the given parameter is present in this context.
     */
    public boolean hasParam(LootContextParam<?> pParameter) {
        return this.params.hasParam(pParameter);
    }

    /**
     * Get the value of the given parameter.
     *
     * @throws NoSuchElementException if the parameter is not present in this context
     */
    public <T> T getParam(LootContextParam<T> pParam) {
        return this.params.getParameter(pParam);
    }

    /**
     * Add the dynamic drops for the given dynamic drops name to the given consumer.
     * If no dynamic drops provider for the given name has been registered to this LootContext, nothing is generated.
     *
     * @see DynamicDrops
     */
    public void addDynamicDrops(ResourceLocation pName, Consumer<ItemStack> pConsumer) {
        this.params.addDynamicDrops(pName, pConsumer);
    }

    /**
     * Get the value of the given parameter if it is present in this context, null otherwise.
     */
    @Nullable
    public <T> T getParamOrNull(LootContextParam<T> pParameter) {
        return this.params.getParamOrNull(pParameter);
    }

    public boolean hasVisitedElement(LootContext.VisitedEntry<?> pElement) {
        return this.visitedElements.contains(pElement);
    }

    public boolean pushVisitedElement(LootContext.VisitedEntry<?> pElement) {
        return this.visitedElements.add(pElement);
    }

    public void popVisitedElement(LootContext.VisitedEntry<?> pElement) {
        this.visitedElements.remove(pElement);
    }

    public HolderGetter.Provider getResolver() {
        return this.lootDataResolver;
    }

    public RandomSource getRandom() {
        return this.random;
    }

    public float getLuck() {
        return this.params.getLuck();
    }

    public ServerLevel getLevel() {
        return this.params.getLevel();
    }

    public static LootContext.VisitedEntry<LootTable> createVisitedEntry(LootTable pLootTable) {
        return new LootContext.VisitedEntry<>(LootDataType.TABLE, pLootTable);
    }

    public static LootContext.VisitedEntry<LootItemCondition> createVisitedEntry(LootItemCondition pPredicate) {
        return new LootContext.VisitedEntry<>(LootDataType.PREDICATE, pPredicate);
    }

    public static LootContext.VisitedEntry<LootItemFunction> createVisitedEntry(LootItemFunction pModifier) {
        return new LootContext.VisitedEntry<>(LootDataType.MODIFIER, pModifier);
    }

    // Neo: Keep track of the original loot table ID through modifications
    @org.jetbrains.annotations.Nullable
    private ResourceLocation queriedLootTableId;

    private LootContext(LootParams pParams, RandomSource pRandom, HolderGetter.Provider pLootDataResolver, ResourceLocation queriedLootTableId) {
        this(pParams, pRandom, pLootDataResolver);
        this.queriedLootTableId = queriedLootTableId;
    }

    public void setQueriedLootTableId(@org.jetbrains.annotations.Nullable ResourceLocation queriedLootTableId) {
        if (this.queriedLootTableId == null && queriedLootTableId != null) this.queriedLootTableId = queriedLootTableId;
    }

    public ResourceLocation getQueriedLootTableId() {
        return this.queriedLootTableId == null ? net.neoforged.neoforge.common.loot.LootTableIdCondition.UNKNOWN_LOOT_TABLE : this.queriedLootTableId;
    }

    public static class Builder {
        private final LootParams params;
        @Nullable
        private RandomSource random;
        @Nullable
        private ResourceLocation queriedLootTableId; // Forge: correctly pass around loot table ID with copy constructor

        public Builder(LootParams pParams) {
            this.params = pParams;
        }

        public Builder(LootContext context) {
            this.params = context.params;
            this.random = context.random;
            this.queriedLootTableId = context.queriedLootTableId;
        }

        public LootContext.Builder withOptionalRandomSeed(long pSeed) {
            if (pSeed != 0L) {
                this.random = RandomSource.create(pSeed);
            }

            return this;
        }

        public LootContext.Builder withOptionalRandomSource(RandomSource p_347445_) {
            this.random = p_347445_;
            return this;
        }

        public LootContext.Builder withQueriedLootTableId(ResourceLocation queriedLootTableId) {
            this.queriedLootTableId = queriedLootTableId;
            return this;
        }

        public ServerLevel getLevel() {
            return this.params.getLevel();
        }

        public LootContext create(Optional<ResourceLocation> pSequence) {
            ServerLevel serverlevel = this.getLevel();
            MinecraftServer minecraftserver = serverlevel.getServer();
            RandomSource randomsource = Optional.ofNullable(this.random)
                .or(() -> pSequence.map(serverlevel::getRandomSequence))
                .orElseGet(serverlevel::getRandom);
            return new LootContext(this.params, randomsource, minecraftserver.reloadableRegistries().lookup(), queriedLootTableId);
        }
    }

    /**
     * Represents a type of entity that can be looked up in a {@link LootContext} using a {@link LootContextParam}.
     */
    public static enum EntityTarget implements StringRepresentable {
        /**
         * Looks up {@link LootContextParams#THIS_ENTITY}.
         */
        THIS("this", LootContextParams.THIS_ENTITY),
        ATTACKER("attacker", LootContextParams.ATTACKING_ENTITY),
        DIRECT_ATTACKER("direct_attacker", LootContextParams.DIRECT_ATTACKING_ENTITY),
        ATTACKING_PLAYER("attacking_player", LootContextParams.LAST_DAMAGE_PLAYER);

        public static final StringRepresentable.EnumCodec<LootContext.EntityTarget> CODEC = StringRepresentable.fromEnum(LootContext.EntityTarget::values);
        private final String name;
        private final LootContextParam<? extends Entity> param;

        private EntityTarget(String pName, LootContextParam<? extends Entity> pParam) {
            this.name = pName;
            this.param = pParam;
        }

        public LootContextParam<? extends Entity> getParam() {
            return this.param;
        }

        // Forge: This method is patched in to expose the same name used in getByName so that ContextNbtProvider#forEntity serializes it properly
        public String getName() {
            return this.name;
        }

        public static LootContext.EntityTarget getByName(String pName) {
            LootContext.EntityTarget lootcontext$entitytarget = CODEC.byName(pName);
            if (lootcontext$entitytarget != null) {
                return lootcontext$entitytarget;
            } else {
                throw new IllegalArgumentException("Invalid entity target " + pName);
            }
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static record VisitedEntry<T>(LootDataType<T> type, T value) {
    }
}
