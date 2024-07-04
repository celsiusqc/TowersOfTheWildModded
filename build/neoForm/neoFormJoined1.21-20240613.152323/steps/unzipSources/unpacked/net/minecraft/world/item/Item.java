package net.minecraft.world.item;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class Item implements FeatureElement, ItemLike, net.neoforged.neoforge.common.extensions.IItemExtension {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Map<Block, Item> BY_BLOCK = net.neoforged.neoforge.registries.GameData.getBlockItemMap();
    public static final ResourceLocation BASE_ATTACK_DAMAGE_ID = ResourceLocation.withDefaultNamespace("base_attack_damage");
    public static final ResourceLocation BASE_ATTACK_SPEED_ID = ResourceLocation.withDefaultNamespace("base_attack_speed");
    public static final int DEFAULT_MAX_STACK_SIZE = 64;
    public static final int ABSOLUTE_MAX_STACK_SIZE = 99;
    public static final int MAX_BAR_WIDTH = 13;
    private final Holder.Reference<Item> builtInRegistryHolder = BuiltInRegistries.ITEM.createIntrusiveHolder(this);
    private DataComponentMap components;
    @Nullable
    private final Item craftingRemainingItem;
    @Nullable
    private String descriptionId;
    private final FeatureFlagSet requiredFeatures;

    public static int getId(Item pItem) {
        return pItem == null ? 0 : BuiltInRegistries.ITEM.getId(pItem);
    }

    public static Item byId(int pId) {
        return BuiltInRegistries.ITEM.byId(pId);
    }

    @Deprecated
    public static Item byBlock(Block pBlock) {
        return BY_BLOCK.getOrDefault(pBlock, Items.AIR);
    }

    public Item(Item.Properties pProperties) {
        this.components = pProperties.buildAndValidateComponents();
        this.craftingRemainingItem = pProperties.craftingRemainingItem;
        this.requiredFeatures = pProperties.requiredFeatures;
        if (SharedConstants.IS_RUNNING_IN_IDE && false) {
            String s = this.getClass().getSimpleName();
            if (!s.endsWith("Item")) {
                LOGGER.error("Item classes should end with Item and {} doesn't.", s);
            }
        }
        this.canRepair = pProperties.canRepair;
        initClient();
    }

    @Deprecated
    public Holder.Reference<Item> builtInRegistryHolder() {
        return this.builtInRegistryHolder;
    }

    public DataComponentMap components() {
        return this.components;
    }

    /** @deprecated Neo: do not use, use {@link net.neoforged.neoforge.event.ModifyDefaultComponentsEvent the event} instead */
    @org.jetbrains.annotations.ApiStatus.Internal @Deprecated
    public void modifyDefaultComponentsFrom(net.minecraft.core.component.DataComponentPatch patch) {
        if (!net.neoforged.neoforge.internal.RegistrationEvents.canModifyComponents()) throw new IllegalStateException("Default components cannot be modified now!");
        var builder = DataComponentMap.builder().addAll(components);
        patch.entrySet().forEach(entry -> builder.set((DataComponentType)entry.getKey(), entry.getValue().orElse(null)));
        components = Properties.COMPONENT_INTERNER.intern(Properties.validateComponents(builder.build()));
    }

    public int getDefaultMaxStackSize() {
        return this.components.getOrDefault(DataComponents.MAX_STACK_SIZE, 1);
    }

    /**
     * Called as the item is being used by an entity.
     */
    public void onUseTick(Level pLevel, LivingEntity pLivingEntity, ItemStack pStack, int pRemainingUseDuration) {
    }

    /**
 * @deprecated Forge: {@link IItemExtension#onDestroyed(ItemEntity, DamageSource)
 *             Use damage source sensitive version}
 */
    @Deprecated
    public void onDestroyed(ItemEntity pItemEntity) {
    }

    public void verifyComponentsAfterLoad(ItemStack pStack) {
    }

    public boolean canAttackBlock(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
        return true;
    }

    @Override
    public Item asItem() {
        return this;
    }

    /**
     * Called when this item is used when targeting a Block
     */
    public InteractionResult useOn(UseOnContext pContext) {
        return InteractionResult.PASS;
    }

    public float getDestroySpeed(ItemStack pStack, BlockState pState) {
        Tool tool = pStack.get(DataComponents.TOOL);
        return tool != null ? tool.getMiningSpeed(pState) : 1.0F;
    }

    /**
     * Called to trigger the item's "innate" right click behavior. To handle when this item is used on a Block, see {@link net.minecraft.world.item.Item#useOn(net.minecraft.world.item.context.UseOnContext)}.
     */
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pUsedHand);
        FoodProperties foodproperties = itemstack.getFoodProperties(pPlayer);
        if (foodproperties != null) {
            if (pPlayer.canEat(foodproperties.canAlwaysEat())) {
                pPlayer.startUsingItem(pUsedHand);
                return InteractionResultHolder.consume(itemstack);
            } else {
                return InteractionResultHolder.fail(itemstack);
            }
        } else {
            return InteractionResultHolder.pass(pPlayer.getItemInHand(pUsedHand));
        }
    }

    /**
     * Called when the player finishes using this Item (E.g. finishes eating.). Not called when the player stops using the Item before the action is complete.
     */
    public ItemStack finishUsingItem(ItemStack pStack, Level pLevel, LivingEntity pLivingEntity) {
        FoodProperties foodproperties = pStack.getFoodProperties(pLivingEntity);
        return foodproperties != null ? pLivingEntity.eat(pLevel, pStack, foodproperties) : pStack;
    }

    public boolean isBarVisible(ItemStack pStack) {
        return pStack.isDamaged();
    }

    public int getBarWidth(ItemStack pStack) {
        return Math.round(13.0F - (float)pStack.getDamageValue() * 13.0F / (float)this.getMaxDamage(pStack));
    }

    public int getBarColor(ItemStack pStack) {
        int i = pStack.getMaxDamage();
        float stackMaxDamage = this.getMaxDamage(pStack);
        float f = Math.max(0.0F, (stackMaxDamage - (float)pStack.getDamageValue()) / stackMaxDamage);
        return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
    }

    public boolean overrideStackedOnOther(ItemStack pStack, Slot pSlot, ClickAction pAction, Player pPlayer) {
        return false;
    }

    public boolean overrideOtherStackedOnMe(
        ItemStack pStack, ItemStack pOther, Slot pSlot, ClickAction pAction, Player pPlayer, SlotAccess pAccess
    ) {
        return false;
    }

    public float getAttackDamageBonus(Entity p_345249_, float p_336179_, DamageSource p_345403_) {
        return 0.0F;
    }

    /**
     * Current implementations of this method in child classes do not use the entry argument beside ev. They just raise the damage on the stack.
     */
    public boolean hurtEnemy(ItemStack pStack, LivingEntity pTarget, LivingEntity pAttacker) {
        return false;
    }

    public void postHurtEnemy(ItemStack p_346136_, LivingEntity p_346250_, LivingEntity p_346014_) {
    }

    /**
     * Called when a {@link net.minecraft.world.level.block.Block} is destroyed using this Item. Return {@code true} to trigger the "Use Item" statistic.
     */
    public boolean mineBlock(ItemStack pStack, Level pLevel, BlockState pState, BlockPos pPos, LivingEntity pMiningEntity) {
        Tool tool = pStack.get(DataComponents.TOOL);
        if (tool == null) {
            return false;
        } else {
            if (!pLevel.isClientSide && pState.getDestroySpeed(pLevel, pPos) != 0.0F && tool.damagePerBlock() > 0) {
                pStack.hurtAndBreak(tool.damagePerBlock(), pMiningEntity, EquipmentSlot.MAINHAND);
            }

            return true;
        }
    }

    public boolean isCorrectToolForDrops(ItemStack pStack, BlockState pState) {
        Tool tool = pStack.get(DataComponents.TOOL);
        return tool != null && tool.isCorrectForDrops(pState);
    }

    /**
     * Try interacting with given entity. Return {@code InteractionResult.PASS} if nothing should happen.
     */
    public InteractionResult interactLivingEntity(ItemStack pStack, Player pPlayer, LivingEntity pInteractionTarget, InteractionHand pUsedHand) {
        return InteractionResult.PASS;
    }

    public Component getDescription() {
        return Component.translatable(this.getDescriptionId());
    }

    @Override
    public String toString() {
        return BuiltInRegistries.ITEM.wrapAsHolder(this).getRegisteredName();
    }

    protected String getOrCreateDescriptionId() {
        if (this.descriptionId == null) {
            this.descriptionId = Util.makeDescriptionId("item", BuiltInRegistries.ITEM.getKey(this));
        }

        return this.descriptionId;
    }

    public String getDescriptionId() {
        return this.getOrCreateDescriptionId();
    }

    /**
     * Returns the unlocalized name of this item. This version accepts an ItemStack so different stacks can have different names based on their damage or NBT.
     */
    public String getDescriptionId(ItemStack pStack) {
        return this.getDescriptionId();
    }

    @Nullable
    @Deprecated // Use ItemStack sensitive version.
    public final Item getCraftingRemainingItem() {
        return this.craftingRemainingItem;
    }

    @Deprecated // Use ItemStack sensitive version.
    public boolean hasCraftingRemainingItem() {
        return this.craftingRemainingItem != null;
    }

    /**
     * Called each tick as long the item is in a player's inventory. Used by maps to check if it's in a player's hand and update its contents.
     */
    public void inventoryTick(ItemStack pStack, Level pLevel, Entity pEntity, int pSlotId, boolean pIsSelected) {
    }

    /**
     * Called when item is crafted/smelted. Used only by maps so far.
     */
    public void onCraftedBy(ItemStack pStack, Level pLevel, Player pPlayer) {
        this.onCraftedPostProcess(pStack, pLevel);
    }

    public void onCraftedPostProcess(ItemStack pStack, Level pLevel) {
    }

    public boolean isComplex() {
        return false;
    }

    /**
     * Returns the action that specifies what animation to play when the item is being used.
     */
    public UseAnim getUseAnimation(ItemStack pStack) {
        return pStack.has(DataComponents.FOOD) ? UseAnim.EAT : UseAnim.NONE;
    }

    /**
     * How long it takes to use or consume an item
     */
    public int getUseDuration(ItemStack pStack, LivingEntity p_344979_) {
        FoodProperties foodproperties = pStack.getFoodProperties(null);
        return foodproperties != null ? foodproperties.eatDurationTicks() : 0;
    }

    /**
     * Called when the player stops using an Item (stops holding the right mouse button).
     */
    public void releaseUsing(ItemStack pStack, Level pLevel, LivingEntity pLivingEntity, int pTimeCharged) {
    }

    public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
    }

    public Optional<TooltipComponent> getTooltipImage(ItemStack pStack) {
        return Optional.empty();
    }

    public Component getName(ItemStack pStack) {
        return Component.translatable(this.getDescriptionId(pStack));
    }

    /**
     * Returns {@code true} if this item has an enchantment glint. By default, this returns {@code stack.isEnchanted()}, but other items can override it (for instance, written books always return true).
     *
     * Note that if you override this method, you generally want to also call the super version (on {@link Item}) to get the glint for enchanted items. Of course, that is unnecessary if the overwritten version always returns true.
     */
    public boolean isFoil(ItemStack pStack) {
        return pStack.isEnchanted();
    }

    /**
     * Checks isDamagable and if it cannot be stacked
     */
    public boolean isEnchantable(ItemStack pStack) {
        return pStack.getMaxStackSize() == 1 && pStack.has(DataComponents.MAX_DAMAGE);
    }

    protected static BlockHitResult getPlayerPOVHitResult(Level pLevel, Player pPlayer, ClipContext.Fluid pFluidMode) {
        Vec3 vec3 = pPlayer.getEyePosition();
        Vec3 vec31 = vec3.add(pPlayer.calculateViewVector(pPlayer.getXRot(), pPlayer.getYRot()).scale(pPlayer.blockInteractionRange()));
        return pLevel.clip(new ClipContext(vec3, vec31, ClipContext.Block.OUTLINE, pFluidMode, pPlayer));
    }

    /** @deprecated Neo: Use ItemStack sensitive version. */
    @Deprecated
    public int getEnchantmentValue() {
        return 0;
    }

    /**
     * Return whether this item is repairable in an anvil.
     */
    public boolean isValidRepairItem(ItemStack pStack, ItemStack pRepairCandidate) {
        return false;
    }

    @Deprecated // Use ItemStack sensitive version or data components. (deprecated by vanilla too)
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        return ItemAttributeModifiers.EMPTY;
    }

    protected final boolean canRepair;

    @Override
    public boolean isRepairable(ItemStack stack) {
        return canRepair && isDamageable(stack);
    }

    /**
     * If this stack's item is a crossbow
     */
    public boolean useOnRelease(ItemStack pStack) {
        return pStack.getItem() == Items.CROSSBOW;
    }

    public ItemStack getDefaultInstance() {
        return new ItemStack(this);
    }

    public SoundEvent getDrinkingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    public SoundEvent getEatingSound() {
        return SoundEvents.GENERIC_EAT;
    }

    public SoundEvent getBreakingSound() {
        return SoundEvents.ITEM_BREAK;
    }

    public boolean canFitInsideContainerItems() {
        return true;
    }

    @Override
    public FeatureFlagSet requiredFeatures() {
        return this.requiredFeatures;
    }

    // NEO START
    private Object renderProperties;

    /*
        DO NOT CALL, IT WILL DISAPPEAR IN THE FUTURE
        Call RenderProperties.get instead
    */
    public Object getRenderPropertiesInternal() {
        return renderProperties;
    }

    private void initClient() {
        // Minecraft instance isn't available in datagen, so don't call initializeClient if in datagen
                if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT && !net.neoforged.neoforge.data.loading.DatagenModLoader.isRunningDataGen()) {
                initializeClient(properties -> {
                        if (properties == this)
                                throw new IllegalStateException("Don't extend IItemRenderProperties in your item, use an anonymous class instead.");
                        this.renderProperties = properties;
                    });
            }
    }

    public void initializeClient(java.util.function.Consumer<net.neoforged.neoforge.client.extensions.common.IClientItemExtensions> consumer) {
    }
    // END NEO

    public static class Properties implements net.neoforged.neoforge.common.extensions.IItemPropertiesExtensions {
        private static final Interner<DataComponentMap> COMPONENT_INTERNER = Interners.newStrongInterner();
        @Nullable
        private DataComponentMap.Builder components;
        @Nullable
        Item craftingRemainingItem;
        FeatureFlagSet requiredFeatures = FeatureFlags.VANILLA_SET;
        private boolean canRepair = true;

        public Item.Properties food(FoodProperties pFood) {
            return this.component(DataComponents.FOOD, pFood);
        }

        public Item.Properties stacksTo(int pMaxStackSize) {
            return this.component(DataComponents.MAX_STACK_SIZE, pMaxStackSize);
        }

        public Item.Properties durability(int pMaxDamage) {
            this.component(DataComponents.MAX_DAMAGE, pMaxDamage);
            this.component(DataComponents.MAX_STACK_SIZE, 1);
            this.component(DataComponents.DAMAGE, 0);
            return this;
        }

        public Item.Properties craftRemainder(Item pCraftingRemainingItem) {
            this.craftingRemainingItem = pCraftingRemainingItem;
            return this;
        }

        public Item.Properties rarity(Rarity pRarity) {
            return this.component(DataComponents.RARITY, pRarity);
        }

        public Item.Properties fireResistant() {
            return this.component(DataComponents.FIRE_RESISTANT, Unit.INSTANCE);
        }

        public Item.Properties jukeboxPlayable(ResourceKey<JukeboxSong> p_350862_) {
            return this.component(DataComponents.JUKEBOX_PLAYABLE, new JukeboxPlayable(new EitherHolder<>(p_350862_), true));
        }

        public Item.Properties setNoRepair() {
            canRepair = false;
            return this;
        }

        public Item.Properties requiredFeatures(FeatureFlag... pRequiredFeatures) {
            this.requiredFeatures = FeatureFlags.REGISTRY.subset(pRequiredFeatures);
            return this;
        }

        public <T> Item.Properties component(DataComponentType<T> pComponent, T pValue) {
            net.neoforged.neoforge.common.CommonHooks.validateComponent(pValue);
            if (this.components == null) {
                this.components = DataComponentMap.builder().addAll(DataComponents.COMMON_ITEM_COMPONENTS);
            }

            this.components.set(pComponent, pValue);
            return this;
        }

        public Item.Properties attributes(ItemAttributeModifiers pAttributes) {
            return this.component(DataComponents.ATTRIBUTE_MODIFIERS, pAttributes);
        }

        DataComponentMap buildAndValidateComponents() {
            DataComponentMap datacomponentmap = this.buildComponents();
            return validateComponents(datacomponentmap);
        }

        public static DataComponentMap validateComponents(DataComponentMap datacomponentmap) {
            if (datacomponentmap.has(DataComponents.DAMAGE) && datacomponentmap.getOrDefault(DataComponents.MAX_STACK_SIZE, 1) > 1) {
                throw new IllegalStateException("Item cannot have both durability and be stackable");
            } else {
                return datacomponentmap;
            }
        }

        private DataComponentMap buildComponents() {
            return this.components == null ? DataComponents.COMMON_ITEM_COMPONENTS : COMPONENT_INTERNER.intern(this.components.build());
        }
    }

    public interface TooltipContext {
        Item.TooltipContext EMPTY = new Item.TooltipContext() {
            @Nullable
            @Override
            public HolderLookup.Provider registries() {
                return null;
            }

            @Override
            public float tickRate() {
                return 20.0F;
            }

            @Nullable
            @Override
            public MapItemSavedData mapData(MapId p_339618_) {
                return null;
            }
        };

        @Nullable
        HolderLookup.Provider registries();

        float tickRate();

        @Nullable
        MapItemSavedData mapData(MapId pMapId);

        static Item.TooltipContext of(@Nullable final Level pLevel) {
            return pLevel == null ? EMPTY : new Item.TooltipContext() {
                @Override
                public HolderLookup.Provider registries() {
                    return pLevel.registryAccess();
                }

                @Override
                public float tickRate() {
                    return pLevel.tickRateManager().tickrate();
                }

                @Override
                public MapItemSavedData mapData(MapId p_339628_) {
                    return pLevel.getMapData(p_339628_);
                }
            };
        }

        static Item.TooltipContext of(final HolderLookup.Provider pRegistries) {
            return new Item.TooltipContext() {
                @Override
                public HolderLookup.Provider registries() {
                    return pRegistries;
                }

                @Override
                public float tickRate() {
                    return 20.0F;
                }

                @Nullable
                @Override
                public MapItemSavedData mapData(MapId p_339679_) {
                    return null;
                }
            };
        }
    }
}
