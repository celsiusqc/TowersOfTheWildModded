package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractFurnaceBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, RecipeCraftingHolder, StackedContentsCompatible {
    protected static final int SLOT_INPUT = 0;
    protected static final int SLOT_FUEL = 1;
    protected static final int SLOT_RESULT = 2;
    public static final int DATA_LIT_TIME = 0;
    private static final int[] SLOTS_FOR_UP = new int[]{0};
    private static final int[] SLOTS_FOR_DOWN = new int[]{2, 1};
    private static final int[] SLOTS_FOR_SIDES = new int[]{1};
    public static final int DATA_LIT_DURATION = 1;
    public static final int DATA_COOKING_PROGRESS = 2;
    public static final int DATA_COOKING_TOTAL_TIME = 3;
    public static final int NUM_DATA_VALUES = 4;
    public static final int BURN_TIME_STANDARD = 200;
    public static final int BURN_COOL_SPEED = 2;
    private final RecipeType<? extends AbstractCookingRecipe> recipeType;
    protected NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    int litTime;
    int litDuration;
    int cookingProgress;
    int cookingTotalTime;
    @Nullable
    private static volatile Map<Item, Integer> fuelCache;
    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int p_58431_) {
            switch (p_58431_) {
                case 0:
                    if (litDuration > Short.MAX_VALUE) {
                        // Neo: preserve litTime / litDuration ratio on the client as data slots are synced as shorts.
                        return net.minecraft.util.Mth.floor(((double) litTime / litDuration) * Short.MAX_VALUE);
                    }

                    return AbstractFurnaceBlockEntity.this.litTime;
                case 1:
                    return Math.min(AbstractFurnaceBlockEntity.this.litDuration, Short.MAX_VALUE);
                case 2:
                    return AbstractFurnaceBlockEntity.this.cookingProgress;
                case 3:
                    return AbstractFurnaceBlockEntity.this.cookingTotalTime;
                default:
                    return 0;
            }
        }

        @Override
        public void set(int p_58433_, int p_58434_) {
            switch (p_58433_) {
                case 0:
                    AbstractFurnaceBlockEntity.this.litTime = p_58434_;
                    break;
                case 1:
                    AbstractFurnaceBlockEntity.this.litDuration = p_58434_;
                    break;
                case 2:
                    AbstractFurnaceBlockEntity.this.cookingProgress = p_58434_;
                    break;
                case 3:
                    AbstractFurnaceBlockEntity.this.cookingTotalTime = p_58434_;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };
    private final Object2IntOpenHashMap<ResourceLocation> recipesUsed = new Object2IntOpenHashMap<>();
    private final RecipeManager.CachedCheck<SingleRecipeInput, ? extends AbstractCookingRecipe> quickCheck;

    protected AbstractFurnaceBlockEntity(
        BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState, RecipeType<? extends AbstractCookingRecipe> pRecipeType
    ) {
        super(pType, pPos, pBlockState);
        this.quickCheck = RecipeManager.createCheck((RecipeType<AbstractCookingRecipe>)pRecipeType);
        this.recipeType = pRecipeType;
    }

    public static void invalidateCache() {
        fuelCache = null;
    }

    /**
     * @deprecated Neo: get burn times by calling {@link net.neoforged.neoforge.common.extensions.IItemStackExtension#getBurnTime(RecipeType)}
     */
    @Deprecated
    public static Map<Item, Integer> getFuel() {
        Map<Item, Integer> map = fuelCache;
        if (map != null) {
            return map;
        } else {
            Map<Item, Integer> map1 = Maps.newLinkedHashMap();
            buildFuels((e, time) -> e.ifRight(tag -> add(map1, tag, time)).ifLeft(item -> add(map1, item, time)));
            fuelCache = map1;
            return map1;
        }
    }

    private static void add(java.util.function.ObjIntConsumer<com.mojang.datafixers.util.Either<Item, TagKey<Item>>> consumer, ItemLike item, int time) {
        consumer.accept(com.mojang.datafixers.util.Either.left(item.asItem()), time);
    }

    private static void add(java.util.function.ObjIntConsumer<com.mojang.datafixers.util.Either<Item, TagKey<Item>>> consumer, TagKey<Item> tag, int time) {
        consumer.accept(com.mojang.datafixers.util.Either.right(tag), time);
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public static void buildFuels(java.util.function.ObjIntConsumer<com.mojang.datafixers.util.Either<Item, TagKey<Item>>> map1) {
        {
            add(map1, Items.LAVA_BUCKET, 20000);
            add(map1, Blocks.COAL_BLOCK, 16000);
            add(map1, Items.BLAZE_ROD, 2400);
            add(map1, Items.COAL, 1600);
            add(map1, Items.CHARCOAL, 1600);
            add(map1, ItemTags.LOGS, 300);
            add(map1, ItemTags.BAMBOO_BLOCKS, 300);
            add(map1, ItemTags.PLANKS, 300);
            add(map1, Blocks.BAMBOO_MOSAIC, 300);
            add(map1, ItemTags.WOODEN_STAIRS, 300);
            add(map1, Blocks.BAMBOO_MOSAIC_STAIRS, 300);
            add(map1, ItemTags.WOODEN_SLABS, 150);
            add(map1, Blocks.BAMBOO_MOSAIC_SLAB, 150);
            add(map1, ItemTags.WOODEN_TRAPDOORS, 300);
            add(map1, ItemTags.WOODEN_PRESSURE_PLATES, 300);
            add(map1, ItemTags.WOODEN_FENCES, 300);
            add(map1, ItemTags.FENCE_GATES, 300);
            add(map1, Blocks.NOTE_BLOCK, 300);
            add(map1, Blocks.BOOKSHELF, 300);
            add(map1, Blocks.CHISELED_BOOKSHELF, 300);
            add(map1, Blocks.LECTERN, 300);
            add(map1, Blocks.JUKEBOX, 300);
            add(map1, Blocks.CHEST, 300);
            add(map1, Blocks.TRAPPED_CHEST, 300);
            add(map1, Blocks.CRAFTING_TABLE, 300);
            add(map1, Blocks.DAYLIGHT_DETECTOR, 300);
            add(map1, ItemTags.BANNERS, 300);
            add(map1, Items.BOW, 300);
            add(map1, Items.FISHING_ROD, 300);
            add(map1, Blocks.LADDER, 300);
            add(map1, ItemTags.SIGNS, 200);
            add(map1, ItemTags.HANGING_SIGNS, 800);
            add(map1, Items.WOODEN_SHOVEL, 200);
            add(map1, Items.WOODEN_SWORD, 200);
            add(map1, Items.WOODEN_HOE, 200);
            add(map1, Items.WOODEN_AXE, 200);
            add(map1, Items.WOODEN_PICKAXE, 200);
            add(map1, ItemTags.WOODEN_DOORS, 200);
            add(map1, ItemTags.BOATS, 1200);
            add(map1, ItemTags.WOOL, 100);
            add(map1, ItemTags.WOODEN_BUTTONS, 100);
            add(map1, Items.STICK, 100);
            add(map1, ItemTags.SAPLINGS, 100);
            add(map1, Items.BOWL, 100);
            add(map1, ItemTags.WOOL_CARPETS, 67);
            add(map1, Blocks.DRIED_KELP_BLOCK, 4001);
            add(map1, Items.CROSSBOW, 300);
            add(map1, Blocks.BAMBOO, 50);
            add(map1, Blocks.DEAD_BUSH, 100);
            add(map1, Blocks.SCAFFOLDING, 50);
            add(map1, Blocks.LOOM, 300);
            add(map1, Blocks.BARREL, 300);
            add(map1, Blocks.CARTOGRAPHY_TABLE, 300);
            add(map1, Blocks.FLETCHING_TABLE, 300);
            add(map1, Blocks.SMITHING_TABLE, 300);
            add(map1, Blocks.COMPOSTER, 300);
            add(map1, Blocks.AZALEA, 100);
            add(map1, Blocks.FLOWERING_AZALEA, 100);
            add(map1, Blocks.MANGROVE_ROOTS, 300);
        }
    }

    private static boolean isNeverAFurnaceFuel(Item pItem) {
        return pItem.builtInRegistryHolder().is(ItemTags.NON_FLAMMABLE_WOOD);
    }

    private static void add(Map<Item, Integer> pMap, TagKey<Item> pItemTag, int pBurnTime) {
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(pItemTag)) {
            if (!isNeverAFurnaceFuel(holder.value())) {
                pMap.put(holder.value(), pBurnTime);
            }
        }
    }

    private static void add(Map<Item, Integer> pMap, ItemLike pItem, int pBurnTime) {
        Item item = pItem.asItem();
        if (isNeverAFurnaceFuel(item)) {
            if (SharedConstants.IS_RUNNING_IN_IDE) {
                throw (IllegalStateException)Util.pauseInIde(
                    new IllegalStateException(
                        "A developer tried to explicitly make fire resistant item " + item.getName(null).getString() + " a furnace fuel. That will not work!"
                    )
                );
            }
        } else {
            pMap.put(item, pBurnTime);
        }
    }

    private boolean isLit() {
        return this.litTime > 0;
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(pTag, this.items, pRegistries);
        this.litTime = pTag.getInt("BurnTime");
        this.cookingProgress = pTag.getInt("CookTime");
        this.cookingTotalTime = pTag.getInt("CookTimeTotal");
        this.litDuration = this.getBurnDuration(this.items.get(1));
        CompoundTag compoundtag = pTag.getCompound("RecipesUsed");

        for (String s : compoundtag.getAllKeys()) {
            this.recipesUsed.put(ResourceLocation.parse(s), compoundtag.getInt(s));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(pTag, pRegistries);
        pTag.putInt("BurnTime", this.litTime);
        pTag.putInt("CookTime", this.cookingProgress);
        pTag.putInt("CookTimeTotal", this.cookingTotalTime);
        ContainerHelper.saveAllItems(pTag, this.items, pRegistries);
        CompoundTag compoundtag = new CompoundTag();
        this.recipesUsed.forEach((p_187449_, p_187450_) -> compoundtag.putInt(p_187449_.toString(), p_187450_));
        pTag.put("RecipesUsed", compoundtag);
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, AbstractFurnaceBlockEntity pBlockEntity) {
        boolean flag = pBlockEntity.isLit();
        boolean flag1 = false;
        if (pBlockEntity.isLit()) {
            pBlockEntity.litTime--;
        }

        ItemStack itemstack = pBlockEntity.items.get(1);
        ItemStack itemstack1 = pBlockEntity.items.get(0);
        boolean flag2 = !itemstack1.isEmpty();
        boolean flag3 = !itemstack.isEmpty();
        if (pBlockEntity.isLit() || flag3 && flag2) {
            RecipeHolder<?> recipeholder;
            if (flag2) {
                recipeholder = pBlockEntity.quickCheck.getRecipeFor(new SingleRecipeInput(itemstack1), pLevel).orElse(null);
            } else {
                recipeholder = null;
            }

            int i = pBlockEntity.getMaxStackSize();
            if (!pBlockEntity.isLit() && canBurn(pLevel.registryAccess(), recipeholder, pBlockEntity.items, i, pBlockEntity)) {
                pBlockEntity.litTime = pBlockEntity.getBurnDuration(itemstack);
                pBlockEntity.litDuration = pBlockEntity.litTime;
                if (pBlockEntity.isLit()) {
                    flag1 = true;
                    if (itemstack.hasCraftingRemainingItem())
                        pBlockEntity.items.set(1, itemstack.getCraftingRemainingItem());
                    else
                    if (flag3) {
                        Item item = itemstack.getItem();
                        itemstack.shrink(1);
                        if (itemstack.isEmpty()) {
                            pBlockEntity.items.set(1, itemstack.getCraftingRemainingItem());
                        }
                    }
                }
            }

            if (pBlockEntity.isLit() && canBurn(pLevel.registryAccess(), recipeholder, pBlockEntity.items, i, pBlockEntity)) {
                pBlockEntity.cookingProgress++;
                if (pBlockEntity.cookingProgress == pBlockEntity.cookingTotalTime) {
                    pBlockEntity.cookingProgress = 0;
                    pBlockEntity.cookingTotalTime = getTotalCookTime(pLevel, pBlockEntity);
                    if (burn(pLevel.registryAccess(), recipeholder, pBlockEntity.items, i, pBlockEntity)) {
                        pBlockEntity.setRecipeUsed(recipeholder);
                    }

                    flag1 = true;
                }
            } else {
                pBlockEntity.cookingProgress = 0;
            }
        } else if (!pBlockEntity.isLit() && pBlockEntity.cookingProgress > 0) {
            pBlockEntity.cookingProgress = Mth.clamp(pBlockEntity.cookingProgress - 2, 0, pBlockEntity.cookingTotalTime);
        }

        if (flag != pBlockEntity.isLit()) {
            flag1 = true;
            pState = pState.setValue(AbstractFurnaceBlock.LIT, Boolean.valueOf(pBlockEntity.isLit()));
            pLevel.setBlock(pPos, pState, 3);
        }

        if (flag1) {
            setChanged(pLevel, pPos, pState);
        }
    }

    private static boolean canBurn(RegistryAccess pRegistryAccess, @Nullable RecipeHolder<?> pRecipe, NonNullList<ItemStack> pInventory, int pMaxStackSize, AbstractFurnaceBlockEntity furnace) {
        if (!pInventory.get(0).isEmpty() && pRecipe != null) {
            ItemStack itemstack = ((RecipeHolder<? extends AbstractCookingRecipe>) pRecipe).value().assemble(new SingleRecipeInput(furnace.getItem(0)), pRegistryAccess);
            if (itemstack.isEmpty()) {
                return false;
            } else {
                ItemStack itemstack1 = pInventory.get(2);
                if (itemstack1.isEmpty()) {
                    return true;
                } else if (!ItemStack.isSameItemSameComponents(itemstack1, itemstack)) {
                    return false;
                } else {
                    return itemstack1.getCount() + itemstack.getCount() <= pMaxStackSize && itemstack1.getCount() + itemstack.getCount() <= itemstack1.getMaxStackSize() // Neo fix: make furnace respect stack sizes in furnace recipes
                        ? true
                        : itemstack1.getCount() + itemstack.getCount() <= itemstack.getMaxStackSize(); // Neo fix: make furnace respect stack sizes in furnace recipes
                }
            }
        } else {
            return false;
        }
    }

    private static boolean burn(RegistryAccess pRegistryAccess, @Nullable RecipeHolder<?> pRecipe, NonNullList<ItemStack> pInventory, int pMaxStackSize, AbstractFurnaceBlockEntity furnace) {
        if (pRecipe != null && canBurn(pRegistryAccess, pRecipe, pInventory, pMaxStackSize, furnace)) {
            ItemStack itemstack = pInventory.get(0);
            ItemStack itemstack1 = ((RecipeHolder<? extends AbstractCookingRecipe>) pRecipe).value().assemble(new SingleRecipeInput(furnace.getItem(0)), pRegistryAccess);
            ItemStack itemstack2 = pInventory.get(2);
            if (itemstack2.isEmpty()) {
                pInventory.set(2, itemstack1.copy());
            } else if (ItemStack.isSameItemSameComponents(itemstack2, itemstack1)) {
                itemstack2.grow(itemstack1.getCount());
            }

            if (itemstack.is(Blocks.WET_SPONGE.asItem()) && !pInventory.get(1).isEmpty() && pInventory.get(1).is(Items.BUCKET)) {
                pInventory.set(1, new ItemStack(Items.WATER_BUCKET));
            }

            itemstack.shrink(1);
            return true;
        } else {
            return false;
        }
    }

    protected int getBurnDuration(ItemStack pFuel) {
        if (pFuel.isEmpty()) {
            return 0;
        } else {
            return pFuel.getBurnTime(this.recipeType);
        }
    }

    private static int getTotalCookTime(Level pLevel, AbstractFurnaceBlockEntity pBlockEntity) {
        SingleRecipeInput singlerecipeinput = new SingleRecipeInput(pBlockEntity.getItem(0));
        return pBlockEntity.quickCheck.getRecipeFor(singlerecipeinput, pLevel).map(p_300840_ -> p_300840_.value().getCookingTime()).orElse(200);
    }

    public static boolean isFuel(ItemStack pStack) {
        return pStack.getBurnTime(null) > 0;
    }

    @Override
    public int[] getSlotsForFace(Direction pSide) {
        if (pSide == Direction.DOWN) {
            return SLOTS_FOR_DOWN;
        } else {
            return pSide == Direction.UP ? SLOTS_FOR_UP : SLOTS_FOR_SIDES;
        }
    }

    /**
     * Returns {@code true} if automation can insert the given item in the given slot from the given side.
     */
    @Override
    public boolean canPlaceItemThroughFace(int pIndex, ItemStack pItemStack, @Nullable Direction pDirection) {
        return this.canPlaceItem(pIndex, pItemStack);
    }

    /**
     * Returns {@code true} if automation can extract the given item in the given slot from the given side.
     */
    @Override
    public boolean canTakeItemThroughFace(int pIndex, ItemStack pStack, Direction pDirection) {
        return pDirection == Direction.DOWN && pIndex == 1 ? pStack.is(Items.WATER_BUCKET) || pStack.is(Items.BUCKET) : true;
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> pItems) {
        this.items = pItems;
    }

    /**
     * Sets the given item stack to the specified slot in the inventory (can be crafting or armor sections).
     */
    @Override
    public void setItem(int pIndex, ItemStack pStack) {
        ItemStack itemstack = this.items.get(pIndex);
        boolean flag = !pStack.isEmpty() && ItemStack.isSameItemSameComponents(itemstack, pStack);
        this.items.set(pIndex, pStack);
        pStack.limitSize(this.getMaxStackSize(pStack));
        if (pIndex == 0 && !flag) {
            this.cookingTotalTime = getTotalCookTime(this.level, this);
            this.cookingProgress = 0;
            this.setChanged();
        }
    }

    /**
     * Returns {@code true} if automation is allowed to insert the given stack (ignoring stack size) into the given slot. For guis use Slot.isItemValid
     */
    @Override
    public boolean canPlaceItem(int pIndex, ItemStack pStack) {
        if (pIndex == 2) {
            return false;
        } else if (pIndex != 1) {
            return true;
        } else {
            ItemStack itemstack = this.items.get(1);
            return pStack.getBurnTime(this.recipeType) > 0 || pStack.is(Items.BUCKET) && !itemstack.is(Items.BUCKET);
        }
    }

    @Override
    public void setRecipeUsed(@Nullable RecipeHolder<?> pRecipe) {
        if (pRecipe != null) {
            ResourceLocation resourcelocation = pRecipe.id();
            this.recipesUsed.addTo(resourcelocation, 1);
        }
    }

    @Nullable
    @Override
    public RecipeHolder<?> getRecipeUsed() {
        return null;
    }

    @Override
    public void awardUsedRecipes(Player pPlayer, List<ItemStack> pItems) {
    }

    public void awardUsedRecipesAndPopExperience(ServerPlayer pPlayer) {
        List<RecipeHolder<?>> list = this.getRecipesToAwardAndPopExperience(pPlayer.serverLevel(), pPlayer.position());
        pPlayer.awardRecipes(list);

        for (RecipeHolder<?> recipeholder : list) {
            if (recipeholder != null) {
                pPlayer.triggerRecipeCrafted(recipeholder, this.items);
            }
        }

        this.recipesUsed.clear();
    }

    public List<RecipeHolder<?>> getRecipesToAwardAndPopExperience(ServerLevel pLevel, Vec3 pPopVec) {
        List<RecipeHolder<?>> list = Lists.newArrayList();

        for (Entry<ResourceLocation> entry : this.recipesUsed.object2IntEntrySet()) {
            pLevel.getRecipeManager().byKey(entry.getKey()).ifPresent(p_300839_ -> {
                list.add((RecipeHolder<?>)p_300839_);
                createExperience(pLevel, pPopVec, entry.getIntValue(), ((AbstractCookingRecipe)p_300839_.value()).getExperience());
            });
        }

        return list;
    }

    private static void createExperience(ServerLevel pLevel, Vec3 pPopVec, int pRecipeIndex, float pExperience) {
        int i = Mth.floor((float)pRecipeIndex * pExperience);
        float f = Mth.frac((float)pRecipeIndex * pExperience);
        if (f != 0.0F && Math.random() < (double)f) {
            i++;
        }

        ExperienceOrb.award(pLevel, pPopVec, i);
    }

    @Override
    public void fillStackedContents(StackedContents pHelper) {
        for (ItemStack itemstack : this.items) {
            pHelper.accountStack(itemstack);
        }
    }
}
